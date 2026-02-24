package net.ralubog.mcmaps_maze.commands;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChainBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class ExerciseLogic {

    private static int score = 0;
    private static final int TOTAL_EXERCISES = 10;
    private static final Set<Integer> completedExercises = new HashSet<>();

    // --- MEMORY SYSTEM: Remembers the active puzzle state ---
    private static PuzzleContext currentContext = null;

    private static class PuzzleContext {
        int id;
        BlockPos origin;      // Bottom-left of the puzzle
        Direction rightDir;   // The X-axis on the wall
        Direction upDir = Direction.UP; // The Y-axis
        List<BlockPos> expectedNodes = new ArrayList<>(); // Where nodes SHOULD be

        public PuzzleContext(int id, BlockPos origin, Direction right) {
            this.id = id;
            this.origin = origin;
            this.rightDir = right;
        }
    }

    // =========================================================
    //                 1. START EXERCISE
    // =========================================================

    public static void startExercise(ServerPlayerEntity player, int id) {
        ServerWorld world = player.getServerWorld();

        // Calculate the "Canvas" (The wall 4 blocks ahead)
        BlockPos origin = player.getBlockPos().offset(player.getHorizontalFacing(), 4);
        Direction right = player.getHorizontalFacing().rotateYClockwise();
        Direction facing = player.getHorizontalFacing();

        // 1. Initialize Memory
        currentContext = new PuzzleContext(id, origin, right);

        // 2. Clean the Canvas (15x15 area)
        GraphBuilder.clearArea(world, origin, right);

        // 3. Give Tools
        player.getInventory().clear();
        giveTools(player, id);

        // 4. Build & Memorize Nodes
        switch (id) {
            case 2: buildEx2(world, facing); break;
            case 4: buildEx4(world, facing); break;
            case 6: buildEx6(world, facing); break;
            case 7: buildEx7(world, facing); break;
            case 8: buildEx8(world, facing); break;
            case 10: buildEx10(world, facing); break;
            default:
                player.sendMessage(Text.literal("Answer in the book (MCQ)").formatted(Formatting.AQUA), true);
                break;
        }
    }

    private static void giveTools(ServerPlayerEntity player, int id) {
        if (id == 2 || id == 8 || id == 10) {
            player.getInventory().insertStack(new ItemStack(Blocks.CHAIN, 64));
            player.sendMessage(Text.literal("Tool: Chains").formatted(Formatting.YELLOW), true);
        }
        if (id == 4 || id == 7) {
            player.sendMessage(Text.literal("Tool: Break Blocks (Fist)").formatted(Formatting.YELLOW), true);
        }
        if (id == 6) {
            player.getInventory().insertStack(new ItemStack(Blocks.RED_CONCRETE, 64));
            player.getInventory().insertStack(new ItemStack(Blocks.BLUE_CONCRETE, 64));
            player.sendMessage(Text.literal("Tool: Red/Blue Concrete").formatted(Formatting.YELLOW), true);
        }
    }

    // =========================================================
    //                 2. CHECK ANSWER (THE SCANNER)
    // =========================================================

    public static void checkAnswer(ServerPlayerEntity player, int id, String mcqChoice) {
        if (completedExercises.contains(id)) {
            player.sendMessage(Text.literal("Already Completed!").formatted(Formatting.GRAY));
            return;
        }

        boolean correct = false;

        // MCQ Checks (No scanning needed)
        if (id == 1) correct = "B".equals(mcqChoice);
        else if (id == 3) correct = "C".equals(mcqChoice);
        else if (id == 5) correct = "A".equals(mcqChoice);
        else if (id == 9) correct = "B".equals(mcqChoice);

            // INTERACTIVE CHECKS (Scan the World!)
        else {
            if (currentContext == null || currentContext.id != id) {
                player.sendMessage(Text.literal("Please start the exercise first!").formatted(Formatting.RED));
                return;
            }

            // 1. SCAN THE WALL
            GraphResult graph = scanWholePlane(player.getServerWorld());

            // 2. RUN LOGIC
            switch (id) {
                case 2: correct = isConnected(graph); break;
                case 4: correct = isConnected(graph) && !hasCycle(graph); break; // Tree
                case 6: correct = checkBipartite(player.getServerWorld(), graph); break;
                case 7: correct = isConnected(graph) && isTree(graph); break; // MST
                case 8: correct = isConnected(graph); break;
                case 10: correct = hasCycle(graph); break;
            }
        }

        if (correct) {
            score++;
            completedExercises.add(id);
            player.sendMessage(Text.literal("CORRECT! Score: " + score + "/10").formatted(Formatting.GREEN));
            player.getWorld().playSound(null, player.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, net.minecraft.sound.SoundCategory.PLAYERS, 1f, 1f);
        } else {
            player.sendMessage(Text.literal("Incorrect. Try again!").formatted(Formatting.RED));
        }
    }

    // =========================================================
    //              3. THE GRAPH ANALYZER (SCANNER)
    // =========================================================

    private static class GraphResult {
        List<BlockPos> nodes = new ArrayList<>();
        Map<BlockPos, List<BlockPos>> adj = new HashMap<>();
    }

    /**
     * This scans the 15x15 plane defined in currentContext.
     * It detects Nodes (Gold/Concrete) and traces Chains to build the Graph object.
     */
    private static GraphResult scanWholePlane(ServerWorld world) {
        GraphResult result = new GraphResult();
        BlockPos origin = currentContext.origin;
        Direction right = currentContext.rightDir;
        Direction up = Direction.UP;

        // A. Find all Nodes currently in the world
        // We only scan where we expect nodes, OR the whole area to find user-placed ones
        // Let's scan the known list from Context to be strict, or whole area to be flexible.
        // Flexible approach: Scan entire 15x15 grid.
        for (int y = 0; y < 15; y++) {
            for (int x = 0; x < 15; x++) {
                BlockPos p = origin.offset(right, x).up(y);
                BlockState state = world.getBlockState(p);
                if (isNode(state)) {
                    result.nodes.add(p);
                    result.adj.put(p, new ArrayList<>());
                }
            }
        }

        // B. Trace Connections (The "Snake" Logic)
        // For every node, we look in all 4 directions for a chain, then follow it.
        for (BlockPos startNode : result.nodes) {
            for (Direction d : Direction.values()) {
                // If immediate neighbor is a chain...
                if (world.getBlockState(startNode.offset(d)).getBlock() instanceof ChainBlock) {
                    BlockPos connectedNode = followChain(world, startNode, d, result.nodes);
                    if (connectedNode != null) {
                        // Add Edge
                        if (!result.adj.get(startNode).contains(connectedNode)) {
                            result.adj.get(startNode).add(connectedNode);
                        }
                    }
                }
            }
        }
        return result;
    }

    // Walks along chain blocks until it hits a Node or Air
    private static BlockPos followChain(ServerWorld world, BlockPos start, Direction dir, List<BlockPos> validNodes) {
        BlockPos current = start.offset(dir);
        int steps = 0;

        while (steps < 15) { // Max chain length
            BlockState state = world.getBlockState(current);

            // Found a Node?
            if (isNode(state)) {
                // Check if it's one of the graph nodes
                for (BlockPos v : validNodes) {
                    if (v.equals(current)) return v;
                }
                return null; // Hit a random block
            }

            // Found Chain? Keep going
            if (state.getBlock() instanceof ChainBlock) {
                current = current.offset(dir);
                steps++;
            } else {
                // Hit Air or Wall -> Stop
                return null;
            }
        }
        return null;
    }

    private static boolean isNode(BlockState state) {
        return state.isOf(Blocks.GOLD_BLOCK) || state.isOf(Blocks.RED_CONCRETE) || state.isOf(Blocks.BLUE_CONCRETE);
    }

    // =========================================================
    //              4. BUILDERS (With Context Memory)
    // =========================================================

    private static void registerNode(BlockPos pos) {
        currentContext.expectedNodes.add(pos);
    }

    // EX 2: Broken Bridge
    private static void buildEx2(ServerWorld world, Direction facing) {
        BlockPos s = currentContext.origin;
        Direction r = currentContext.rightDir;

        // Two pillars
        BlockPos p1 = s.up(2);
        BlockPos p2 = s.up(6);
        BlockPos p3 = s.offset(r, 4).up(2); // Island
        BlockPos p4 = s.offset(r, 4).up(6); // Island

        GraphBuilder.drawNode(world, p1, "A", facing); registerNode(p1);
        GraphBuilder.drawNode(world, p2, "B", facing); registerNode(p2);
        GraphBuilder.drawNode(world, p3, "C", facing); registerNode(p3);
        GraphBuilder.drawNode(world, p4, "D", facing); registerNode(p4);

        GraphBuilder.drawEdge(world, p1, p2, false, r, facing);
        GraphBuilder.drawEdge(world, p3, p4, false, r, facing);

        GraphBuilder.drawLabel(world, s.up(8), "Ex 2: Connect the two sides", 0xFFFFFF, facing);
    }

    // EX 4: Cycle to Tree
    private static void buildEx4(ServerWorld world, Direction facing) {
        BlockPos s = currentContext.origin;
        Direction r = currentContext.rightDir;

        // Square Cycle
        BlockPos p1 = s.up(2);
        BlockPos p2 = s.up(6);
        BlockPos p3 = s.offset(r, 4).up(6);
        BlockPos p4 = s.offset(r, 4).up(2);

        GraphBuilder.drawNode(world, p1, "", facing); registerNode(p1);
        GraphBuilder.drawNode(world, p2, "", facing); registerNode(p2);
        GraphBuilder.drawNode(world, p3, "", facing); registerNode(p3);
        GraphBuilder.drawNode(world, p4, "", facing); registerNode(p4);

        GraphBuilder.drawEdge(world, p1, p2, false, r, facing);
        GraphBuilder.drawEdge(world, p2, p3, false, r, facing);
        GraphBuilder.drawEdge(world, p3, p4, false, r, facing);
        GraphBuilder.drawEdge(world, p4, p1, false, r, facing);

        GraphBuilder.drawLabel(world, s.up(8), "Ex 4: Break the Cycle", 0xFFFFFF, facing);
    }

    // EX 6: Coloring
    private static void buildEx6(ServerWorld world, Direction facing) {
        BlockPos s = currentContext.origin;
        Direction r = currentContext.rightDir;

        BlockPos p1 = s.up(4);
        BlockPos p2 = s.offset(r, 3).up(4);
        BlockPos p3 = s.offset(r, 6).up(4);

        GraphBuilder.drawNode(world, p1, "", facing); registerNode(p1);
        GraphBuilder.drawNode(world, p2, "", facing); registerNode(p2);
        GraphBuilder.drawNode(world, p3, "", facing); registerNode(p3);

        GraphBuilder.drawEdge(world, p1, p2, false, r, facing);
        GraphBuilder.drawEdge(world, p2, p3, false, r, facing);

        GraphBuilder.drawLabel(world, s.up(6), "Ex 6: Color Red/Blue", 0xFFFFFF, facing);
    }

    // EX 7: MST (Ladder)
    private static void buildEx7(ServerWorld world, Direction facing) {
        BlockPos s = currentContext.origin;
        Direction r = currentContext.rightDir;

        // Square with Cross
        BlockPos p1 = s.up(2);
        BlockPos p2 = s.up(6);
        BlockPos p3 = s.offset(r, 4).up(6);
        BlockPos p4 = s.offset(r, 4).up(2);

        GraphBuilder.drawNode(world, p1, "", facing); registerNode(p1);
        GraphBuilder.drawNode(world, p2, "", facing); registerNode(p2);
        GraphBuilder.drawNode(world, p3, "", facing); registerNode(p3);
        GraphBuilder.drawNode(world, p4, "", facing); registerNode(p4);

        // Frame
        GraphBuilder.drawEdge(world, p1, p2, false, r, facing);
        GraphBuilder.drawEdge(world, p2, p3, false, r, facing);
        GraphBuilder.drawEdge(world, p3, p4, false, r, facing);
        GraphBuilder.drawEdge(world, p4, p1, false, r, facing);

        // Inner "Cross" (Simulated via center node to keep grid clean)
        BlockPos center = s.offset(r, 2).up(4);
        GraphBuilder.drawNode(world, center, "", facing); registerNode(center);

        // Connect center to corners (Star shape)
        // Note: GraphBuilder handles straight lines. We do partials for pseudo-diagonal or just straight.
        // Let's do a "+" shape.
        GraphBuilder.drawEdge(world, center, s.offset(r, 2).up(6), false, r, facing); // Up
        GraphBuilder.drawEdge(world, center, s.offset(r, 2).up(2), false, r, facing); // Down
        GraphBuilder.drawEdge(world, center, s.up(4), false, r, facing); // Left
        GraphBuilder.drawEdge(world, center, s.offset(r, 4).up(4), false, r, facing); // Right

        GraphBuilder.drawLabel(world, s.up(8), "Ex 7: Make it a Tree (Max Removals)", 0xFFFFFF, facing);
    }

    // EX 8: Isolated
    private static void buildEx8(ServerWorld world, Direction facing) {
        BlockPos s = currentContext.origin;
        Direction r = currentContext.rightDir;

        BlockPos p1 = s.up(4);
        BlockPos p2 = s.offset(r, 3).up(4);
        BlockPos iso = s.offset(r, 6).up(4);

        GraphBuilder.drawNode(world, p1, "", facing); registerNode(p1);
        GraphBuilder.drawNode(world, p2, "", facing); registerNode(p2);
        GraphBuilder.drawNode(world, iso, "Lonely", facing); registerNode(iso);

        GraphBuilder.drawEdge(world, p1, p2, false, r, facing);

        GraphBuilder.drawLabel(world, s.up(6), "Ex 8: Connect the lonely node", 0xFFFFFF, facing);
    }

    // EX 10: Make Cycle (U-Shape)
    private static void buildEx10(ServerWorld world, Direction facing) {
        BlockPos s = currentContext.origin;
        Direction r = currentContext.rightDir;

        BlockPos tl = s.up(6);
        BlockPos bl = s.up(2);
        BlockPos br = s.offset(r, 4).up(2);
        BlockPos tr = s.offset(r, 4).up(6);

        GraphBuilder.drawNode(world, tl, "", facing); registerNode(tl);
        GraphBuilder.drawNode(world, bl, "", facing); registerNode(bl);
        GraphBuilder.drawNode(world, br, "", facing); registerNode(br);
        GraphBuilder.drawNode(world, tr, "", facing); registerNode(tr);

        GraphBuilder.drawEdge(world, tl, bl, false, r, facing);
        GraphBuilder.drawEdge(world, bl, br, false, r, facing);
        GraphBuilder.drawEdge(world, br, tr, false, r, facing);

        GraphBuilder.drawLabel(world, s.up(8), "Ex 10: Close the Loop", 0xFFFFFF, facing);
    }


    // =========================================================
    //              5. ALGORITHM CHECKERS
    // =========================================================

    private static boolean isConnected(GraphResult g) {
        if (g.nodes.isEmpty()) return false;
        Set<BlockPos> visited = new HashSet<>();
        dfs(g, g.nodes.get(0), visited);
        return visited.size() == g.nodes.size();
    }

    private static void dfs(GraphResult g, BlockPos u, Set<BlockPos> visited) {
        visited.add(u);
        for (BlockPos v : g.adj.getOrDefault(u, Collections.emptyList())) {
            if (!visited.contains(v)) {
                dfs(g, v, visited);
            }
        }
    }

    private static boolean hasCycle(GraphResult g) {
        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos node : g.nodes) {
            if (!visited.contains(node)) {
                if (dfsCycle(g, node, visited, null)) return true;
            }
        }
        return false;
    }

    private static boolean dfsCycle(GraphResult g, BlockPos u, Set<BlockPos> visited, BlockPos parent) {
        visited.add(u);
        for (BlockPos v : g.adj.getOrDefault(u, Collections.emptyList())) {
            if (v.equals(parent)) continue;
            if (visited.contains(v)) return true;
            if (dfsCycle(g, v, visited, u)) return true;
        }
        return false;
    }

    private static boolean isTree(GraphResult g) {
        return isConnected(g) && !hasCycle(g);
    }

    private static boolean checkBipartite(ServerWorld world, GraphResult g) {
        for (BlockPos u : g.nodes) {
            boolean isRed = world.getBlockState(u).isOf(Blocks.RED_CONCRETE);
            boolean isBlue = world.getBlockState(u).isOf(Blocks.BLUE_CONCRETE);

            if (!isRed && !isBlue) return false; // Not colored

            for (BlockPos v : g.adj.getOrDefault(u, Collections.emptyList())) {
                boolean neighborRed = world.getBlockState(v).isOf(Blocks.RED_CONCRETE);
                boolean neighborBlue = world.getBlockState(v).isOf(Blocks.BLUE_CONCRETE);

                if (isRed && neighborRed) return false; // Same color touch
                if (isBlue && neighborBlue) return false;
            }
        }
        return true;
    }
}
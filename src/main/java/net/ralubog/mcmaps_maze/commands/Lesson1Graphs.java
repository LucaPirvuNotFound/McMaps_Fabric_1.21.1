package net.ralubog.mcmaps_maze.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

public class Lesson1Graphs {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // --- COMMAND 1: THE BOOK ---
            dispatcher.register(CommandManager.literal("give_lesson_1_book")
                    .executes(context -> {
                        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

                        // PAGE 1: INTRODUCTION
                        Text page1 = Text.empty()
                                .append(Text.literal("1. Introduction\n\n").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.DARK_BLUE)))
                                .append(Text.literal("A Graph G = (V, E) is a structure where:\n\n"))
                                .append(Text.literal("V = Vertices").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
                                .append(Text.literal(" (Nodes)\n"))
                                .append(Text.literal("E = Edges").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                                .append(Text.literal(" (Links)\n\n"))
                                .append(Text.literal("Visual Example:\n"))
                                .append(Text.literal("  [SPAWN GRAPH]")
                                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN).withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/build_graph_lesson 1"))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Spawn a simple 3-node graph")))));

                        // PAGE 2: TYPES
                        Text page2 = Text.empty()
                                .append(Text.literal("2. Graph Types\n\n").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.DARK_RED)))
                                .append(Text.literal("1. Undirected:\n").setStyle(Style.EMPTY.withBold(true)))
                                .append(Text.literal("Edges have no direction.\n(A -- B)\n\n"))
                                .append(Text.literal("2. Directed:\n").setStyle(Style.EMPTY.withBold(true)))
                                .append(Text.literal("Edges are arrows.\n(A -> B)\n\n"))
                                .append(Text.literal("  [COMPARE TYPES]")
                                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN).withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/build_graph_lesson 2"))));

                        // PAGE 3: MULTIGRAPHS
                        Text page3 = Text.empty()
                                .append(Text.literal("3. Multigraphs\n\n").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.DARK_PURPLE)))
                                .append(Text.literal("Simple Graph:\nMax 1 edge between two nodes. No loops.\n\n"))
                                .append(Text.literal("Multigraph:\nMultiple edges allowed between nodes.\nLoops allowed (node to itself).\n\n"))
                                .append(Text.literal("  [BUILD MULTIGRAPH]")
                                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN).withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/build_graph_lesson 3"))));

                        // PAGE 4: VERTEX DEGREE
                        Text page4 = Text.empty()
                                .append(Text.literal("4. Vertex Degree\n\n").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.GOLD)))
                                .append(Text.literal("Degree d(v):\nNumber of edges connected to vertex v.\n\n"))
                                .append(Text.literal("Directed Graphs:\n"))
                                .append(Text.literal("In-Degree (d-): Arrows entering.\n"))
                                .append(Text.literal("Out-Degree (d+): Arrows leaving.\n\n"))
                                .append(Text.literal("  [SHOW DEGREES]")
                                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN).withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/build_graph_lesson 4"))));

                        // PAGE 5: SUBGRAPHS
                        Text page5 = Text.empty()
                                .append(Text.literal("5. Subgraphs\n\n").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.BLUE)))
                                .append(Text.literal("A Subgraph H is part of Graph G.\n\n"))
                                .append(Text.literal("H contains a subset of vertices V' ⊆ V and edges E' ⊆ E.\n\n"))
                                .append(Text.literal("Induced Subgraph:\nContains ALL edges from G connecting vertices in V'.\n\n"))
                                .append(Text.literal("  [BUILD SUBGRAPH]")
                                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN).withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/build_graph_lesson 5"))));

                        // PAGE 6: PATHS & CYCLES
                        Text page6 = Text.empty()
                                .append(Text.literal("6. Paths & Cycles\n\n").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.DARK_GREEN)))
                                .append(Text.literal("Walk:\nSequence of vertices/edges.\n\n"))
                                .append(Text.literal("Path:\nWalk with distinct vertices (no repeats).\n\n"))
                                .append(Text.literal("Cycle:\nPath that starts and ends at the same vertex.\n\n"))
                                .append(Text.literal("  [SHOW PATH/CYCLE]")
                                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN).withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/build_graph_lesson 6"))));

                        // PAGE 7: CONNECTIVITY
                        Text page7 = Text.empty()
                                .append(Text.literal("7. Connectivity\n\n").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.RED)))
                                .append(Text.literal("Connected Graph:\nEvery pair of vertices has a path between them.\n\n"))
                                .append(Text.literal("Disconnected:\nContains isolated components (islands).\n\n"))
                                .append(Text.literal("  [SHOW CONNECTIVITY]")
                                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN).withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/build_graph_lesson 7"))));

                        // PAGE 8: BIPARTITE GRAPHS
                        Text page8 = Text.empty()
                                .append(Text.literal("8. Bipartite Graphs\n\n").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.AQUA)))
                                .append(Text.literal("Vertices can be divided into two sets V1, V2.\n\n"))
                                .append(Text.literal("Edges ONLY go between V1 and V2. No edges inside V1 or V2.\n\n"))
                                .append(Text.literal("  [BUILD BIPARTITE]")
                                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN).withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/build_graph_lesson 8"))));

                        // PAGE 9: ADJACENCY MATRIX
                        Text page9 = Text.empty()
                                .append(Text.literal("9. Adjacency Matrix\n\n").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.DARK_PURPLE)))
                                .append(Text.literal("A 2D Grid (Table).\n\n"))
                                .append(Text.literal("Rows = Start Node\nCols = End Node\n\n"))
                                .append(Text.literal("1 = Connected\n0 = Not Connected\n\n"))
                                .append(Text.literal("  [BUILD MATRIX]")
                                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN).withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/build_graph_lesson 9"))));

                        // PAGE 10: ADJACENCY LIST
                        Text page10 = Text.empty()
                                .append(Text.literal("10. Adjacency List\n\n").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.DARK_AQUA)))
                                .append(Text.literal("An Array of Lists.\n\n"))
                                .append(Text.literal("Each array slot contains a list of neighbors for that node.\n\n"))
                                .append(Text.literal("  [BUILD LIST]")
                                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN).withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/build_graph_lesson 10"))));

                        WrittenBookContentComponent content = new WrittenBookContentComponent(
                                RawFilteredPair.of("Graph Basics"), "Instructor", 0,
                                List.of(RawFilteredPair.of(page1), RawFilteredPair.of(page2), RawFilteredPair.of(page3), RawFilteredPair.of(page4),
                                        RawFilteredPair.of(page5), RawFilteredPair.of(page6), RawFilteredPair.of(page7), RawFilteredPair.of(page8),
                                        RawFilteredPair.of(page9), RawFilteredPair.of(page10)),
                                true
                        );
                        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, content);
                        context.getSource().getPlayer().giveItemStack(book);
                        return 1;
                    }));

            // --- COMMAND 2: BUILDER ---
            dispatcher.register(CommandManager.literal("build_graph_lesson")
                    .then(CommandManager.argument("id", IntegerArgumentType.integer())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                ServerWorld world = player.getServerWorld();
                                int id = IntegerArgumentType.getInteger(context, "id");

                                // Setup Coordinates (Wall 4 blocks ahead)
                                BlockPos start = player.getBlockPos().offset(player.getHorizontalFacing(), 4);
                                Direction facing = player.getHorizontalFacing();
                                Direction right = facing.rotateYClockwise();

                                GraphBuilder.clearArea(world, start, right);

                                // --- LESSON 1: DEFINITION (G = V, E) ---
                                if (id == 1) {
                                    // L-Shape Graph: 0 (Top) -- 1 (Corner) -- 2 (Right)
                                    BlockPos p0 = start.up(6);
                                    BlockPos p1 = start.up(2);
                                    BlockPos p2 = start.up(2).offset(right, 4);

                                    GraphBuilder.drawNode(world, p0, "V:0", facing);
                                    GraphBuilder.drawNode(world, p1, "V:1", facing);
                                    GraphBuilder.drawNode(world, p2, "V:2", facing);

                                    GraphBuilder.drawEdge(world, p0, p1, false, right, facing);
                                    GraphBuilder.drawEdge(world, p1, p2, false, right, facing);

                                    // Info Label
                                    GraphBuilder.drawLabel(world, start.up(4).offset(right, 2), "Graph G = (V, E)", 0x55FFFF, facing);
                                    player.sendMessage(Text.literal("Built: Graph Definition").formatted(Formatting.GREEN));
                                }

                                // --- LESSON 2: TYPES ---
                                else if (id == 2) {
                                    // Left: Undirected
                                    BlockPos u1 = start.up(6);
                                    BlockPos u2 = start.up(2);
                                    GraphBuilder.drawNode(world, u1, "A", facing);
                                    GraphBuilder.drawNode(world, u2, "B", facing);
                                    GraphBuilder.drawEdge(world, u1, u2, false, right, facing);
                                    GraphBuilder.drawLabel(world, u1.west(), "Undirected", 0xAAAAAA, facing);

                                    // Right: Directed
                                    BlockPos d1 = start.offset(right, 6).up(6);
                                    BlockPos d2 = start.offset(right, 6).up(2);
                                    GraphBuilder.drawNode(world, d1, "C", facing);
                                    GraphBuilder.drawNode(world, d2, "D", facing);
                                    GraphBuilder.drawEdge(world, d1, d2, true, right, facing);
                                    GraphBuilder.drawLabel(world, d1.east(), "Directed", 0xFFA500, facing);

                                    player.sendMessage(Text.literal("Built: Types Comparison").formatted(Formatting.YELLOW));
                                }

                                // --- LESSON 3: MULTIGRAPHS ---
                                else if (id == 3) {
                                    BlockPos m1 = start.up(2);
                                    BlockPos m2 = start.up(6);

                                    GraphBuilder.drawNode(world, m1, "A", facing);
                                    GraphBuilder.drawNode(world, m2, "B", facing);

                                    // Edge 1
                                    GraphBuilder.drawEdge(world, m1, m2, false, right, facing);
                                    // Edge 2 (Simulated by adjacent chain)
                                    GraphBuilder.drawEdge(world, m1.offset(right, 1), m2.offset(right, 1), false, right, facing);

                                    GraphBuilder.drawLabel(world, start.up(4).offset(right, 2), "Multiple Edges", 0xFFA500, facing);
                                    player.sendMessage(Text.literal("Built: Multigraph Example").formatted(Formatting.LIGHT_PURPLE));
                                }

                                // --- LESSON 4: VERTEX DEGREE ---
                                else if (id == 4) {
                                    BlockPos center = start.offset(right, 4).up(4);
                                    BlockPos up = center.up(4);
                                    BlockPos left = center.offset(right, -4);
                                    BlockPos rightNode = center.offset(right, 4);

                                    GraphBuilder.drawNode(world, center, "V", facing);
                                    GraphBuilder.drawNode(world, up, "In", facing);
                                    GraphBuilder.drawNode(world, left, "In", facing);
                                    GraphBuilder.drawNode(world, rightNode, "Out", facing);

                                    GraphBuilder.drawEdge(world, up, center, true, right, facing);
                                    GraphBuilder.drawEdge(world, left, center, true, right, facing);
                                    GraphBuilder.drawEdge(world, center, rightNode, true, right, facing);

                                    GraphBuilder.drawLabel(world, center.up(2).offset(right, 1), "In-Degree: 2", 0x00FF00, facing);
                                    GraphBuilder.drawLabel(world, center.down(1).offset(right, 1), "Out-Degree: 1", 0xFFA500, facing);

                                    player.sendMessage(Text.literal("Built: Vertex Degrees").formatted(Formatting.GOLD));
                                }

                                // --- LESSON 5: SUBGRAPHS ---
                                else if (id == 5) {
                                    // Original Graph G (Ghost/Glass) - Conceptual
                                    // We show Subgraph H solid
                                    BlockPos p1 = start.up(2);
                                    BlockPos p2 = start.up(6);
                                    BlockPos p3 = start.offset(right, 4).up(2);

                                    GraphBuilder.drawNode(world, p1, "1", facing);
                                    GraphBuilder.drawNode(world, p2, "2", facing);
                                    GraphBuilder.drawNode(world, p3, "3", facing);

                                    GraphBuilder.drawEdge(world, p1, p2, false, right, facing);

                                    GraphBuilder.drawLabel(world, start.up(4).offset(right, 2), "Subgraph H of G", 0x55FFFF, facing);
                                    GraphBuilder.drawLabel(world, p3.up(), "Isolated in H", 0xAAAAAA, facing);

                                    player.sendMessage(Text.literal("Built: Subgraph").formatted(Formatting.BLUE));
                                }

                                // --- LESSON 6: PATHS & CYCLES ---
                                else if (id == 6) {
                                    // Cycle 1-2-3-4-1
                                    BlockPos p1 = start.up(2);
                                    BlockPos p2 = start.up(6);
                                    BlockPos p3 = start.offset(right, 4).up(6);
                                    BlockPos p4 = start.offset(right, 4).up(2);

                                    GraphBuilder.drawNode(world, p1, "1", facing);
                                    GraphBuilder.drawNode(world, p2, "2", facing);
                                    GraphBuilder.drawNode(world, p3, "3", facing);
                                    GraphBuilder.drawNode(world, p4, "4", facing);

                                    GraphBuilder.drawEdge(world, p1, p2, true, right, facing);
                                    GraphBuilder.drawEdge(world, p2, p3, true, right, facing);
                                    GraphBuilder.drawEdge(world, p3, p4, true, right, facing);
                                    GraphBuilder.drawEdge(world, p4, p1, true, right, facing);

                                    GraphBuilder.drawLabel(world, start.offset(right, 2).up(4), "Cycle (1-2-3-4-1)", 0x55FFFF, facing);
                                    player.sendMessage(Text.literal("Built: Cycle").formatted(Formatting.DARK_GREEN));
                                }

                                // --- LESSON 7: CONNECTIVITY ---
                                else if (id == 7) {
                                    // Connected Component
                                    GraphBuilder.drawNode(world, start.up(2), "1", facing);
                                    GraphBuilder.drawNode(world, start.up(6), "2", facing);
                                    GraphBuilder.drawEdge(world, start.up(2), start.up(6), false, right, facing);
                                    GraphBuilder.drawLabel(world, start.up(4).offset(right, 1), "Connected", 0x00FF00, facing);

                                    // Isolated Node
                                    BlockPos island = start.offset(right, 6).up(4);
                                    GraphBuilder.drawNode(world, island, "3", facing);
                                    GraphBuilder.drawLabel(world, island.up(2), "Isolated", 0xFF0000, facing);

                                    player.sendMessage(Text.literal("Built: Connectivity").formatted(Formatting.RED));
                                }

                                // --- LESSON 8: BIPARTITE ---
                                else if (id == 8) {
                                    // Set V1 (Top)
                                    BlockPos v1a = start.up(6);
                                    BlockPos v1b = start.offset(right, 4).up(6);

                                    // Set V2 (Bottom)
                                    BlockPos v2a = start.up(2);
                                    BlockPos v2b = start.offset(right, 4).up(2);

                                    GraphBuilder.drawNode(world, v1a, "U1", facing);
                                    GraphBuilder.drawNode(world, v1b, "U2", facing);
                                    world.setBlockState(v1a, Blocks.DIAMOND_BLOCK.getDefaultState()); // Color code V1
                                    world.setBlockState(v1b, Blocks.DIAMOND_BLOCK.getDefaultState());

                                    GraphBuilder.drawNode(world, v2a, "V1", facing);
                                    GraphBuilder.drawNode(world, v2b, "V2", facing); // Color code V2 is Gold (default)

                                    // Edges ONLY between sets
                                    GraphBuilder.drawEdge(world, v1a, v2a, false, right, facing);
                                    GraphBuilder.drawEdge(world, v1b, v2b, false, right, facing);
                                    GraphBuilder.drawEdge(world, v1a, v2b, false, right, facing); // Diagonal-ish connection logic would be needed for true bipartite look, simplifying to straight lines or L-shapes if library supports

                                    GraphBuilder.drawLabel(world, start.offset(right, 2).up(8), "Set U (Diamond)", 0x55FFFF, facing);
                                    GraphBuilder.drawLabel(world, start.offset(right, 2).up(1), "Set V (Gold)", 0xFFA500, facing);

                                    player.sendMessage(Text.literal("Built: Bipartite Graph").formatted(Formatting.AQUA));
                                }

                                // --- LESSON 9: ADJACENCY MATRIX ---
                                else if (id == 9) {
                                    BlockPos p0 = start.up(6);
                                    BlockPos p1 = start.up(2);
                                    BlockPos p2 = start.up(2).offset(right, 4);

                                    GraphBuilder.drawNode(world, p0, "0", facing);
                                    GraphBuilder.drawNode(world, p1, "1", facing);
                                    GraphBuilder.drawNode(world, p2, "2", facing);

                                    GraphBuilder.drawEdge(world, p0, p1, false, right, facing); // 0-1
                                    GraphBuilder.drawEdge(world, p1, p2, false, right, facing); // 1-2

                                    BlockPos matrixStart = start.offset(right, 8).up(2);
                                    int[][] matrix = {
                                            {0, 1, 0},
                                            {1, 0, 1},
                                            {0, 1, 0}
                                    };

                                    for(int row=0; row<3; row++) {
                                        for(int col=0; col<3; col++) {
                                            BlockPos cell = matrixStart.offset(right, col).up(2 - row);
                                            if (matrix[row][col] == 1) {
                                                world.setBlockState(cell, Blocks.EMERALD_BLOCK.getDefaultState());
                                                GraphBuilder.drawLabel(world, cell, "1", 0x00FF00, facing);
                                            } else {
                                                world.setBlockState(cell, Blocks.IRON_BLOCK.getDefaultState());
                                                GraphBuilder.drawLabel(world, cell, "0", 0xAAAAAA, facing);
                                            }
                                        }
                                        GraphBuilder.drawLabel(world, matrixStart.offset(right, -1).up(2-row), "Row " + row, 0xFFFFFF, facing);
                                    }
                                    for(int col=0; col<3; col++) GraphBuilder.drawLabel(world, matrixStart.offset(right, col).up(3), "Col " + col, 0xFFFFFF, facing);

                                    GraphBuilder.drawLabel(world, matrixStart.offset(right, 1).up(5), "Adjacency Matrix", 0x55FFFF, facing);
                                    player.sendMessage(Text.literal("Built: Adjacency Matrix").formatted(Formatting.AQUA));
                                }

                                // --- LESSON 10: ADJACENCY LIST ---
                                else if (id == 10) {
                                    BlockPos p0 = start.up(6);
                                    BlockPos p1 = start.up(2);
                                    BlockPos p2 = start.up(2).offset(right, 4);

                                    GraphBuilder.drawNode(world, p0, "0", facing);
                                    GraphBuilder.drawNode(world, p1, "1", facing);
                                    GraphBuilder.drawNode(world, p2, "2", facing);
                                    GraphBuilder.drawEdge(world, p0, p1, false, right, facing);
                                    GraphBuilder.drawEdge(world, p1, p2, false, right, facing);

                                    BlockPos listStart = start.offset(right, 8).up(6);

                                    // Node 0 -> [1]
                                    buildLinkedList(world, listStart, 0, new int[]{1}, right, facing);

                                    // Node 1 -> [0, 2]
                                    buildLinkedList(world, listStart.down(2), 1, new int[]{0, 2}, right, facing);

                                    // Node 2 -> [1]
                                    buildLinkedList(world, listStart.down(4), 2, new int[]{1}, right, facing);

                                    GraphBuilder.drawLabel(world, listStart.up(2), "Adjacency List", 0x55FFFF, facing);
                                    player.sendMessage(Text.literal("Built: Adjacency List").formatted(Formatting.AQUA));
                                }

                                return 1;
                            })
                    ));
        });
    }

    private static void buildLinkedList(ServerWorld world, BlockPos start, int headIndex, int[] neighbors, Direction right, Direction facing) {
        // Array Index
        world.setBlockState(start, Blocks.OBSIDIAN.getDefaultState());
        GraphBuilder.drawLabel(world, start, "Idx " + headIndex, 0xFFA500, facing);

        BlockPos current = start;
        for (int neighbor : neighbors) {
            // Arrow/Chain
            GraphBuilder.drawEdge(world, current, current.offset(right, 2), true, right, facing);

            // Next Node
            current = current.offset(right, 2);
            world.setBlockState(current, Blocks.GOLD_BLOCK.getDefaultState());
            GraphBuilder.drawLabel(world, current, String.valueOf(neighbor), 0xFFFFFF, facing);
        }

        // Null Terminator
        GraphBuilder.drawEdge(world, current, current.offset(right, 2), true, right, facing);
        GraphBuilder.drawLabel(world, current.offset(right, 2), "NULL", 0xFF0000, facing);
    }
}
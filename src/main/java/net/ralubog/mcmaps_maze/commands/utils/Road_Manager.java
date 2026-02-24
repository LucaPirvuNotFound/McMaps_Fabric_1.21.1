package net.ralubog.mcmaps_maze.commands.utils;


import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.*;
import java.util.concurrent.CompletableFuture;

record NodeScore(double fScore, BlockPos pos) {}

public class Road_Manager {
    public static volatile boolean isVisible = false;
    public static final List<Pair<BlockPos, BlockState>> originalBlocks = Collections.synchronizedList(new ArrayList<>());

    public static List<BlockPos> WAYPOINTS_ASTAR = Collections.synchronizedList(new ArrayList<>());
    public static List<BlockPos> WAYPOINTS_DIJKSTRA = Collections.synchronizedList(new ArrayList<>());
    public static List<BlockPos> WAYPOINTS_BELLMAN_FORD = Collections.synchronizedList(new ArrayList<>());
    public static List<BlockPos> WAYPOINTS_GREEDY = Collections.synchronizedList(new ArrayList<>());

    private static double base_cost = 1.0;
    private static double slope_cost = 2.0;
    private static double water_cost = 4.0;

    // pentru a numara cate noduri accesez
    private static volatile int nr_blocks_astar = 0;
    private static volatile int nr_blocks_dijkstra = 0;
    private static volatile int nr_blocks_bellman_ford = 0;
    private static volatile int nr_blocks_greedy = 0;

    public static void reset_block_counter() {
        nr_blocks_astar = 0;
        nr_blocks_dijkstra = 0;
        nr_blocks_bellman_ford = 0;
        nr_blocks_greedy = 0;
    }

    public static void reset_waypoints() {
        WAYPOINTS_ASTAR.clear();
        WAYPOINTS_DIJKSTRA.clear();
        WAYPOINTS_BELLMAN_FORD.clear();
        WAYPOINTS_GREEDY.clear();
    }

    public static int getNr_blocks(String algo) {
        switch (algo) {
            case "astar" -> {
                return nr_blocks_astar;
            }
            case "dijkstra" -> {
                return nr_blocks_dijkstra;
            }
            case "bellman_ford" -> {
                return  nr_blocks_bellman_ford;
            }
            case "greedy" -> {
                return nr_blocks_greedy;
            }
        }
        return -1;
    }

    public static void updateCosts(String option) {
        // Resetam la default
        base_cost = 1.0;
        slope_cost = 2.0;
        water_cost = 4.0;

        switch (option) {
            case "-has_boat" -> water_cost = 1.2;
            case "-fly" -> slope_cost = 0.5;
            case "-aquaman" -> water_cost = 0.1;
        }
    }

    public static void return_to_original_state(ServerWorld world) {
        // Rulam restaurarea pe un fir de executie separat pentru a nu bloca jocul
        CompletableFuture.runAsync(() -> {
            synchronized (originalBlocks) {
                // Parcurgem lista invers (LIFO)
                for (int i = originalBlocks.size() - 1; i >= 0; i--) {
                    Pair<BlockPos, BlockState> entry = originalBlocks.get(i);
                    BlockPos pos = entry.getLeft();
                    BlockState originalState = entry.getRight();

                    // Trimitem comanda de setare a blocului catre thread-ul principal
                    world.getServer().execute(() -> {
                        world.setBlockState(pos, originalState);
                    });

                    // Aici controlam viteza de disparitie
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                originalBlocks.clear();
            }
        });
    }

    // Helper method to identify if a block is part of our visualization
    // This ensures we don't save a visualization block as an "original" block
    private static boolean isVisualizationBlock(BlockState state) {
        return state.isOf(Blocks.RED_CONCRETE) || state.isOf(Blocks.YELLOW_CONCRETE) ||
                state.isOf(Blocks.BLACK_CONCRETE) || state.isOf(Blocks.WHITE_CONCRETE) ||
                state.isOf(Blocks.BLUE_CONCRETE) || state.isOf(Blocks.LIGHT_BLUE_CONCRETE) ||
                state.isOf(Blocks.PURPLE_CONCRETE) || state.isOf(Blocks.PINK_CONCRETE) ||
                state.isOf(Blocks.ORANGE_CONCRETE); // Keeping orange just in case
    }

    public static double heuristic(BlockPos p1, BlockPos p2) {
        // AI FOLOSIT: Math.sqrt + Math.pow (Euclidean distance).
        // In Python codul tau folosea Manhattan distance (abs + abs).
        // Pentru Minecraft, Euclidean e ok, dar e mai lenta din cauza sqrt.
        return Math.sqrt(Math.pow((p1.getX() - p2.getX()), 2) + Math.pow((p1.getY() - p2.getY()), 2) + Math.pow((p1.getZ() - p2.getZ()), 2));
    }

    private static double get_cost(BlockPos current, BlockPos neighbour, ServerWorld world) {
        int h_current = current.getY();
        int h_neigh = neighbour.getY();

        double base_penalty, slope_penalty, water_penalty;

        //
        if (current.getX() != neighbour.getX() && current.getZ() != neighbour.getZ()) {
            base_penalty = 1.414;
        } else {
            base_penalty = 1.0;
        }
        base_penalty *= base_cost;

        // slope cost
        slope_penalty = -(h_neigh - h_current) > 1 ? Double.POSITIVE_INFINITY : Math.abs(h_neigh - h_current) * slope_cost;

        // water cost
        BlockState state = world.getBlockState(neighbour);
        if (state.isOf(Blocks.WATER)) {
            water_penalty = water_cost;
            if (current.getX() != neighbour.getX() && current.getZ() != neighbour.getZ()) {
                water_penalty *= 1.414;
            }
            return water_penalty;
        }

        return base_penalty + slope_penalty;
    }

    public static Map<BlockPos, Double> scanSurface(ServerWorld world, BlockPos center, int radius) {
        Map<BlockPos, Double> surfaceMap = new HashMap<>();

        // Calculam limitele in coordonate de blocuri
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // Obtinem cea mai inalta pozitie solida la coordonatele X, Z
                // WORLD_SURFACE include si apa; MOTION_BLOCKING ignora iarba/florile
                int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);

                BlockPos pos = new BlockPos(x, surfaceY - 1, z); // Scadem 1 pentru a fi pe bloc, nu deasupra

                // Initializam g_score cu infinit
                surfaceMap.put(pos, Double.POSITIVE_INFINITY);
            }
        }

        return surfaceMap;
    }

    public static double astar(BlockPos start, BlockPos goal, Map<BlockPos, Double> g_score, ServerWorld world, boolean realtime) {
        // PriorityQueue sortata dupa fScore (g_score + heuristic)
        PriorityQueue<NodeScore> openSet = new PriorityQueue<>(Comparator.comparingDouble(NodeScore::fScore));

        Map<BlockPos, BlockPos> came_from = new HashMap<>();

        // Will addd next part

        // Setam punctul de start
        // Presupunem ca start-ul a fost deja pus in g_score cu 0.0 in functia de scanare
        g_score.put(start, 0.0);
        openSet.add(new NodeScore(heuristic(start, goal), start));
        came_from.put(start, null);
        BlockPos lastCurrent = null; // Tinem evidenta nodului anterior pentru a-l face rosu

        while (!openSet.isEmpty()) {
            NodeScore temp = openSet.poll();
            BlockPos current = temp.pos();

            //contorizez aici cate blocuri am parcurs
            nr_blocks_astar++;

            // LOGICA REALTIME: Înlocuim blocul curent cu Red Concrete
            if (realtime) {

                final BlockPos final_lastCurrent = lastCurrent;
                // Executăm pe thread-ul principal pentru siguranță
                world.getServer().execute(() -> {
                    if (final_lastCurrent != null && world.getBlockState(final_lastCurrent).isOf(Blocks.YELLOW_CONCRETE)) {
                        world.setBlockState(final_lastCurrent, Blocks.RED_CONCRETE.getDefaultState());
                    }
 
                    BlockState currentState = world.getBlockState(current);
                    // Save state if it's not ANY visualization block
                    if (!isVisualizationBlock(currentState)) {
                        originalBlocks.add(new Pair<>(current, currentState));
                    }

                    // Current node becomes YELLOW
                    world.setBlockState(current, Blocks.YELLOW_CONCRETE.getDefaultState());
                });
                lastCurrent = current;
            }

            // Verificam daca am ajuns la tinta (ignoram mici diferente de Y daca e cazul)
            if (current.getX() == goal.getX() && current.getZ() == goal.getZ()) {
                WAYPOINTS_ASTAR = reconstructPath(came_from, current);
                return g_score.get(current);
            }

            // Explorăm vecinii doar pe X si Z
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;

                    // Cautam in g_score un punct care are coordonatele X+dx si Z+dz
                    // Trebuie sa gasim cheia (BlockPos) care se potriveste
                    BlockPos neighbor = findNeighborInMap(g_score, current.getX() + dx, current.getZ() + dz);

                    if (neighbor != null) {
                        double tentative_g_score = g_score.get(current) + get_cost(current, neighbor, world);

                        if (tentative_g_score < g_score.get(neighbor)) {
                            came_from.put(neighbor, current);
                            // DE STERS DACA NU MERGE
//                            WAYPOINTS.add(neighbor);

                            g_score.put(neighbor, tentative_g_score);

                            double fScore = tentative_g_score + heuristic(neighbor, goal);
                            openSet.add(new NodeScore(fScore, neighbor));
                        }
                    }
                }
            }
        }
        return 0; // returnam costul 0 daca nu am gasit un cost
    }

    public static double dijkstra(BlockPos start, BlockPos goal, Map<BlockPos, Double> g_score, ServerWorld world, boolean realtime) {
        // PriorityQueue sortata dupa fScore (g_score + heuristic)
        PriorityQueue<NodeScore> openSet = new PriorityQueue<>(Comparator.comparingDouble(NodeScore::fScore));

        Map<BlockPos, BlockPos> came_from = new HashMap<>();

        // Setam punctul de start
        // Presupunem ca start-ul a fost deja pus in g_score cu 0.0 in functia de scanare
        g_score.put(start, 0.0);
        openSet.add(new NodeScore(heuristic(start, goal), start));
        came_from.put(start, null);
        BlockPos lastCurrent = null; // Tinem evidenta nodului anterior pentru a-l face rosu

        while (!openSet.isEmpty()) {
            NodeScore temp = openSet.poll();
            BlockPos current = temp.pos();

            //contorizez aici cate blocuri am parcurs
            nr_blocks_dijkstra++;

            // LOGICA REALTIME: Înlocuim blocul curent cu Red Concrete
            if (realtime) {

                final BlockPos final_lastCurrent = lastCurrent;
                // Executăm pe thread-ul principal pentru siguranță
                world.getServer().execute(() -> {
                    // Previous node (WHITE) becomes BLACK
                    if (final_lastCurrent != null && world.getBlockState(final_lastCurrent).isOf(Blocks.WHITE_CONCRETE)) {
                        world.setBlockState(final_lastCurrent, Blocks.BLACK_CONCRETE.getDefaultState());
                    }

                    BlockState currentState = world.getBlockState(current);
                    if (!isVisualizationBlock(currentState)) {
                        originalBlocks.add(new Pair<>(current, currentState));
                    }

                    // Current node becomes WHITE
                    world.setBlockState(current, Blocks.WHITE_CONCRETE.getDefaultState());
                });

                // Update pentru referinta nodului anterior
                lastCurrent = current;
            }

            // Verificam daca am ajuns la tinta (ignoram mici diferente de Y daca e cazul)
            if (current.getX() == goal.getX() && current.getZ() == goal.getZ()) {
                WAYPOINTS_DIJKSTRA = reconstructPath(came_from, current);
                return g_score.get(current);
            }

            // Explorăm vecinii doar pe X si Z
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;

                    // Cautam in g_score un punct care are coordonatele X+dx si Z+dz
                    // Trebuie sa gasim cheia (BlockPos) care se potriveste
                    BlockPos neighbor = findNeighborInMap(g_score, current.getX() + dx, current.getZ() + dz);

                    if (neighbor != null) {
                        double tentative_g_score = g_score.get(current) + get_cost(current, neighbor, world);

                        if (tentative_g_score < g_score.get(neighbor)) {
                            came_from.put(neighbor, current);


                            g_score.put(neighbor, tentative_g_score);

                            openSet.add(new NodeScore(tentative_g_score, neighbor));
                        }
                    }
                }
            }
        }
        return 0; // returnam costul 0 daca nu am gasit un cost
    }

    public static double bellman_ford(BlockPos start, BlockPos goal, Map<BlockPos, Double> grid, ServerWorld world, boolean realtime) {
        // 1. Initialize distances
        // Unlike A*, Bellman-Ford iterates a specific number of times based on vertex count.
        int totalVertices = grid.size();

        // Initialize start distance to 0
        grid.put(start, 0.0);

        // Track paths
        Map<BlockPos, BlockPos> came_from = new HashMap<>();
        came_from.put(start, null);

        // Variable for visualization coloring (Red/Orange)
        // We use an AtomicReference or a wrapper array to use it inside lambdas if needed,
        // but here we can manage it within the scope since the loop is synchronous.
        BlockPos lastCurrent = null;

        // 2. Main Relaxation Loop
        // Run |V| - 1 times
        for (int i = 0; i < totalVertices - 1; i++) {
            boolean changed = false;

            // Create a copy of keys or iterate directly.
            // Since we don't add/remove keys from 'grid', iterating keySet is safe.
            // We convert to list to avoid ConcurrentModification if logic were to change,
            // but effectively we just need to iterate all nodes
            List<BlockPos> currentNodes = new ArrayList<>(grid.keySet());

            for (BlockPos current : currentNodes) {
                // Optimization: Skip nodes that haven't been reached yet (Distance is Infinity)
                // This matches your Python: "skip unreachable nodes"
                if (grid.get(current) == Double.POSITIVE_INFINITY) {
                    continue;
                }


                nr_blocks_bellman_ford++;
                if (realtime) {
                    final BlockPos final_lastCurrent = lastCurrent;
                    final BlockPos final_current = current;

                    world.getServer().execute(() -> {
                        // Previous node (LIGHT BLUE) becomes BLUE
                        if (final_lastCurrent != null && world.getBlockState(final_lastCurrent).isOf(Blocks.LIGHT_BLUE_CONCRETE)) {
                            world.setBlockState(final_lastCurrent, Blocks.BLUE_CONCRETE.getDefaultState());
                        }

                        BlockState currentState = world.getBlockState(final_current);
                        if (!isVisualizationBlock(currentState)) {
                            originalBlocks.add(new Pair<>(final_current, currentState));
                        }

                        // Current node becomes LIGHT BLUE
                        world.setBlockState(final_current, Blocks.LIGHT_BLUE_CONCRETE.getDefaultState());
                    });
                    lastCurrent = current;
                }

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) continue;

                        // Use existing helper to find the valid surface block at these coords
                        BlockPos neighbor = findNeighborInMap(grid, current.getX() + dx, current.getZ() + dz);

                        // If neighbor exists in our scanned surface map
                        if (neighbor != null) {
                            double weight = get_cost(current, neighbor, world);
                            double tentative_dist = grid.get(current) + weight;

                            // Relaxation step
                            if (tentative_dist < grid.get(neighbor)) {
                                grid.put(neighbor, tentative_dist);
                                came_from.put(neighbor, current);
                                changed = true;
                            }
                        }
                    }
                }
            }

            // Optimization: If no distances were updated in a full pass, stop early
            if (!changed) {
                break;
            }
        }

        // Path Reconstruction
        if (grid.get(goal) == Double.POSITIVE_INFINITY) {
            // Path not found
            return 0;
        }

        // Reconstruct path using the existing helper
        WAYPOINTS_BELLMAN_FORD = reconstructPath(came_from, goal);

        return grid.get(goal);
    }

    public static double greedy(BlockPos start, BlockPos goal, Map<BlockPos, Double> grid, ServerWorld world, boolean realtime) {
        // PriorityQueue sorted strictly by Heuristic (h)
        // Unlike A* which uses f = g + h, Greedy uses f = h
        PriorityQueue<NodeScore> openSet = new PriorityQueue<>(Comparator.comparingDouble(NodeScore::fScore));

        Map<BlockPos, BlockPos> came_from = new HashMap<>();

        // Initialize start
        // We use the passed 'grid' map to store the 'cost_so_far' for the final return value
        grid.put(start, 0.0);

        // Add start to queue with priority = heuristic
        openSet.add(new NodeScore(heuristic(start, goal), start));
        came_from.put(start, null);

        BlockPos lastCurrent = null; // For visualization

        while (!openSet.isEmpty()) {
            NodeScore temp = openSet.poll();
            BlockPos current = temp.pos();

            nr_blocks_greedy++;


            if (realtime) {
                final BlockPos final_lastCurrent = lastCurrent;
                final BlockPos final_current = current;

                world.getServer().execute(() -> {
                    // Previous node (PINK) becomes PURPLE
                    if (final_lastCurrent != null && world.getBlockState(final_lastCurrent).isOf(Blocks.PINK_CONCRETE)) {
                        world.setBlockState(final_lastCurrent, Blocks.PURPLE_CONCRETE.getDefaultState());
                    }

                    BlockState currentState = world.getBlockState(current);
                    if (!isVisualizationBlock(currentState)) {
                        originalBlocks.add(new Pair<>(current, currentState));
                    }

                    // Current node becomes PINK
                    world.setBlockState(current, Blocks.PINK_CONCRETE.getDefaultState());
                });

                lastCurrent = current;
            }


            // Check Goal
            if (current.getX() == goal.getX() && current.getZ() == goal.getZ()) {
                WAYPOINTS_GREEDY = reconstructPath(came_from, current);
                return grid.get(current); // Return actual cost incurred
            }

            // Neighbor Loop
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;

                    BlockPos neighbor = findNeighborInMap(grid, current.getX() + dx, current.getZ() + dz);

                    if (neighbor != null) {
                        // GREEDY LOGIC:
                        // We only care if we have NOT seen the node before.
                        // We do not update paths to nodes we've already seen, even if this way is cheaper
                        if (!came_from.containsKey(neighbor)) {

                            came_from.put(neighbor, current);

                            // Update the actual cost tracking (just for the final report)
                            double new_cost = grid.get(current) + get_cost(current, neighbor, world);
                            grid.put(neighbor, new_cost);

                            // Priority is PURELY the heuristic distance to goal
                            double priority = heuristic(neighbor, goal);
                            openSet.add(new NodeScore(priority, neighbor));
                        }
                    }
                }
            }
        }

        return 0; // Path not found
    }

    // Functie auxiliara pentru a gasi BlockPos-ul corect in Map dupa X si Z
    private static BlockPos findNeighborInMap(Map<BlockPos, Double> map, int x, int z) {
        // Aceasta parte poate fi lenta daca map-ul e urias.
        // O solutie mai buna ar fi sa stochezi map-ul ca Map<Long, Double> unde cheia e BlockPos.asLong()
        for (BlockPos pos : map.keySet()) {
            if (pos.getX() == x && pos.getZ() == z) {
                return pos;
            }
        }
        return null;
    }

    private static List<BlockPos> reconstructPath(Map<BlockPos, BlockPos> came_from, BlockPos current) {
        List<BlockPos> path = new ArrayList<>();
        while (current != null) {
            path.add(current);
            current = came_from.get(current);
        }
        Collections.reverse(path);
        return path;
    }
}
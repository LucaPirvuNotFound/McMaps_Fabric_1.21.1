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
    private static final List<Pair<BlockPos, BlockState>> originalBlocks = Collections.synchronizedList(new ArrayList<>());
    public static List<BlockPos> WAYPOINTS = Collections.synchronizedList(new ArrayList<>());
    private static double base_cost = 1.0;
    private static double slope_cost = 2.0;
    private static double water_cost = 4.0;

    // pentru a numara cate noduri accesez
    private static volatile int nr_blocks = 0;

    public static void reset_block_counter() {
        nr_blocks = 0;
    }

    public static int getNr_blocks() {
        return nr_blocks;
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

                    // Aici controlam viteza de disparitie (ex: 20 milisecunde intre blocuri)
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
        slope_penalty = Math.abs(h_neigh - h_current) > 1 ? 1000000.0 : Math.abs(h_neigh - h_current) * slope_cost;

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

                // Initializam g_score cu infinit, asa cum ai facut in Python
                surfaceMap.put(pos, Double.POSITIVE_INFINITY);
            }
        }

        return surfaceMap;
    }

    public static double astar(BlockPos start, BlockPos goal, Map<BlockPos, Double> g_score, ServerWorld world, boolean realtime) {
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
            nr_blocks++;

            // LOGICA REALTIME: Înlocuim blocul curent cu Red Concrete
            if (realtime) {

                final BlockPos final_lastCurrent = lastCurrent;
                // Executăm pe thread-ul principal pentru siguranță
                world.getServer().execute(() -> {
                    // Daca am avut un nod portocaliu inainte, il facem acum rosu
                    if (final_lastCurrent != null && world.getBlockState(final_lastCurrent).isOf(Blocks.ORANGE_CONCRETE)) {
                        world.setBlockState(final_lastCurrent, Blocks.RED_CONCRETE.getDefaultState());
                    }

                    // Salvam blocul original DOAR daca nu e deja unul pus de noi
                    BlockState currentState = world.getBlockState(current);
                    if (!currentState.isOf(Blocks.RED_CONCRETE) && !currentState.isOf(Blocks.ORANGE_CONCRETE)) {
                        originalBlocks.add(new Pair<>(current, currentState));
                    }

                    // Punem blocul portocaliu pentru nodul curent
                    world.setBlockState(current, Blocks.ORANGE_CONCRETE.getDefaultState());
                });

                // Update pentru referinta nodului anterior
                lastCurrent = current;
            }

            // Verificam daca am ajuns la tinta (ignoram mici diferente de Y daca e cazul)
            if (current.getX() == goal.getX() && current.getZ() == goal.getZ()) {
                WAYPOINTS = reconstructPath(came_from, current);
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
            nr_blocks++;

            // LOGICA REALTIME: Înlocuim blocul curent cu Red Concrete
            if (realtime) {

                final BlockPos final_lastCurrent = lastCurrent;
                // Executăm pe thread-ul principal pentru siguranță
                world.getServer().execute(() -> {
                    // Daca am avut un nod portocaliu inainte, il facem acum rosu
                    if (final_lastCurrent != null && world.getBlockState(final_lastCurrent).isOf(Blocks.ORANGE_CONCRETE)) {
                        world.setBlockState(final_lastCurrent, Blocks.RED_CONCRETE.getDefaultState());
                    }

                    // Salvam blocul original DOAR daca nu e deja unul pus de noi
                    BlockState currentState = world.getBlockState(current);
                    if (!currentState.isOf(Blocks.RED_CONCRETE) && !currentState.isOf(Blocks.ORANGE_CONCRETE)) {
                        originalBlocks.add(new Pair<>(current, currentState));
                    }

                    // Punem blocul portocaliu pentru nodul curent
                    world.setBlockState(current, Blocks.ORANGE_CONCRETE.getDefaultState());
                });

                // Update pentru referinta nodului anterior
                lastCurrent = current;
            }

            // Verificam daca am ajuns la tinta (ignoram mici diferente de Y daca e cazul)
            if (current.getX() == goal.getX() && current.getZ() == goal.getZ()) {
                WAYPOINTS = reconstructPath(came_from, current);
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

                            openSet.add(new NodeScore(tentative_g_score, neighbor));
                        }
                    }
                }
            }
        }
        return 0; // returnam costul 0 daca nu am gasit un cost
    }

public static double bellman_ford(BlockPos start, BlockPos goal, Map<BlockPos, Double> grid, ServerWorld world, boolean realtime) {
    return 0;
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
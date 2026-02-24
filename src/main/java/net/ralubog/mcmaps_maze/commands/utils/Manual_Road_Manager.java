package net.ralubog.mcmaps_maze.commands.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class Manual_Road_Manager {

    // --- A* STAR MANUAL ---
    public static void prepare_astar(BlockPos start, BlockPos goal, Map<BlockPos, Double> g_score, ServerWorld world) {
        PriorityQueue<NodeScore> openSet = new PriorityQueue<>(Comparator.comparingDouble(NodeScore::fScore));
        Map<BlockPos, BlockPos> came_from = new HashMap<>();

        g_score.put(start, 0.0);
        openSet.add(new NodeScore(Road_Manager.heuristic(start, goal), start));
        came_from.put(start, null);
        BlockPos lastCurrent = null;

        while (!openSet.isEmpty()) {
            NodeScore temp = openSet.poll();
            BlockPos current = temp.pos();

            // --- SAVE STEP INSTEAD OF PLACING BLOCKS ---
            List<AlgoDebugger.BlockChange> changes = new ArrayList<>();
            if (lastCurrent != null) {
                changes.add(new AlgoDebugger.BlockChange(lastCurrent, Blocks.RED_CONCRETE.getDefaultState(), false));
            }
            changes.add(new AlgoDebugger.BlockChange(current, Blocks.YELLOW_CONCRETE.getDefaultState(), true));
            AlgoDebugger.addStep(new AlgoDebugger.AlgoStep(changes));
            // -------------------------------------------

            lastCurrent = current;

            if (current.getX() == goal.getX() && current.getZ() == goal.getZ()) {
                Road_Manager.WAYPOINTS_ASTAR = reconstructPath(came_from, current);
                return;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos neighbor = findNeighborInMap(g_score, current.getX() + dx, current.getZ() + dz);

                    if (neighbor != null) {
                        double tentative_g_score = g_score.get(current) + get_cost(current, neighbor, world);
                        if (tentative_g_score < g_score.get(neighbor)) {
                            came_from.put(neighbor, current);
                            g_score.put(neighbor, tentative_g_score);
                            double fScore = tentative_g_score + Road_Manager.heuristic(neighbor, goal);
                            openSet.add(new NodeScore(fScore, neighbor));
                        }
                    }
                }
            }
        }
    }

    // --- DIJKSTRA MANUAL ---
    public static void prepare_dijkstra(BlockPos start, BlockPos goal, Map<BlockPos, Double> g_score, ServerWorld world) {
        PriorityQueue<NodeScore> openSet = new PriorityQueue<>(Comparator.comparingDouble(NodeScore::fScore));
        Map<BlockPos, BlockPos> came_from = new HashMap<>();

        g_score.put(start, 0.0);
        openSet.add(new NodeScore(Road_Manager.heuristic(start, goal), start));
        came_from.put(start, null);
        BlockPos lastCurrent = null;

        while (!openSet.isEmpty()) {
            NodeScore temp = openSet.poll();
            BlockPos current = temp.pos();

            List<AlgoDebugger.BlockChange> changes = new ArrayList<>();
            if (lastCurrent != null) {
                changes.add(new AlgoDebugger.BlockChange(lastCurrent, Blocks.BLACK_CONCRETE.getDefaultState(), false));
            }
            changes.add(new AlgoDebugger.BlockChange(current, Blocks.WHITE_CONCRETE.getDefaultState(), true));
            AlgoDebugger.addStep(new AlgoDebugger.AlgoStep(changes));

            lastCurrent = current;

            if (current.getX() == goal.getX() && current.getZ() == goal.getZ()) {
                Road_Manager.WAYPOINTS_DIJKSTRA = reconstructPath(came_from, current);
                return;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
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
    }

    // --- BELLMAN-FORD MANUAL ---
    public static void prepare_bellman_ford(BlockPos start, BlockPos goal, Map<BlockPos, Double> grid, ServerWorld world) {
        int totalVertices = grid.size();
        grid.put(start, 0.0);
        Map<BlockPos, BlockPos> came_from = new HashMap<>();
        came_from.put(start, null);
        BlockPos lastCurrent = null;

        for (int i = 0; i < totalVertices - 1; i++) {
            boolean changed = false;
            List<BlockPos> currentNodes = new ArrayList<>(grid.keySet());

            for (BlockPos current : currentNodes) {
                if (grid.get(current) == Double.POSITIVE_INFINITY) {
                    continue;
                }

                // --- SAVE STEP INSTEAD OF PLACING BLOCKS ---
                List<AlgoDebugger.BlockChange> changes = new ArrayList<>();
                if (lastCurrent != null) {
                    changes.add(new AlgoDebugger.BlockChange(lastCurrent, Blocks.BLUE_CONCRETE.getDefaultState(), false));
                }
                changes.add(new AlgoDebugger.BlockChange(current, Blocks.LIGHT_BLUE_CONCRETE.getDefaultState(), true));
                AlgoDebugger.addStep(new AlgoDebugger.AlgoStep(changes));
                // -------------------------------------------

                lastCurrent = current;

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) continue;

                        BlockPos neighbor = findNeighborInMap(grid, current.getX() + dx, current.getZ() + dz);

                        if (neighbor != null) {
                            double weight = get_cost(current, neighbor, world);
                            double tentative_dist = grid.get(current) + weight;

                            if (tentative_dist < grid.get(neighbor)) {
                                grid.put(neighbor, tentative_dist);
                                came_from.put(neighbor, current);
                                changed = true;
                            }
                        }
                    }
                }
            }

            if (!changed) {
                break;
            }
        }

        if (grid.get(goal) != Double.POSITIVE_INFINITY) {
            Road_Manager.WAYPOINTS_BELLMAN_FORD = reconstructPath(came_from, goal);
        }
    }

    // --- GREEDY MANUAL ---
    public static void prepare_greedy(BlockPos start, BlockPos goal, Map<BlockPos, Double> grid, ServerWorld world) {
        PriorityQueue<NodeScore> openSet = new PriorityQueue<>(Comparator.comparingDouble(NodeScore::fScore));
        Map<BlockPos, BlockPos> came_from = new HashMap<>();

        grid.put(start, 0.0);
        openSet.add(new NodeScore(Road_Manager.heuristic(start, goal), start));
        came_from.put(start, null);
        BlockPos lastCurrent = null;

        while (!openSet.isEmpty()) {
            NodeScore temp = openSet.poll();
            BlockPos current = temp.pos();

            // --- SAVE STEP INSTEAD OF PLACING BLOCKS ---
            List<AlgoDebugger.BlockChange> changes = new ArrayList<>();
            if (lastCurrent != null) {
                changes.add(new AlgoDebugger.BlockChange(lastCurrent, Blocks.PURPLE_CONCRETE.getDefaultState(), false));
            }
            changes.add(new AlgoDebugger.BlockChange(current, Blocks.PINK_CONCRETE.getDefaultState(), true));
            AlgoDebugger.addStep(new AlgoDebugger.AlgoStep(changes));
            // -------------------------------------------

            lastCurrent = current;

            if (current.getX() == goal.getX() && current.getZ() == goal.getZ()) {
                Road_Manager.WAYPOINTS_GREEDY = reconstructPath(came_from, current);
                return;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;

                    BlockPos neighbor = findNeighborInMap(grid, current.getX() + dx, current.getZ() + dz);

                    if (neighbor != null) {
                        if (!came_from.containsKey(neighbor)) {
                            came_from.put(neighbor, current);
                            double new_cost = grid.get(current) + get_cost(current, neighbor, world);
                            grid.put(neighbor, new_cost);
                            double priority = Road_Manager.heuristic(neighbor, goal);
                            openSet.add(new NodeScore(priority, neighbor));
                        }
                    }
                }
            }
        }
    }

    // --- HELPER METHODS COPIED FROM ROAD_MANAGER ---
    private static BlockPos findNeighborInMap(Map<BlockPos, Double> map, int x, int z) {
        for (BlockPos pos : map.keySet()) {
            if (pos.getX() == x && pos.getZ() == z) return pos;
        }
        return null;
    }

    private static double get_cost(BlockPos current, BlockPos neighbour, ServerWorld world) {
        int h_current = current.getY();
        int h_neigh = neighbour.getY();
        double base_penalty = (current.getX() != neighbour.getX() && current.getZ() != neighbour.getZ()) ? 1.414 : 1.0;
        double slope_penalty = Math.abs(h_neigh - h_current) > 1 ? Double.POSITIVE_INFINITY : Math.abs(h_neigh - h_current) * 2.0;

        BlockState state = world.getBlockState(neighbour);
        if (state.isOf(Blocks.WATER)) {
            double water_penalty = 4.0;
            if (current.getX() != neighbour.getX() && current.getZ() != neighbour.getZ()) water_penalty *= 1.414;
            return water_penalty;
        }
        return base_penalty + slope_penalty;
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
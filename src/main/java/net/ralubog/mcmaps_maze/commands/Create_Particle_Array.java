package net.ralubog.mcmaps_maze.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.ralubog.mcmaps_maze.commands.utils.Road_Manager;
import net.ralubog.mcmaps_maze.item.custom.RoadWandItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Create_Particle_Array {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // =================================================================================
            // COMMAND 1: /display_road <destination> ...
            // Uses Player Position -> Specified Destination Argument
            // =================================================================================
            dispatcher.register(CommandManager.literal("display_road")
                    .then(CommandManager.argument("destination", BlockPosArgumentType.blockPos())
                            .then(CommandManager.argument("option", StringArgumentType.word())
                                    .then(CommandManager.argument("realtime", BoolArgumentType.bool())
                                            // Default: A* only
                                            .executes(context -> {
                                                BlockPos playerPos = context.getSource().getPlayer().getBlockPos();
                                                BlockPos destPos = BlockPosArgumentType.getBlockPos(context, "destination");
                                                List<String> algos = new ArrayList<>();
                                                algos.add("astar");

                                                return runPathfinding(context.getSource(), playerPos, destPos,
                                                        StringArgumentType.getString(context, "option"),
                                                        BoolArgumentType.getBool(context, "realtime"), algos);
                                            })
                                            // Custom Algorithms
                                            .then(CommandManager.argument("algorithms", StringArgumentType.word())
                                                    .executes(context -> {
                                                        BlockPos playerPos = context.getSource().getPlayer().getBlockPos();
                                                        BlockPos destPos = BlockPosArgumentType.getBlockPos(context, "destination");

                                                        String algoArg = StringArgumentType.getString(context, "algorithms");
                                                        List<String> algos = processAlgorithms(algoArg);

                                                        if (algos.isEmpty()) {
                                                            context.getSource().sendError(Text.literal("Invalid algorithm! Use: -astar, -dijkstra, -bellman_ford, -greedy or -all"));
                                                            return 0;
                                                        }

                                                        return runPathfinding(context.getSource(), playerPos, destPos,
                                                                StringArgumentType.getString(context, "option"),
                                                                BoolArgumentType.getBool(context, "realtime"), algos);
                                                    }))
                                    )
                            )
                    )
            );

            // =================================================================================
            // COMMAND 2: /find_path ...
            // Uses Road Wand Start -> Road Wand End
            // =================================================================================
            dispatcher.register(CommandManager.literal("find_path")
                    .then(CommandManager.argument("option", StringArgumentType.word())
                            .then(CommandManager.argument("realtime", BoolArgumentType.bool())
                                    // Default: A* only
                                    .executes(context -> {
                                        // Validate Wand Coordinates
                                        if (RoadWandItem.startPos == null || RoadWandItem.endPos == null) {
                                            context.getSource().sendError(Text.literal("Error: You must select both a Start and End point using the Road Wand first!").formatted(Formatting.RED));
                                            return 0;
                                        }

                                        List<String> algos = new ArrayList<>();
                                        algos.add("astar");

                                        return runPathfinding(context.getSource(), RoadWandItem.startPos, RoadWandItem.endPos,
                                                StringArgumentType.getString(context, "option"),
                                                BoolArgumentType.getBool(context, "realtime"), algos);
                                    })
                                    // Custom Algorithms
                                    .then(CommandManager.argument("algorithms", StringArgumentType.word())
                                            .executes(context -> {
                                                // Validate Wand Coordinates
                                                if (RoadWandItem.startPos == null || RoadWandItem.endPos == null) {
                                                    context.getSource().sendError(Text.literal("Error: You must select both a Start and End point using the Road Wand first!").formatted(Formatting.RED));
                                                    return 0;
                                                }

                                                String algoArg = StringArgumentType.getString(context, "algorithms");
                                                List<String> algos = processAlgorithms(algoArg);

                                                if (algos.isEmpty()) {
                                                    context.getSource().sendError(Text.literal("Invalid algorithm! Use: -astar, -dijkstra, -bellman_ford, -greedy or -all"));
                                                    return 0;
                                                }

                                                return runPathfinding(context.getSource(), RoadWandItem.startPos, RoadWandItem.endPos,
                                                        StringArgumentType.getString(context, "option"),
                                                        BoolArgumentType.getBool(context, "realtime"), algos);
                                            }))
                            )
                    )
            );
        });
    }

    private static List<String> processAlgorithms(String input) {
        List<String> result = new ArrayList<>();
        // Lista cu algoritmii permisi pentru validare
        List<String> validAlgos = List.of("astar", "dijkstra", "bellman_ford", "greedy");

        // Cazul special pentru toti
        if (input.equals("-all")) {
            result.addAll(validAlgos);
            return result;
        }

        String[] parts = input.split("-");

        for (String part : parts) {
            // Ignoram string-urile goale (care apar daca inputul incepe cu -)
            if (part.isEmpty()) continue;

            // Daca partea extrasa este un algoritm valid, il adaugam in lista
            if (validAlgos.contains(part)) {
                // Evitam duplicatele daca utilizatorul scrie -astar-astar
                if (!result.contains(part)) {
                    result.add(part);
                }
            }
        }

        return result;
    }

    // Renamed to runPathfinding and cleaned up to accept explicit Start/Stop coordinates
    private static int runPathfinding(ServerCommandSource source, BlockPos start, BlockPos stop, String option, boolean realtime, List<String> algorithms) {
        if (source.getPlayer() == null) return 0;

        source.sendFeedback(() -> Text.literal("Calculation started for: " + algorithms.toString()), false);

        ServerWorld world = source.getWorld();

        // Ensure coordinates are valid (snap to surface if needed)
        final BlockPos final_stop = correct_height(world, stop);
        final BlockPos final_start = start; // Start is usually ground level if player/wand selected correctly

        // --- Visualization Legend ---
        if (realtime) {
            source.sendFeedback(() -> Text.literal("--- VISUALIZATION LEGEND ---").formatted(Formatting.GRAY), false);
            for (String algo : algorithms) {
                switch (algo) {
                    case "astar" -> source.sendFeedback(() ->
                            Text.literal("Astar: Base-").append(Text.literal("Red").formatted(Formatting.RED))
                                    .append(Text.literal("; Current-").append(Text.literal("Yellow").formatted(Formatting.YELLOW))), false);

                    case "dijkstra" -> source.sendFeedback(() ->
                            Text.literal("Dijkstra: Base-").append(Text.literal("Black").formatted(Formatting.BLACK))
                                    .append(Text.literal("; Current-").append(Text.literal("White").formatted(Formatting.WHITE))), false);

                    case "bellman_ford" -> source.sendFeedback(() ->
                            Text.literal("Bellman_Ford: Base-").append(Text.literal("Blue").formatted(Formatting.BLUE))
                                    .append(Text.literal("; Current-").append(Text.literal("Aqua").formatted(Formatting.AQUA))), false);

                    case "greedy" -> source.sendFeedback(() ->
                            Text.literal("Greedy: Base-").append(Text.literal("Purple").formatted(Formatting.DARK_PURPLE))
                                    .append(Text.literal("; Current-").append(Text.literal("Pink").formatted(Formatting.LIGHT_PURPLE))), false);
                }
            }
        }

        Road_Manager.reset_waypoints();

        // Run calculation on a separate thread
        CompletableFuture.runAsync(() -> {
            try {
                Road_Manager.updateCosts(option);

                // Scan surface
                Map<BlockPos, Double> baseMap = Road_Manager.scanSurface(world, final_start, (int) (Road_Manager.heuristic(final_start, final_stop) * 1.30));

                for (String algo : algorithms) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            Map<BlockPos, Double> algoMap = new HashMap<>(baseMap);

                            Road_Manager.reset_block_counter();

                            long start_time = System.nanoTime();
                            double cost = 0;
                            boolean success = false;
                            String colorName = "unknown";
                            Formatting format = Formatting.WHITE;

                            switch (algo) {
                                case "astar" -> {
                                    cost = Road_Manager.astar(final_start, final_stop, algoMap, world, realtime);
                                    success = !Road_Manager.WAYPOINTS_ASTAR.isEmpty();
                                    colorName = "green";
                                    format = Formatting.GREEN;
                                }
                                case "dijkstra" -> {
                                    cost = Road_Manager.dijkstra(final_start, final_stop, algoMap, world, realtime);
                                    success = !Road_Manager.WAYPOINTS_DIJKSTRA.isEmpty();
                                    colorName = "white";
                                }
                                case "bellman_ford" -> {
                                    cost = Road_Manager.bellman_ford(final_start, final_stop, algoMap, world, realtime);
                                    success = !Road_Manager.WAYPOINTS_BELLMAN_FORD.isEmpty();
                                    colorName = "purple";
                                    format = Formatting.LIGHT_PURPLE;
                                }
                                case "greedy" -> {
                                    cost = Road_Manager.greedy(final_start, final_stop, algoMap, world, realtime);
                                    success = !Road_Manager.WAYPOINTS_GREEDY.isEmpty();
                                    colorName = "black";
                                    format = Formatting.BLACK;
                                }
                            }

                            double duration = (System.nanoTime() - start_time) / 1_000_000_000.0;
                            final double finalCost = cost;
                            final boolean finalSuccess = success;
                            final String finalColor = colorName;
                            final Formatting finalFormat = format;

                            // Display results on main server thread
                            source.getServer().execute(() -> {
                                source.sendFeedback(() -> Text.literal(" "), false);
                                source.sendFeedback(() -> Text.literal("--- RESULTS [" + algo.toUpperCase() + "] ---").formatted(Formatting.GOLD), false);

                                if (!finalSuccess) {
                                    source.sendError(Text.literal("No path found with " + algo));
                                } else {
                                    Road_Manager.isVisible = true;
                                    source.sendFeedback(() -> Text.literal("Path calculated successfully! [" + algo.toUpperCase() + "]").formatted(Formatting.GREEN), false);
                                    source.sendFeedback(() -> Text.literal("Path cost: " + String.format("%.2f", finalCost)), false);
                                    source.sendFeedback(() -> Text.literal("Visited nodes (approx): " + Road_Manager.getNr_blocks(algo)), false);
                                    source.sendFeedback(() -> Text.literal("Execution time: " + String.format("%.4f", duration) + " seconds"), false);
                                    source.sendFeedback(() -> Text.literal("PATH COLOR: ").append(finalColor).formatted(finalFormat), false);
                                }
                            });

                        } catch (Exception e) {
                            source.getServer().execute(() -> source.sendError(Text.literal("Error at " + algo + ": " + e.getMessage())));
                        }
                    });
                }
            } catch (Exception e) {
                source.getServer().execute(() -> source.sendError(Text.literal("Error calculating path: " + e.getMessage())));
            }
        });

        return 1;
    }

    private static BlockPos correct_height(ServerWorld world, BlockPos pos) {
        int x = pos.getX();
        int z = pos.getZ();

        // Obținem Y-ul primului bloc de aer de deasupra, apoi scădem 1 ca să ajungem la blocul solid/lichid
        int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;

        // Condiția cerută: dacă Y-ul primit e mai mare decât suprafața, îl coborâm la suprafață.
        // Altfel, îl lăsăm așa cum e.
        if (pos.getY() > surfaceY) {
            return new BlockPos(x, surfaceY, z);
        }

        return pos;
    }
}
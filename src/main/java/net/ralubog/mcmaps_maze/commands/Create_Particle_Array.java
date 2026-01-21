package net.ralubog.mcmaps_maze.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.ralubog.mcmaps_maze.commands.utils.Road_Manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Create_Particle_Array {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                // În Create_Particle_Array.register()
                dispatcher.register(CommandManager.literal("display_road")
                        .then(CommandManager.argument("destination", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("option", StringArgumentType.word())
                                        .then(CommandManager.argument("realtime", BoolArgumentType.bool()) // Argument nou
                                                .executes(context -> {
                                                    List<String> algos = new ArrayList<>();
                                                    algos.add("astar");
                                                    return executePathfindingAsync(context.getSource(),
                                                            BlockPosArgumentType.getBlockPos(context, "destination"),
                                                            StringArgumentType.getString(context, "option"),
                                                            BoolArgumentType.getBool(context, "realtime"), algos);
                                                })
                                                // Varianta cu argumentul de algoritmi
                                                .then(CommandManager.argument("algorithms", StringArgumentType.word())
                                                        .executes(context -> {
                                                            String algoArg = StringArgumentType.getString(context, "algorithms");
                                                            List<String> algos = processAlgorithms(algoArg);

                                                            if (algos.isEmpty()) {
                                                                context.getSource().sendError(Text.literal("Algoritm invalid! Folositi: -astar, -dijkstra, -bellman_ford, -greedy sau -all"));
                                                                return 0;
                                                            }

                                                            return executePathfindingAsync(context.getSource(),
                                                                    BlockPosArgumentType.getBlockPos(context, "destination"),
                                                                    StringArgumentType.getString(context, "option"),
                                                                    BoolArgumentType.getBool(context, "realtime"), algos);
                                                        }))
                                        )
                                )
                        )
                );
        });
    }

    private static List<String> processAlgorithms(String input) {
        List<String> result = new ArrayList<>();
        if (input.equals("-all")) {
            result.add("astar");
            result.add("dijkstra");
            result.add("bellman_ford");
            result.add("greedy");
        } else if (input.startsWith("-")) {
            String algo = input.substring(1); // scoatem "-"
            if (List.of("astar", "dijkstra", "bellman_ford", "greedy").contains(algo)) {
                result.add(algo);
            }
        }
        return result;
    }

    private static int executePathfindingAsync(ServerCommandSource source, BlockPos stop, String option, boolean realtime, List<String> algorithms) {
        if (source.getPlayer() == null) return 0;

        BlockPos start = source.getPlayer().getBlockPos();
        ServerWorld world = source.getWorld();

        source.sendFeedback(() -> Text.literal("Calculul inceput pentru: " + algorithms.toString()), false);

        // Pornim calculul pe un alt thread
        CompletableFuture.runAsync(() -> {
            try {
                // Actualizam setarile in functie de optiune
//                Road_Manager.updateCosts(option); maybe

                // Executam scanarea si A*
                Map<BlockPos, Double> g_score = Road_Manager.scanSurface(world, start, (int) (Road_Manager.heuristic(start, stop) * 1.30));

                long start_time = System.nanoTime();

                Road_Manager.reset_block_counter();
                double cost = Road_Manager.astar(start, stop, g_score, world, realtime);

                double duration = (System.nanoTime() - start_time) / 1_000_000_000.0; //o secunda = 1.000.000.000 nanosec

                // Ne intoarcem pe thread-ul principal pentru a seta rezultatele finale
                source.getServer().execute(() -> {
                    if (Road_Manager.WAYPOINTS.isEmpty()) {
                        source.sendError(Text.literal("Nu a fost gasit niciun drum!"));
                    } else {
                        Road_Manager.isVisible = true;
                        source.sendFeedback(() -> Text.literal("Drum calculat cu succes!"), false);
                        source.sendFeedback(() -> Text.literal("Costul drumului: " + String.format("%.2f", cost)), false);
                        source.sendFeedback(() -> Text.literal("Au fost parcurse " + Road_Manager.getNr_blocks() + " noduri"), false);
                        source.sendFeedback(() -> Text.literal("Drum calculat in " + String.format("%.4f", duration) + " secunde"), false);
                    }
                });
            } catch (Exception e) {
                source.getServer().execute(() -> source.sendError(Text.literal("Eroare la calcularea drumului: " + e.getMessage())));
            }
        });

        return 1;
    }
}
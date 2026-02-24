package net.ralubog.mcmaps_maze.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.ralubog.mcmaps_maze.commands.utils.AlgoDebugger;
import net.ralubog.mcmaps_maze.commands.utils.Manual_Road_Manager;
import net.ralubog.mcmaps_maze.commands.utils.Road_Manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AlgoDebugCommand {

    // Store the last used delay so the "Continue" key knows how fast to go!
    private static double lastDelay = 0.5;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 1. COMMAND: /algo_debug <algorithms>
            dispatcher.register(CommandManager.literal("algo_debug")
                    .then(CommandManager.argument("algorithms", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;

                                String algoArg = StringArgumentType.getString(context, "algorithms");
                                List<String> algos = processAlgorithms(algoArg);

                                if (algos.isEmpty()) {
                                    context.getSource().sendError(Text.literal("Invalid algorithm! Use: astar, dijkstra, bellman_ford, greedy (or combinations like -astar-dijkstra)"));
                                    return 0;
                                }

                                ServerWorld world = context.getSource().getWorld();
                                BlockPos playerPos = player.getBlockPos();

                                BlockPos detectedStart = null;
                                BlockPos detectedGoal = null;

                                BlockPos mapStart = playerPos;
                                int mapSize = 30;

                                for (int dx = 0; dx <= mapSize; dx++) {
                                    for (int dz = 0; dz <= mapSize; dz++) {
                                        int px = mapStart.getX() + dx;
                                        int pz = mapStart.getZ() + dz;

                                        int py = world.getTopY(Heightmap.Type.WORLD_SURFACE, px, pz) - 1;
                                        BlockPos surfacePos = new BlockPos(px, py, pz);

                                        if (detectedGoal == null) {
                                            if (world.getBlockState(surfacePos).isOf(Blocks.DIAMOND_BLOCK)) {
                                                detectedGoal = surfacePos;
                                            } else if (world.getBlockState(surfacePos.down()).isOf(Blocks.DIAMOND_BLOCK)) {
                                                detectedGoal = surfacePos.down();
                                            }
                                        }

                                        if (detectedStart == null) {
                                            if (world.getBlockState(surfacePos).isOf(Blocks.GOLD_BLOCK)) {
                                                detectedStart = surfacePos;
                                            } else if (world.getBlockState(surfacePos.down()).isOf(Blocks.GOLD_BLOCK)) {
                                                detectedStart = surfacePos.down();
                                            }
                                        }
                                    }
                                    if (detectedGoal != null && detectedStart != null) break;
                                }

                                final BlockPos start;
                                if (detectedStart != null) {
                                    start = detectedStart.up();
                                    context.getSource().sendFeedback(() -> Text.literal("Start Locked: Gold Block found!").formatted(Formatting.YELLOW), false);
                                } else {
                                    start = playerPos;
                                    context.getSource().sendFeedback(() -> Text.literal("Warning: No Gold Block found! Using player position.").formatted(Formatting.GOLD), false);
                                }

                                final BlockPos goal;
                                if (detectedGoal != null) {
                                    goal = detectedGoal;
                                    context.getSource().sendFeedback(() -> Text.literal("Target Locked: Diamond Block found!").formatted(Formatting.AQUA), false);
                                } else {
                                    goal = start.add(20, 0, 20);
                                    context.getSource().sendFeedback(() -> Text.literal("Warning: No Diamond Block found! Using default distance.").formatted(Formatting.RED), false);
                                }

                                AlgoDebugger.clear();
                                context.getSource().sendFeedback(() -> Text.literal("Preparing " + algos.toString() + " for manual debug...").formatted(Formatting.GRAY), false);

                                CompletableFuture.runAsync(() -> {
                                    Map<BlockPos, Double> baseMap = Road_Manager.scanSurface(world, start, 50);

                                    for (String algo : algos) {
                                        AlgoDebugger.startRecording(algo);
                                        Map<BlockPos, Double> algoMap = new HashMap<>(baseMap);

                                        if (algo.equals("astar")) {
                                            Manual_Road_Manager.prepare_astar(start, goal, algoMap, world);
                                        } else if (algo.equals("dijkstra")) {
                                            Manual_Road_Manager.prepare_dijkstra(start, goal, algoMap, world);
                                        } else if (algo.equals("bellman_ford")) {
                                            Manual_Road_Manager.prepare_bellman_ford(start, goal, algoMap, world);
                                        } else if (algo.equals("greedy")) {
                                            Manual_Road_Manager.prepare_greedy(start, goal, algoMap, world);
                                        }
                                    }

                                    context.getSource().getServer().execute(() -> {
                                        context.getSource().sendFeedback(() -> Text.literal("Ready! Max " + AlgoDebugger.getMaxSteps() + " steps calculated. Use Arrow Keys to step.").formatted(Formatting.GREEN), false);
                                    });
                                });

                                return 1;
                            })));

            // 2. COMMAND: /algo_step <number> (FORWARD)
            dispatcher.register(CommandManager.literal("algo_step")
                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                int count = IntegerArgumentType.getInteger(context, "count");
                                ServerWorld world = context.getSource().getWorld();

                                if (AlgoDebugger.allSavedSteps.isEmpty()) {
                                    context.getSource().sendError(Text.literal("No steps loaded! Please prepare steps first."));
                                    return 0;
                                }

                                if (AlgoDebugger.isFinished()) {
                                    context.getSource().sendFeedback(() -> Text.literal("Already finished! You cannot go further.").formatted(Formatting.RED), false);
                                    return 0;
                                }

                                int played = AlgoDebugger.playNextSteps(count, world);
                                context.getSource().sendFeedback(() -> Text.literal("Played " + played + " steps. (Total: " + AlgoDebugger.currentStep + "/" + AlgoDebugger.getMaxSteps() + ")").formatted(Formatting.YELLOW), false);

                                if (AlgoDebugger.isFinished()) {
                                    context.getSource().sendFeedback(() -> Text.literal("============================").formatted(Formatting.GREEN).styled(style -> style.withBold(true)), false);
                                    context.getSource().sendFeedback(() -> Text.literal("RACE FINISHED!").formatted(Formatting.GREEN).styled(style -> style.withBold(true)), false);
                                    context.getSource().sendFeedback(() -> Text.literal("============================").formatted(Formatting.GREEN).styled(style -> style.withBold(true)), false);
                                }

                                return 1;
                            })));

            // 3. COMMAND: /algo_step_back <number> (BACKWARD)
            dispatcher.register(CommandManager.literal("algo_step_back")
                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                int count = IntegerArgumentType.getInteger(context, "count");
                                ServerWorld world = context.getSource().getWorld();

                                if (AlgoDebugger.allSavedSteps.isEmpty()) {
                                    context.getSource().sendError(Text.literal("No steps loaded!"));
                                    return 0;
                                }

                                if (AlgoDebugger.currentStep <= 0) {
                                    context.getSource().sendFeedback(() -> Text.literal("Already at the very beginning!").formatted(Formatting.RED), false);
                                    return 0;
                                }

                                int reverted = AlgoDebugger.playPreviousSteps(count, world);
                                context.getSource().sendFeedback(() -> Text.literal("Reverted " + reverted + " steps. (Total: " + AlgoDebugger.currentStep + "/" + AlgoDebugger.getMaxSteps() + ")").formatted(Formatting.GOLD), false);

                                return 1;
                            })));

            // 4. COMMAND: /algo_pause (PAUSE)
            dispatcher.register(CommandManager.literal("algo_pause")
                    .executes(context -> {
                        AlgoDebugger.pauseAuto();
                        context.getSource().sendFeedback(() -> Text.literal("Auto-play PAUSED!").formatted(Formatting.YELLOW), false);
                        return 1;
                    }));

            // 5. COMMAND: /algo_resume (CONTINUE)
            dispatcher.register(CommandManager.literal("algo_resume")
                    .executes(context -> {
                        AlgoDebugger.resumeAuto();

                        // Restart the background loop using the last known delay in case it completely stopped
                        ServerWorld world = context.getSource().getWorld();
                        AlgoDebugger.startAutoPlay(world, lastDelay);

                        context.getSource().sendFeedback(() -> Text.literal("Auto-play RESUMED!").formatted(Formatting.GREEN), false);
                        return 1;
                    }));

            // 6. COMMAND: /algo_auto <delay> (START AUTO-PLAY)
            dispatcher.register(CommandManager.literal("algo_auto")
                    .then(CommandManager.argument("delay", DoubleArgumentType.doubleArg(0))
                            .executes(context -> {
                                double delay = DoubleArgumentType.getDouble(context, "delay");
                                lastDelay = delay; // Save this delay for the resume key!

                                ServerWorld world = context.getSource().getWorld();

                                if (AlgoDebugger.allSavedSteps.isEmpty()) {
                                    context.getSource().sendError(Text.literal("No steps loaded!"));
                                    return 0;
                                }

                                if (AlgoDebugger.isFinished()) {
                                    context.getSource().sendFeedback(() -> Text.literal("Already finished!").formatted(Formatting.RED), false);
                                    return 0;
                                }

                                AlgoDebugger.startAutoPlay(world, delay);
                                context.getSource().sendFeedback(() -> Text.literal("Auto-play started! Delay: " + delay + "s").formatted(Formatting.GREEN), false);
                                return 1;
                            })));

            // 7. COMMAND: /algo_reset (CLEARS LABELS & RESTORES BLOCKS)
            dispatcher.register(CommandManager.literal("algo_reset")
                    .executes(context -> {
                        ServerWorld world = context.getSource().getWorld();

                        // Clear the debugger memory and floating labels
                        AlgoDebugger.clear();

                        // Smoothly revert all blocks back to their original state
                        Road_Manager.return_to_original_state(world);

                        context.getSource().sendFeedback(() -> Text.literal("Debugger reset and labels cleared!").formatted(Formatting.GREEN), false);
                        return 1;
                    }));
        });
    }

    private static List<String> processAlgorithms(String input) {
        List<String> result = new ArrayList<>();
        List<String> validAlgos = List.of("astar", "dijkstra", "bellman_ford", "greedy");

        if (input.equals("-all")) {
            result.addAll(validAlgos);
            return result;
        }

        if (validAlgos.contains(input)) {
            result.add(input);
            return result;
        }

        String[] parts = input.split("-");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (validAlgos.contains(part) && !result.contains(part)) {
                result.add(part);
            }
        }

        return result;
    }
}
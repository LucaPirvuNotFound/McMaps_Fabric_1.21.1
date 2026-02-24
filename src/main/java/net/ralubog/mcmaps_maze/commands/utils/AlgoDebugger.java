package net.ralubog.mcmaps_maze.commands.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AlgoDebugger {

    public static class BlockChange {
        public final BlockPos pos;
        public final BlockState newState;
        public final boolean saveOriginal;
        public BlockState stateToRestore;
        public String algo;

        public BlockChange(BlockPos pos, BlockState newState, boolean saveOriginal) {
            this.pos = pos;
            this.newState = newState;
            this.saveOriginal = saveOriginal;
            this.stateToRestore = null;
            this.algo = null;
        }
    }

    public record AlgoStep(List<BlockChange> changes) {}

    public static final Map<String, List<AlgoStep>> allSavedSteps = new ConcurrentHashMap<>();
    public static final Map<BlockPos, Map<String, Integer>> activeVisitors = new ConcurrentHashMap<>();
    private static final Map<String, DisplayEntity.TextDisplayEntity> algoLabels = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> recordingAlgo = new ThreadLocal<>();

    public static int currentStep = 0;
    public static BlockPos lastGoal = null;

    // --- AUTO-PLAY STATE VARIABLES ---
    public static volatile boolean isAutoPaused = false;
    private static volatile boolean isAutoRunning = false; // Prevents multiple auto-plays overlapping

    public static void clear() {
        for (DisplayEntity.TextDisplayEntity label : algoLabels.values()) {
            if (label != null) label.discard();
        }
        algoLabels.clear();

        allSavedSteps.clear();
        activeVisitors.clear();
        currentStep = 0;
        isAutoPaused = false;
        isAutoRunning = false;
        Road_Manager.isVisible = false;
        Road_Manager.reset_waypoints();
    }

    public static void startRecording(String algo) {
        recordingAlgo.set(algo);
        allSavedSteps.put(algo, new ArrayList<>());
    }

    public static void addStep(AlgoStep step) {
        String algo = recordingAlgo.get();
        if (algo != null) {
            for (BlockChange change : step.changes()) {
                change.algo = algo;
            }
            allSavedSteps.get(algo).add(step);
        }
    }

    public static int getMaxSteps() {
        return allSavedSteps.values().stream().mapToInt(List::size).max().orElse(0);
    }

    // --- AUTO-PLAY CONTROLS ---
    public static void pauseAuto() {
        isAutoPaused = true;
    }

    public static void resumeAuto() {
        isAutoPaused = false;
    }

    public static void startAutoPlay(ServerWorld world, double delaySeconds) {
        if (isAutoRunning || isFinished()) return;

        isAutoRunning = true;
        isAutoPaused = false;
        long delayMillis = (long) (delaySeconds * 1000);

        CompletableFuture.runAsync(() -> {
            try {
                while (!isFinished() && isAutoRunning) {
                    if (isAutoPaused) {
                        Thread.sleep(100); // Check every 100ms if unpaused
                        continue;
                    }

                    playNextSteps(1, world);

                    if (delayMillis > 0) {
                        Thread.sleep(delayMillis);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                isAutoRunning = false;
            }
        });
    }

    // --- STEP FORWARD (PARALLEL) ---
    public static int playNextSteps(int count, ServerWorld world) {
        if (allSavedSteps.isEmpty()) return 0;
        int stepsPlayed = 0;
        int maxSteps = getMaxSteps();

        for (int i = 0; i < count; i++) {
            if (currentStep >= maxSteps) break;

            List<BlockChange> combinedChanges = new ArrayList<>();
            for (List<AlgoStep> steps : allSavedSteps.values()) {
                if (currentStep < steps.size()) {
                    combinedChanges.addAll(steps.get(currentStep).changes());
                }
            }

            world.getServer().execute(() -> {
                for (BlockChange change : combinedChanges) {
                    change.stateToRestore = world.getBlockState(change.pos);
                    if (change.saveOriginal && !isVisualizationBlock(change.stateToRestore)) {
                        Road_Manager.originalBlocks.add(new Pair<>(change.pos, change.stateToRestore));
                    }

                    if (change.algo != null) {
                        activeVisitors.computeIfAbsent(change.pos, k -> new HashMap<>())
                                .merge(change.algo, 1, Integer::sum);
                    }

                    Set<String> visitors = activeVisitors.getOrDefault(change.pos, Collections.emptyMap()).keySet();
                    BlockState finalState = getIntersectionState(visitors, change.newState);

                    world.setBlockState(change.pos, finalState);
                }
            });

            currentStep++;
            stepsPlayed++;
        }

        if (currentStep >= maxSteps) {
            Road_Manager.isVisible = true;
            isAutoRunning = false; // Stop auto-play loop if finished
        }

        updateAllLabels(world, true);

        return stepsPlayed;
    }

    // --- STEP BACKWARD (PARALLEL) ---
    public static int playPreviousSteps(int count, ServerWorld world) {
        if (allSavedSteps.isEmpty()) return 0;
        int stepsReverted = 0;
        int maxSteps = getMaxSteps();

        for (int i = 0; i < count; i++) {
            if (currentStep <= 0) break;

            currentStep--;

            List<BlockChange> combinedChanges = new ArrayList<>();
            for (List<AlgoStep> steps : allSavedSteps.values()) {
                if (currentStep < steps.size()) {
                    combinedChanges.addAll(steps.get(currentStep).changes());
                }
            }

            world.getServer().execute(() -> {
                for (int j = combinedChanges.size() - 1; j >= 0; j--) {
                    BlockChange change = combinedChanges.get(j);

                    if (change.algo != null) {
                        Map<String, Integer> visitorsAtPos = activeVisitors.get(change.pos);
                        if (visitorsAtPos != null) {
                            int visitCount = visitorsAtPos.getOrDefault(change.algo, 0);
                            if (visitCount <= 1) {
                                visitorsAtPos.remove(change.algo);
                                if (visitorsAtPos.isEmpty()) {
                                    activeVisitors.remove(change.pos);
                                }
                            } else {
                                visitorsAtPos.put(change.algo, visitCount - 1);
                            }
                        }
                    }

                    if (change.stateToRestore != null) {
                        world.setBlockState(change.pos, change.stateToRestore);
                    }
                }
            });

            stepsReverted++;
        }

        if (currentStep < maxSteps) {
            Road_Manager.isVisible = false;
        }

        updateAllLabels(world, false);

        return stepsReverted;
    }

    public static boolean isFinished() {
        return !allSavedSteps.isEmpty() && currentStep >= getMaxSteps();
    }

    // --- FLOATING LABEL LOGIC ---
    private static void updateAllLabels(ServerWorld world, boolean forward) {
        world.getServer().execute(() -> {
            for (String algo : allSavedSteps.keySet()) {
                BlockPos lastPos = findLastPosForAlgo(algo, currentStep);
                if (lastPos != null) {
                    boolean isFinished = currentStep >= allSavedSteps.get(algo).size();
                    updateLabel(world, algo, lastPos, forward, isFinished);
                } else {
                    removeLabel(algo);
                }
            }
        });
    }

    private static BlockPos findLastPosForAlgo(String algo, int stepIndex) {
        List<AlgoStep> steps = allSavedSteps.get(algo);
        if (steps == null || steps.isEmpty()) return null;

        int startSearch = Math.min(stepIndex - 1, steps.size() - 1);

        for (int i = startSearch; i >= 0; i--) {
            for (BlockChange change : steps.get(i).changes()) {
                if (change.saveOriginal) return change.pos;
            }
        }

        if (!steps.get(0).changes().isEmpty()) {
            return steps.get(0).changes().get(0).pos;
        }

        return null;
    }

    private static void updateLabel(ServerWorld world, String algo, BlockPos pos, boolean forward, boolean isFinished) {
        removeLabel(algo);

        double targetX = pos.getX() + 0.5;
        double targetY = pos.getY() + 1.4;
        double targetZ = pos.getZ() + 0.5;

        DisplayEntity.TextDisplayEntity label = EntityType.TEXT_DISPLAY.create(world);
        if (label != null) {
            label.setBillboardMode(DisplayEntity.BillboardMode.CENTER);

            label.setPosition(targetX, targetY, targetZ);

            Formatting color = getAlgoColor(algo);

            label.setText(Text.literal("[" + algo.toUpperCase() + "]\n").formatted(color, Formatting.BOLD)
                    .append(getExplanationText(algo, pos, forward, isFinished)));

            world.spawnEntity(label);
            algoLabels.put(algo, label);
        }
    }

    private static void removeLabel(String algo) {
        DisplayEntity.TextDisplayEntity label = algoLabels.remove(algo);
        if (label != null) label.discard();
    }

    private static Text getExplanationText(String algo, BlockPos currentPos, boolean forward, boolean isFinished) {
        List<AlgoStep> steps = allSavedSteps.get(algo);
        int totalSteps = steps != null ? steps.size() : 0;
        int stepProgress = Math.min(currentStep, totalSteps);

        String status;
        if (isFinished) {
            status = "Finished!";
        } else if (isAutoPaused) {
            status = "PAUSED (Press 'C' to continue)";
        } else if (!forward) {
            status = "Backtracking... Revisiting states";
        } else {
            status = switch (algo) {
                case "astar" -> "Minimizing Cost + Heuristic";
                case "dijkstra" -> "Exploring Shortest Known Path";
                case "bellman_ford" -> "Relaxing Graph Edges";
                case "greedy" -> "Rushing to Target (Heuristic)";
                default -> "Exploring Node";
            };
        }

        double distToGoal = 0.0;
        double distFromStart = 0.0;

        if (steps != null && !steps.isEmpty()) {
            BlockPos goalPos = null;
            BlockPos startPos = null;

            if (!steps.get(0).changes().isEmpty()) {
                startPos = steps.get(0).changes().get(0).pos;
            }

            for (BlockChange c : steps.get(totalSteps - 1).changes()) {
                if (c.saveOriginal) { goalPos = c.pos; break; }
            }
            if (goalPos == null && !steps.get(totalSteps - 1).changes().isEmpty()) {
                goalPos = steps.get(totalSteps - 1).changes().get(0).pos;
            }

            if (currentPos != null) {
                if (goalPos != null) {
                    distToGoal = Math.sqrt(currentPos.getSquaredDistance(goalPos));
                }
                if (startPos != null) {
                    distFromStart = Math.sqrt(currentPos.getSquaredDistance(startPos));
                }
            }
        }

        String formattedDistToGoal = String.format("%.1f", distToGoal);
        String formattedDistFromStart = String.format("%.1f", distFromStart);

        return Text.literal("Nodes Visited: " + stepProgress + " / " + totalSteps + "\n")
                .append(Text.literal("Pos: [X:" + currentPos.getX() + ", Y:" + currentPos.getY() + ", Z:" + currentPos.getZ() + "]\n"))
                .append(Text.literal("Dist from Start: " + formattedDistFromStart + " blk\n").formatted(Formatting.GRAY))
                .append(Text.literal("Dist to Target: " + formattedDistToGoal + " blk\n").formatted(Formatting.GRAY))
                .append(Text.literal("Status: " + status).formatted(isFinished ? Formatting.GREEN : (isAutoPaused ? Formatting.YELLOW : Formatting.WHITE)));
    }

    private static Formatting getAlgoColor(String algo) {
        return switch (algo) {
            case "astar" -> Formatting.YELLOW;
            case "dijkstra" -> Formatting.WHITE;
            case "bellman_ford" -> Formatting.AQUA;
            case "greedy" -> Formatting.LIGHT_PURPLE;
            default -> Formatting.GRAY;
        };
    }

    private static BlockState getIntersectionState(Set<String> algos, BlockState originalRequest) {
        if (algos.size() <= 1) return originalRequest;

        boolean a = algos.contains("astar");
        boolean d = algos.contains("dijkstra");
        boolean b = algos.contains("bellman_ford");
        boolean g = algos.contains("greedy");

        if (a && d && b && g) return Blocks.CYAN_TERRACOTTA.getDefaultState();

        if (a && d && b) return Blocks.CYAN_CONCRETE.getDefaultState();
        if (a && d && g) return Blocks.TERRACOTTA.getDefaultState();
        if (a && b && g) return Blocks.PINK_TERRACOTTA.getDefaultState();
        if (d && b && g) return Blocks.GREEN_TERRACOTTA.getDefaultState();

        if (a && d) return Blocks.BROWN_TERRACOTTA.getDefaultState();
        if (a && b) return Blocks.PURPLE_TERRACOTTA.getDefaultState();
        if (a && g) return Blocks.YELLOW_TERRACOTTA.getDefaultState();
        if (d && b) return Blocks.BLUE_CONCRETE.getDefaultState();
        if (d && g) return Blocks.YELLOW_TERRACOTTA.getDefaultState();
        if (b && g) return Blocks.LIME_TERRACOTTA.getDefaultState();

        return originalRequest;
    }

    private static boolean isVisualizationBlock(BlockState state) {
        return state.isOf(Blocks.RED_CONCRETE) || state.isOf(Blocks.YELLOW_CONCRETE) ||
                state.isOf(Blocks.BLACK_CONCRETE) || state.isOf(Blocks.WHITE_CONCRETE) ||
                state.isOf(Blocks.BLUE_CONCRETE) || state.isOf(Blocks.LIGHT_BLUE_CONCRETE) ||
                state.isOf(Blocks.PURPLE_CONCRETE) || state.isOf(Blocks.PINK_CONCRETE) ||
                state.isOf(Blocks.ORANGE_CONCRETE) ||
                state.isOf(Blocks.BROWN_TERRACOTTA) || state.isOf(Blocks.PURPLE_TERRACOTTA) ||
                state.isOf(Blocks.YELLOW_TERRACOTTA) || state.isOf(Blocks.LIME_TERRACOTTA) ||
                state.isOf(Blocks.CYAN_CONCRETE) || state.isOf(Blocks.TERRACOTTA) ||
                state.isOf(Blocks.PINK_TERRACOTTA) || state.isOf(Blocks.GREEN_TERRACOTTA) ||
                state.isOf(Blocks.CYAN_TERRACOTTA);
    }
}
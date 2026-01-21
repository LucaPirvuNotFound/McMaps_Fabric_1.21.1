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

import java.util.ArrayList;
import java.util.HashMap;
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

    private static int executePathfindingAsync(ServerCommandSource source, BlockPos stop, String option, boolean realtime, List<String> algorithms) {
        if (source.getPlayer() == null) return 0;

        BlockPos start = source.getPlayer().getBlockPos();
        ServerWorld world = source.getWorld();
        final BlockPos final_stop = correct_height(world, stop);

        source.sendFeedback(() -> Text.literal("Calcul inceput pentru: " + algorithms.toString()), false);

        // --- sectiunea unde afisez o mica legenda despre culorile folosite pentru realtime
        if (realtime) {
            source.sendFeedback(() -> Text.literal("--- LEGENDA VIZUALIZARE ---").formatted(Formatting.GRAY), false);
            for (String algo : algorithms) {
                switch (algo) {
                    case "astar" -> source.sendFeedback(() ->
                            Text.literal("Astar: Baza-").append(Text.literal("Rosu").formatted(Formatting.RED))
                                    .append(Text.literal("; Curent-").append(Text.literal("Galben").formatted(Formatting.YELLOW))), false);

                    case "dijkstra" -> source.sendFeedback(() ->
                            Text.literal("Dijkstra: Baza-").append(Text.literal("Negru").formatted(Formatting.BLACK))
                                    .append(Text.literal("; Curent-").append(Text.literal("Alb").formatted(Formatting.WHITE))), false);

                    case "bellman_ford" -> source.sendFeedback(() ->
                            Text.literal("Bellman_Ford: Baza-").append(Text.literal("Albastru").formatted(Formatting.BLUE))
                                    .append(Text.literal("; Curent-").append(Text.literal("Bleu").formatted(Formatting.AQUA))), false);

                    case "greedy" -> source.sendFeedback(() ->
                            Text.literal("Greedy: Baza-").append(Text.literal("Mov").formatted(Formatting.DARK_PURPLE))
                                    .append(Text.literal("; Curent-").append(Text.literal("Roz").formatted(Formatting.LIGHT_PURPLE))), false);
                }
            }
        }
        // -----------------------------------------------

        Road_Manager.reset_waypoints();

        // Pornim calculul pe un alt thread
        CompletableFuture.runAsync(() -> {
            try {
                // Actualizam setarile in functie de optiune
                Road_Manager.updateCosts(option);

                // Executam scanarea
                Map<BlockPos, Double> baseMap = Road_Manager.scanSurface(world, start, (int) (Road_Manager.heuristic(start, final_stop) * 1.30));

                for (String algo : algorithms) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            // Facem o copie a hartii pentru ca algoritmii modifica valorile din map
                            // Daca nu facem copie, thread-urile se vor calca pe picioare
                            Map<BlockPos, Double> algoMap = new HashMap<>(baseMap);

                            Road_Manager.reset_block_counter();

                            long start_time = System.nanoTime();
                            double cost = 0;
                            boolean success = false;
                            String culoare = "necunoscut"; //nu ar trebui sa ramana asa ever
                            Formatting format = Formatting.WHITE;

                            // Selectam algoritmul
                            switch (algo) {
                                case "astar" -> {
                                    cost = Road_Manager.astar(start, final_stop, algoMap, world, realtime);
                                    success = !Road_Manager.WAYPOINTS_ASTAR.isEmpty();
                                    culoare = "verde";
                                    format = Formatting.GREEN;
                                }
                                case "dijkstra" -> {
                                    cost = Road_Manager.dijkstra(start, final_stop, algoMap, world, realtime);
                                    success = !Road_Manager.WAYPOINTS_DIJKSTRA.isEmpty();
                                    culoare = "alb";
                                }
                                case "bellman_ford" -> {
                                    cost = Road_Manager.bellman_ford(start, final_stop, algoMap, world, realtime);
                                    success = !Road_Manager.WAYPOINTS_BELLMAN_FORD.isEmpty();
                                    culoare = "mov";
                                    format = Formatting.LIGHT_PURPLE;
                                }
                                case "greedy" -> {
                                    cost = Road_Manager.greedy(start, final_stop, algoMap, world, realtime);
                                    success = !Road_Manager.WAYPOINTS_GREEDY.isEmpty();
                                    culoare = "negru";
                                    format = Formatting.BLACK;
                                }
                            }

                            double duration = (System.nanoTime() - start_time) / 1_000_000_000.0;
                            final double finalCost = cost;
                            final boolean finalSuccess = success;
                            final String finalCuloare = culoare;
                            final Formatting finalFormat = format;

                            // Afisarea rezultatelor pe thread-ul principal
                            source.getServer().execute(() -> {
                                source.sendFeedback(() -> Text.literal(" "), false); // Spatiu gol intre algoritmi
                                source.sendFeedback(() -> Text.literal("--- REZULTATE [" + algo.toUpperCase() + "] ---").formatted(Formatting.GOLD), false);

                                if (!finalSuccess) {
                                    source.sendError(Text.literal("Nu a fost gasit niciun drum cu " + algo));
                                } else {

                                    Road_Manager.isVisible = true;
                                    source.sendFeedback(() -> Text.literal("Drum calculat cu succes! [" + algo.toUpperCase() + "]").formatted(Formatting.GREEN), false);
                                    source.sendFeedback(() -> Text.literal("Costul drumului: " + String.format("%.2f", finalCost)), false);
                                    // Nota: nr_blocks este static volatil, daca ruleaza simultan numarul poate fi putin imprecis
                                    source.sendFeedback(() -> Text.literal("Noduri vizitate (aprox): " + Road_Manager.getNr_blocks(algo)), false);
                                    source.sendFeedback(() -> Text.literal("Timp executie: " + String.format("%.4f", duration) + " secunde"), false);
                                    source.sendFeedback(() -> Text.literal("CULOAREA DRUMULUI: ").append(finalCuloare).formatted(finalFormat), false);
                                }
                            });

                        } catch (Exception e) {
                            source.getServer().execute(() -> source.sendError(Text.literal("Eroare la " + algo + ": " + e.getMessage())));
                        }
                    });
                }
            } catch (Exception e) {
                source.getServer().execute(() -> source.sendError(Text.literal("Eroare la calcularea drumului: " + e.getMessage())));
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
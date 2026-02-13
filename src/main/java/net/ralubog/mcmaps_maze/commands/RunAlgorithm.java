package net.ralubog.mcmaps_maze.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class RunAlgorithm {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("run_algo")
                    // Argument 1: The Algorithm Name (e.g., "astar", "dijkstra")
                    .then(CommandManager.argument("algorithm", StringArgumentType.word())
                            // Argument 2: Show Steps? (true = Steps, false = Result)
                            .then(CommandManager.argument("show_steps", BoolArgumentType.bool())
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();

                                        String algoName = StringArgumentType.getString(context, "algorithm");
                                        boolean showSteps = BoolArgumentType.getBool(context, "show_steps");

                                        // 1. Calculate Coordinates (Logic from before)
                                        int playerX = (int) player.getX();
                                        int baseX = (playerX / 50) * 50;
                                        int destX = baseX + 11;
                                        int destY = 101;
                                        int destZ = 11;

                                        // 2. Construct the exact command
                                        // Template: /display_road X Y Z -default <bool> -<algo>
                                        String exactCommand = String.format("display_road %d %d %d -default %b -%s",
                                                destX, destY, destZ, showSteps, algoName);

                                        // 3. Feedback and Execution
                                        String mode = showSteps ? "Process (Steps)" : "Final Result";
                                        player.sendMessage(Text.literal("Running " + algoName + " [" + mode + "]...").formatted(Formatting.AQUA));

                                        player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource(), exactCommand);

                                        return 1;
                                    })
                            )
                    ));
        });
    }
}
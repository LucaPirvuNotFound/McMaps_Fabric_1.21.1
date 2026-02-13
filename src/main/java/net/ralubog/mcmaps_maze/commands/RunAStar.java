package net.ralubog.mcmaps_maze.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class RunAStar {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("run_astar")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();

                        // 1. Get Player Position
                        int playerX = (int) player.getX();

                        // 2. Calculate the Platform Base
                        // Example: If X is 453, (453 / 50) is 9.   9 * 50 is 450.
                        int baseX = (playerX / 50) * 50;

                        // 3. Calculate Destination (The Finish Line)
                        // The maps are 12x12, so the finish is at index 11 relative to the start.
                        int destX = baseX + 11;
                        int destY = 101;        // Stable height
                        int destZ = 11;         // Z is always 0-11 in your current generator

                        // 4. Construct the exact command
                        // Output example: "/display_road 461 101 11 -default false -astar"
                        String exactCommand = String.format("display_road %d %d %d -default false -astar",
                                destX, destY, destZ);

                        // 5. Run it!
                        player.sendMessage(Text.literal("Calculated Target: " + destX + " " + destY + " " + destZ).formatted(Formatting.GRAY));
                        player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource(), exactCommand);

                        return 1;
                    }));
        });
    }
}
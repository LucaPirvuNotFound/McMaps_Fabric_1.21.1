package net.ralubog.mcmaps_maze.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.ralubog.mcmaps_maze.commands.utils.Road_Manager;

public class Hide_Particle_Array {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("hide_road")
                    .executes(context -> {
                        Road_Manager.isVisible = false;
                        context.getSource().sendFeedback(() -> Text.literal("Road display DISABLED"), false);
                        return 1;
                    }));
        });
    }
}
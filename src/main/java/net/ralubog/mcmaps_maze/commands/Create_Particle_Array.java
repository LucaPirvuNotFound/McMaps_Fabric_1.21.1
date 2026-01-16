package net.ralubog.mcmaps_maze.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.ralubog.mcmaps_maze.commands.utils.Road_Manager;

import java.util.Map;

public class Create_Particle_Array {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("display_road")
                    .executes(context -> {
                        ServerWorld world = context.getSource().getWorld();
                        BlockPos beniging = new BlockPos(56, 76, -8);
                        BlockPos stop = new BlockPos(82, 79, -22);
                        Map<BlockPos, Double> g_score = Road_Manager.scanSurface(world, beniging,(int) (Road_Manager.heuristic(beniging, stop) * 1.20) );
                        Road_Manager.astar(beniging, stop, g_score, world);
                        Road_Manager.isVisible = true;
                        context.getSource().sendFeedback(() -> Text.literal("Road display ENABLED"), false);
                        return 1;
                    }));
        });
    }
}

package net.ralubog.mcmaps_maze.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class VisualizeGraph {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("visualize_graph")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        BlockPos center = player.getBlockPos();

                        // The 4 neighbors (North, South, East, West)
                        BlockPos[] neighbors = {
                                center.north(), center.south(), center.east(), center.west()
                        };

                        for (BlockPos neighbor : neighbors) {
                            // Check if the "Edge" exists (is the block walkable?)
                            // We check if the block is Air, or if it is something you can walk through
                            boolean isWalkable = player.getWorld().getBlockState(neighbor).isAir() ||
                                    !player.getWorld().getBlockState(neighbor).isSolidBlock(player.getWorld(), neighbor);

                            // Draw a line of particles from Center to Neighbor
                            // This math interpolates 5 points between the two blocks to draw a line
                            double startX = center.getX() + 0.5;
                            double startZ = center.getZ() + 0.5;
                            double endX = neighbor.getX() + 0.5;
                            double endZ = neighbor.getZ() + 0.5;
                            double y = center.getY() + 0.5; // Height of the particles

                            for (int i = 0; i <= 5; i++) {
                                double t = i / 5.0;
                                double currentX = startX + (endX - startX) * t;
                                double currentZ = startZ + (endZ - startZ) * t;

                                if (isWalkable) {
                                    // Green "Happy" particles for valid paths (Edges)
                                    player.getServerWorld().spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                                            currentX, y, currentZ, 1, 0, 0, 0, 0);
                                } else {
                                    // Red "Angry" particles for walls (No Edge)
                                    player.getServerWorld().spawnParticles(ParticleTypes.ANGRY_VILLAGER,
                                            currentX, y, currentZ, 1, 0, 0, 0, 0);
                                }
                            }
                        }

                        player.sendMessage(net.minecraft.text.Text.literal("Visualized Graph Edges!").formatted(net.minecraft.util.Formatting.GREEN), true);
                        return 1;
                    }));
        });
    }
}
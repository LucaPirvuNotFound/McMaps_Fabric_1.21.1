package net.ralubog.mcmaps_maze.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class Create_Particle {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("spawnbeam")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player != null) {
                            ServerWorld world = (ServerWorld) player.getWorld();

                            // The starting point (Eye level)
                            Vec3d startPos = player.getEyePos();
                            // The direction the player is looking (Heading)
                            Vec3d direction = player.getRotationVec(1.0F);

                            // Create a line of particles
                            for (double i = 0; i < 10; i += 0.5) { // i is the distance in blocks
                                Vec3d particlePos = startPos.add(direction.multiply(i));

                                world.spawnParticles(
                                        ParticleTypes.END_ROD, // Particle type
                                        particlePos.x,         // X
                                        particlePos.y,         // Y
                                        particlePos.z,         // Z
                                        1,                     // Count
                                        0.0, 0.0, 0.0,         // Spread (delta)
                                        0.0                    // Speed
                                );
                            }

                            context.getSource().sendFeedback(() -> Text.literal("Beam spawned!"), false);
                        }
                        return 1;
                    }));
        });
    }
}

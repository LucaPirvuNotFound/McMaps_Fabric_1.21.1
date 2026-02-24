package net.ralubog.mcmaps_maze;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.ralubog.mcmaps_maze.item.custom.RoadWandItem;

public class McMaps_MazeClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        // 1. CONTINUOUS RENDERING SYSTEM
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return;

            // Render Start Point (Gold/Flame)
            if (RoadWandItem.startPos != null) {
                spawnBeaconBeam(client.world, RoadWandItem.startPos, ParticleTypes.HAPPY_VILLAGER);
            }

            // Render End Point (Blue/Soul Flame)
            if (RoadWandItem.endPos != null) {
                spawnBeaconBeam(client.world, RoadWandItem.endPos, ParticleTypes.INSTANT_EFFECT);
            }
        });

        // 2. Middle Click to Clear (Stop Rendering)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.options.pickItemKey.isPressed()) {
                if (client.player.getMainHandStack().getItem() instanceof RoadWandItem) {

                    // Reset variables -> Particles stop spawning immediately
                    RoadWandItem.startPos = null;
                    RoadWandItem.endPos = null;

                    client.player.sendMessage(Text.literal("Selection Cleared").formatted(Formatting.RED), true);
                }
            }
        });

        // 3. Prevent Left Click Mining
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player.getStackInHand(hand).getItem() instanceof RoadWandItem) {
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }

    // Helper Method to create a vertical beam of particles for road_wand
    private void spawnBeaconBeam(net.minecraft.world.World world, BlockPos pos, SimpleParticleType particle) {
        // Spawn a particle every 0.5 blocks going up 6 blocks high
        for (double y = 0; y < 6.0; y += 0.5) {
            // We use addParticle (Client side) instead of spawnParticles
            world.addParticle(particle,
                    pos.getX() + 0.5,
                    pos.getY() + 1.2 + y, // Start slightly above the block
                    pos.getZ() + 0.5,
                    0, 0, 0 // No velocity, just hovering
            );
        }
    }
}


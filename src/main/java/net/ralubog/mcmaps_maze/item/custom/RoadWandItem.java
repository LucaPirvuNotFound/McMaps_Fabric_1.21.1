package net.ralubog.mcmaps_maze.item.custom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class RoadWandItem extends Item {

    // Static variables to hold the selection (simplified for one player)
    public static BlockPos startPos = null;
    public static BlockPos endPos = null;

    public RoadWandItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // Run on BOTH Client and Server
        // This ensures both sides know the coordinates without needing custom packets.

        BlockPos pos = raycastSelect(user);

        if (pos != null) {
            if (user.isSneaking()) {
                // Shift + Right Click = Start Point
                startPos = pos;
                // Only play sound on client to avoid double audio, or server if you prefer
                if (world.isClient) {
                    user.sendMessage(Text.literal("Start Point Set: " + pos.toShortString()).formatted(Formatting.GOLD), true);
//                    user.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
            } else {
                // Right Click = End Point
                endPos = pos;
                if (world.isClient) {
                    user.sendMessage(Text.literal("End Point Set: " + pos.toShortString()).formatted(Formatting.AQUA), true);
//                    user.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 1.0f, 1.5f);
                }
            }
        }

        return TypedActionResult.success(user.getStackInHand(hand));
    }

    // Simplified raycast - PlayerEntity has a built-in helper
    public static BlockPos raycastSelect(PlayerEntity player) {
        HitResult hit = player.raycast(200.0, 0.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) hit).getBlockPos();
        }
        return null;
    }

    public static void playSelectEffects(ServerWorld world, BlockPos pos, boolean isEnd) {
        // Play sound to all players in the area
        world.playSound(null, pos, isEnd ? SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value() : SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(),
                SoundCategory.PLAYERS, 1.0f, isEnd ? 1.5f : 1.0f);

        // Now spawnParticles will resolve correctly because we are using ServerWorld
        SimpleParticleType particle = isEnd ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
        for (int i = 0; i < 20; i++) {
            world.spawnParticles(particle,
                    pos.getX() + 0.5, pos.getY() + i, pos.getZ() + 0.5,
                    5, 0.1, 0.5, 0.1, 0.05);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.mcmaps_maze.road_wand.left").formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("tooltip.mcmaps_maze.road_wand.right").formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.mcmaps_maze.road_wand.middle").formatted(Formatting.YELLOW));
        super.appendTooltip(stack, context, tooltip, type);
    }


}

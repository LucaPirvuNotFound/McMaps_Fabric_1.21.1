package net.ralubog.mcmaps_maze.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

public class RoadWandItem extends Item {

    // Static variables to hold the selection (simplified for one player)
    public static BlockPos startPos = null;
    public static BlockPos endPos = null;

    public RoadWandItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            BlockPos pos = raycastSelect(world, user);
            if (pos != null) {
                endPos = pos;
                playSelectSound(world, user, pos);
                user.sendMessage(Text.literal("End Point Set: " + pos.toShortString()).formatted(Formatting.AQUA), true);
            }
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    // Custom Raycast to select blocks far away (up to 100 blocks)
    private BlockPos raycastSelect(World world, PlayerEntity player) {
        HitResult hit = player.raycast(100.0, 0.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) hit).getBlockPos();
        }
        return null;
    }

    private void playSelectSound(World world, PlayerEntity player, BlockPos pos) {
        world.playSound(player, pos, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.mcmaps_maze.road_wand.left").formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("tooltip.mcmaps_maze.road_wand.right").formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.mcmaps_maze.road_wand.middle").formatted(Formatting.YELLOW));
        super.appendTooltip(stack, context, tooltip, type);
    }


}

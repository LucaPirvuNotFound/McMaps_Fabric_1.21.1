package net.ralubog.mcmaps_maze.commands;

import net.minecraft.block.Blocks;
import net.minecraft.block.ChainBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GraphBuilder {

    // --- 1. BUILD A NODE (Vertex) ---
    public static void drawNode(ServerWorld world, BlockPos pos, String label, Direction wallFacing) {
        world.setBlockState(pos, Blocks.GOLD_BLOCK.getDefaultState());
        if (label != null && !label.isEmpty()) {
            drawLabel(world, pos.up(), label, 0xFFFFFF, wallFacing);
        }
    }

    // --- 2. CONNECT TWO NODES (Edge) ---
    public static void drawEdge(ServerWorld world, BlockPos start, BlockPos end, boolean directed, Direction wallRight, Direction wallFacing) {
        BlockPos diff = end.subtract(start);
        Direction dir = Direction.fromVector(diff.getX(), diff.getY(), diff.getZ());

        if (dir == null) return;

        int length = (int) Math.sqrt(diff.getSquaredDistance(0, 0, 0)) - 1;
        Direction.Axis axis = dir.getAxis();

        for (int i = 1; i <= length; i++) {
            BlockPos current = start.offset(dir, i);
            world.setBlockState(current, Blocks.CHAIN.getDefaultState().with(ChainBlock.AXIS, axis));
        }

        if (directed) {
            BlockPos midPoint = start.offset(dir, length / 2 + 1);
            spawnArrow(world, midPoint, dir, wallRight, wallFacing);
        }
    }

    // --- 3. SPAWN FLOATING LABEL (Fixed Rotation) ---
    public static void drawLabel(ServerWorld world, BlockPos pos, String text, int colorHex, Direction wallFacing) {
        DisplayEntity.TextDisplayEntity textEntity = new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);

        // Push slightly towards player so it's not inside blocks
        double offset = 0.6;
        double xOff = wallFacing.getOpposite().getOffsetX() * offset;
        double zOff = wallFacing.getOpposite().getOffsetZ() * offset;

        textEntity.setPosition(pos.getX() + 0.5 + xOff, pos.getY() + 0.5, pos.getZ() + 0.5 + zOff);
        textEntity.setText(Text.literal(text));

        // FIX: Rotate to face the player (Same logic as Arrows)
        textEntity.setBillboardMode(DisplayEntity.BillboardMode.FIXED);
        Quaternionf q = new Quaternionf();
        q.rotateY((float) Math.toRadians(180 - wallFacing.asRotation()));

        // Scale text up (1.5x)
        textEntity.setTransformation(new AffineTransformation(null, q, new Vector3f(1.5f, 1.5f, 1.5f), null));

        textEntity.setBackground(0x40000000); // Black transparent background
        textEntity.setGlowColorOverride(colorHex);

        world.spawnEntity(textEntity);
    }

    // --- 4. SPAWN 3D ARROW ---
    private static void spawnArrow(ServerWorld world, BlockPos pos, Direction arrowDir, Direction wallRight, Direction wallFacing) {
        DisplayEntity.ItemDisplayEntity arrow = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);

        double offset = 0.55;
        double xOff = wallFacing.getOpposite().getOffsetX() * offset;
        double zOff = wallFacing.getOpposite().getOffsetZ() * offset;

        arrow.setPosition(pos.getX() + 0.5 + xOff, pos.getY() + 0.5, pos.getZ() + 0.5 + zOff);
        arrow.setItemStack(new ItemStack(Items.ARROW));
        arrow.setBillboardMode(DisplayEntity.BillboardMode.FIXED);

        Quaternionf q = new Quaternionf();
        // 1. Face Player
        q.rotateY((float) Math.toRadians(180 - wallFacing.asRotation()));
        // 2. Fix Default Texture
        q.rotateZ((float) Math.toRadians(-45));

        // 3. Point Direction
        if (arrowDir == Direction.DOWN) q.rotateZ((float) Math.toRadians(180));
        else if (arrowDir == wallRight) q.rotateZ((float) Math.toRadians(-90));
        else if (arrowDir == wallRight.getOpposite()) q.rotateZ((float) Math.toRadians(90));

        arrow.setTransformation(new AffineTransformation(null, q, new Vector3f(1.5f, 1.5f, 1.5f), null));
        world.spawnEntity(arrow);
    }

    // --- 5. CLEAR AREA ---
    public static void clearArea(ServerWorld world, BlockPos center, Direction right) {
        Direction facing = right.rotateYCounterclockwise();
        for (int y = 0; y < 15; y++) {
            for (int i = -5; i < 15; i++) {
                for (int z = 0; z < 3; z++) {
                    BlockPos p = center.offset(right, i).up(y).offset(facing, z);
                    world.setBlockState(p, Blocks.AIR.getDefaultState());
                }
            }
        }
        world.getEntitiesByType(EntityType.TEXT_DISPLAY, new net.minecraft.util.math.Box(center).expand(20), e -> true).forEach(e -> e.discard());
        world.getEntitiesByType(EntityType.ITEM_DISPLAY, new net.minecraft.util.math.Box(center).expand(20), e -> true).forEach(e -> e.discard());
    }
}
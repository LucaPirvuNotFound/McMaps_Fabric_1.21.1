package net.ralubog.mcmaps_maze.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.ralubog.mcmaps_maze.MapManager; // Import the manager

public class GeneratePlatform {

    // Hardcoded Levels (1-3)
    private static final String[] LEVEL_1 = {
            "0S;0;0;0;0;0;0;0;0;0;0;0",
            "0;0;0;1;1;1;1;0;0;0;0;0",
            "0;0;1;2;2;2;1;0;0;0;0;0",
            "0;0;1;2;2;2;1;0;0;0;0;0",
            "0;0;0;1;1;1;1;0;0;0;0;0",
            "0;0;0;0;0;0;0;0;0;0;0;0",
            "0;0;0;0;0;0;0w;0w;0;0;0;0",
            "0;0;0;0;0;0;0w;0w;0;0;0;0",
            "0;0;0;0;0;0;0;0;0;0;0;0",
            "0;0;0;0;0;0;0;0;0;0;0;0",
            "0;0;0;0;0;0;0;0;0;0;0;0",
            "0;0;0;0;0;0;0;0;0;0;0;0F"
    };

    private static final String[] LEVEL_2 = {
            "2S;2;0;0;0;0;0;0;0;0;2;2", "2;2;0;0;0;0;0;0;0;0;2;2", "0;0;0;0;0w;0w;0w;0w;0;0;0;0",
            "0;0;0;0;0w;0w;0w;0w;0;0;0;0", "0;0;0;0;0;0;0;0;0;0;0;0", "0;0;1;1;0;0;0;0;1;1;0;0",
            "0;0;1;1;0;0;0;0;1;1;0;0", "0;0;0;0;0;0;0;0;0;0;0;0", "0;0;0;0;0w;0w;0w;0w;0;0;0;0",
            "0;0;0;0;0w;0w;0w;0w;0;0;0;0", "2;2;0;0;0;0;0;0;0;0;2;2", "2;2;0;0;0;0;0;0;0;0;2;2F"
    };

    private static final String[] LEVEL_3 = {
            "0S;0w;0w;0w;0w;0w;0w;0w;0w;0w;0w;0w", "0w;0;0;0;0;0w;0w;0;0;0;0;0w", "0w;0;0;0;0;0w;0w;0;0;0;0;0w",
            "0w;0w;0w;0w;0w;1;1;0w;0w;0w;0w;0w", "0w;0w;0w;0w;0w;1;1;0w;0w;0w;0w;0w", "0w;0;0;0;0;0w;0w;0;0;0;0;0w",
            "0w;0;0;0;0;0w;0w;0;0;0;0;0w", "0w;0w;0w;0w;0w;1;1;0w;0w;0w;0w;0w", "0w;0w;0w;0w;0w;1;1;0w;0w;0w;0w;0w",
            "0w;0;0;0;0;0w;0w;0;0;0;0;0w", "0w;0;0;0;0;0w;0w;0;0;0;0;0w", "0w;0w;0w;0w;0w;0w;0w;0w;0w;0w;0w;0F",
            "0S;0w;0w;0w;0w;0w;0w;0w;0w;0w;0w;0w", "0w;0;0;0;0;0w;0w;0;0;0;0;0w", "0w;0;0;0;0;0w;0w;0;0;0;0;0w",
            "0w;0w;0w;0w;0w;1;1;0w;0w;0w;0w;0w", "0w;0w;0w;0w;0w;1;1;0w;0w;0w;0w;0w", "0w;0;0;0;0;0w;0w;0;0;0;0;0w",
            "0w;0;0;0;0;0w;0w;0;0;0;0;0w", "0w;0w;0w;0w;0w;1;1;0w;0w;0w;0w;0w", "0w;0w;0w;0w;0w;1;1;0w;0w;0w;0w;0w",
            "0w;0;0;0;0;0w;0w;0;0;0;0;0w", "0w;0;0;0;0;0w;0w;0;0;0;0;0w", "0w;0w;0w;0w;0w;0w;0w;0w;0w;0w;0w;0F"

    };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("gen_platform")
                    .then(CommandManager.argument("level_id", IntegerArgumentType.integer())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                int levelId = IntegerArgumentType.getInteger(context, "level_id");

                                String[] selectedMap;

                                // 1. Load Map Logic
                                if (levelId == 1) selectedMap = LEVEL_1;
                                else if (levelId == 2) selectedMap = LEVEL_2;
                                else if (levelId == 3) selectedMap = LEVEL_3;
                                else selectedMap = MapManager.getLevel(levelId);

                                if (selectedMap == null) {
                                    player.sendMessage(Text.literal("Level " + levelId + " not found!").formatted(Formatting.RED));
                                    return 0;
                                }

                                // 2. Find Spot
                                int startX = 0;
                                int yLevel = 100;
                                int startZ = 0;
                                while (!player.getWorld().getBlockState(new BlockPos(startX, yLevel, startZ)).isAir()) {
                                    startX += 50;
                                }

                                // 3. Build It (Standard Logic)
                                for (int z = 0; z < selectedMap.length; z++) {
                                    String[] cells = selectedMap[z].split(";");
                                    for (int x = 0; x < cells.length; x++) {
                                        String cell = cells[x].trim();
                                        boolean isWater = cell.contains("w");
                                        boolean isStart = cell.contains("S");
                                        boolean isFinish = cell.contains("F");
                                        String numberPart = cell.replace("w", "").replace("S", "").replace("F", "");

                                        int height = 0;
                                        try { height = Integer.parseInt(numberPart); } catch (NumberFormatException e) {}

                                        BlockPos basePos = new BlockPos(startX + x, yLevel, startZ + z);
                                        player.getWorld().setBlockState(basePos.down(), Blocks.DIRT.getDefaultState());

                                        for (int h = 0; h <= height; h++) {
                                            BlockPos targetPos = basePos.up(h);
                                            if (isWater) player.getWorld().setBlockState(targetPos, Blocks.WATER.getDefaultState());
                                            else {
                                                if (h == height) {
                                                    if (isStart) player.getWorld().setBlockState(targetPos, Blocks.GOLD_BLOCK.getDefaultState());
                                                    else if (isFinish) player.getWorld().setBlockState(targetPos, Blocks.DIAMOND_BLOCK.getDefaultState());
                                                    else player.getWorld().setBlockState(targetPos, Blocks.GRASS_BLOCK.getDefaultState());
                                                } else {
                                                    player.getWorld().setBlockState(targetPos, Blocks.DIRT.getDefaultState());
                                                }
                                            }
                                        }
                                    }
                                }

                                // --- KEY UPDATE: SAVE THE LINK ---
                                MapManager.linkPlatform(startX, levelId);
                                // ---------------------------------

                                player.teleport(player.getServerWorld(), startX + 6.5, yLevel + 1, startZ + 6.5, 0, 0);
                                player.sendMessage(Text.literal("Generated Level " + levelId).formatted(Formatting.GREEN));
                                return 1;
                            })
                    ));
        });
    }
}
package net.ralubog.mcmaps_maze.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.ralubog.mcmaps_maze.MapManager;
import net.ralubog.mcmaps_maze.commands.GenerateMenu; // Ensure this import exists

public class MapDesigner {

    private static final int MAP_SIZE = 12;
    private static final int GRID_SPACING = 50;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // --- COMMAND 1: GEN_CANVAS ---
            // Generates a blank platform at the next available spot
            dispatcher.register(CommandManager.literal("gen_canvas")
                    .then(CommandManager.argument("size", IntegerArgumentType.integer())
                            .executes(context -> {
                                int size = IntegerArgumentType.getInteger(context, "size");
                                return generateCanvas(context.getSource().getPlayer(), size);
                            })
                    ));

            // --- COMMAND 2: SAVE_MAP ---
            dispatcher.register(CommandManager.literal("save_map")
                    // A. Auto-mode: Checks if this spot is already linked to an ID
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();

                        // 1. Calculate X based on grid
                        int currentX = player.getBlockX();
                        int startX = (currentX / GRID_SPACING) * GRID_SPACING;

                        // 2. Check for existing link
                        int linkedId = MapManager.getLinkedLevelId(startX);

                        // 3. Decide ID
                        int idToSave;
                        if (linkedId != -1) {
                            idToSave = linkedId; // Overwrite existing!
                            player.sendMessage(Text.literal("Existing Level " + idToSave + " detected. Updating...").formatted(Formatting.AQUA));
                        } else {
                            idToSave = MapManager.getNextAvailableId(); // New ID
                            player.sendMessage(Text.literal("New build detected. Saving as Level " + idToSave).formatted(Formatting.YELLOW));
                        }

                        return performSave(player, idToSave, startX);
                    })
                    // B. Manual mode: Force a specific ID
                    .then(CommandManager.argument("level_id", IntegerArgumentType.integer())
                            .executes(context -> {
                                int levelId = IntegerArgumentType.getInteger(context, "level_id");
                                int currentX = context.getSource().getPlayer().getBlockX();
                                int startX = (currentX / GRID_SPACING) * GRID_SPACING;
                                return performSave(context.getSource().getPlayer(), levelId, startX);
                            })
                    ));
        });
    }

    private static int generateCanvas(ServerPlayerEntity player, int size) {
        int startX = 0;
        int yLevel = 100;
        int startZ = 0;

        // Find the next empty spot in the grid
        while (!player.getWorld().getBlockState(new BlockPos(startX, yLevel, startZ)).isAir()) {
            startX += GRID_SPACING;
        }

        // --- UNLINK: This is a new canvas, remove any old ID association ---
        MapManager.unlinkPlatform(startX);
        // -------------------------------------------------------------------

        BlockPos origin = new BlockPos(startX, yLevel, startZ);

        // Build the flat grass platform
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                BlockPos pos = origin.add(x, 0, z);
                player.getWorld().setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState());
                // Clear air above
                for(int y = 1; y < 5; y++) {
                    player.getWorld().setBlockState(pos.up(y), Blocks.AIR.getDefaultState());
                }
            }
        }

        // NOTE: Teleport removed as requested
        player.sendMessage(Text.literal("Canvas created at X=" + startX).formatted(Formatting.GREEN));
        return 1;
    }

    private static int performSave(ServerPlayerEntity player, int levelId, int startX) {
        BlockPos origin = new BlockPos(startX, 100, 0);

        String[] mapData = new String[MAP_SIZE];

        // Scan the 12x12 area
        for (int z = 0; z < MAP_SIZE; z++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < MAP_SIZE; x++) {
                BlockPos base = origin.add(x, 0, z);
                int height = 0;
                BlockState topState = player.getWorld().getBlockState(base);

                // Check height (up to 10 blocks)
                for (int h = 1; h <= 10; h++) {
                    BlockState s = player.getWorld().getBlockState(base.up(h));
                    if (!s.isAir()) {
                        height = h;
                        topState = s;
                    } else break;
                }

                // Determine Block Type Suffix
                String suffix = "";
                if (topState.isOf(Blocks.GOLD_BLOCK)) suffix = "S";
                else if (topState.isOf(Blocks.DIAMOND_BLOCK)) suffix = "F";
                else if (topState.isOf(Blocks.WATER)) suffix = "w";

                row.append(height).append(suffix);
                if (x < MAP_SIZE - 1) row.append(";");
            }
            mapData[z] = row.toString();
        }

        // 1. Save Data to File
        MapManager.saveLevel(levelId, mapData);

        // 2. Link this X-coordinate to this Level ID
        MapManager.linkPlatform(startX, levelId);

        // 3. Update the Book in Player's Inventory
        GenerateMenu.updateBookInInventory(player);

        player.sendMessage(Text.literal("Saved as Level " + levelId + "! Check your book.").formatted(Formatting.GREEN));
        return 1;
    }
}
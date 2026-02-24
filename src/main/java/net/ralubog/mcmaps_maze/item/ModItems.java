package net.ralubog.mcmaps_maze.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.ralubog.mcmaps_maze.McMaps_Maze;

public class ModItems {
    // ROAD_WAND (main tool)
    public static final Item ROAD_WAND = registerItem("road_wand", new Item(new Item.Settings()));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(McMaps_Maze.MOD_ID, name), item);
    }

    public static void registerModItems() {
        McMaps_Maze.LOGGER.info("Registering Mod Items for " + McMaps_Maze.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(ROAD_WAND);
        });
    }
}

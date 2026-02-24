package net.ralubog.mcmaps_maze.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText; // Import this!
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.ralubog.mcmaps_maze.MapManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class GenerateMenu {

    private static final String BOOK_TITLE = "Map Tools";

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("givebook")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        ItemStack book = createBook();
                        player.giveItemStack(book);
                        return 1;
                    }));
        });
    }

    public static ItemStack createBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<RawFilteredPair<Text>> pages = new ArrayList<>();

        // --- PAGE 1: CREATOR TOOLS ---
        // Change 'Text' to 'MutableText' here
        MutableText page1 = Text.empty()
                .append(Text.literal("Creator Tools\n\n").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.DARK_BLUE)))

                // Button: Create Canvas
                .append(Text.literal("[CREATE 12x12 CANVAS]\n")
                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN).withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gen_canvas 12"))))
                .append(Text.literal("Click to build an empty platform.\n\n"))

                // Button: Save Map (Auto ID)
                .append(Text.literal("[SAVE NEW MAP]\n")
                        .setStyle(Style.EMPTY.withColor(Formatting.BLUE).withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/save_map"))))
                .append(Text.literal("Saves the platform you are standing on as the next Level ID."));

        pages.add(RawFilteredPair.of(page1));

        // --- PAGE 2: BUILT-IN LEVELS ---
        // Change 'Text' to 'MutableText' here
        MutableText page2 = Text.empty()
                .append(Text.literal("Standard Levels\n\n").setStyle(Style.EMPTY.withBold(true)))
                .append(createButton(1, "Novice", Formatting.DARK_GREEN))
                .append(createButton(2, "Expert", Formatting.DARK_PURPLE))
                .append(createButton(3, "Master", Formatting.RED));
        pages.add(RawFilteredPair.of(page2));

        // --- PAGES 3+: SAVED LEVELS ---
        Set<Integer> savedIds = MapManager.getSavedLevelIds();
        List<Integer> sortedIds = new ArrayList<>(savedIds);
        Collections.sort(sortedIds);

        if (!sortedIds.isEmpty()) {
            // Change 'Text' to 'MutableText' here
            MutableText savedPage = Text.empty().append(Text.literal("Saved Levels\n\n").setStyle(Style.EMPTY.withBold(true)));
            int count = 0;

            for (int id : sortedIds) {
                if (id <= 3) continue; // Skip hardcoded ones

                savedPage.append(createButton(id, "Custom Save", Formatting.GOLD));
                count++;

                // Create new page every 4 buttons to avoid overflow
                if (count % 4 == 0) {
                    pages.add(RawFilteredPair.of(savedPage));
                    savedPage = Text.empty().append(Text.literal("Saved Levels (Cont.)\n\n").setStyle(Style.EMPTY.withBold(true)));
                }
            }
            // Check if the last page has content before adding
            if (count % 4 != 0) {
                pages.add(RawFilteredPair.of(savedPage));
            }
        }

        WrittenBookContentComponent content = new WrittenBookContentComponent(
                RawFilteredPair.of(BOOK_TITLE), "Instructor", 0,
                pages, true
        );
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    private static MutableText createButton(int id, String label, Formatting color) {
        return Text.empty()
                .append(Text.literal("Level " + id + ": " + label + "\n"))
                .append(Text.literal("[GENERATE]\n\n")
                        .setStyle(Style.EMPTY.withColor(color).withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gen_platform " + id))));
    }

    public static void updateBookInInventory(ServerPlayerEntity player) {
        ItemStack freshBook = createBook();
        WrittenBookContentComponent newContent = freshBook.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.WRITTEN_BOOK) {
                WrittenBookContentComponent currentContent = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
                if (currentContent != null && currentContent.title().raw().equals(BOOK_TITLE)) {
                    stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, newContent);
                }
            }
        }
        player.sendMessage(Text.literal("Book updated.").formatted(Formatting.AQUA), true);
    }
}
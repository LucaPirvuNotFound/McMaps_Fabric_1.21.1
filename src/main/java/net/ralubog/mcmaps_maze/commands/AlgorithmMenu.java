package net.ralubog.mcmaps_maze.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;

public class AlgorithmMenu {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("givealgobook")
                    .executes(context -> {
                        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

                        // --- HELPER: CREATE BUTTONS ---
                        // Creates: [RESULT] [STEPS] and then [RESET] below it
                        java.util.function.Function<String, Text> createButtons = (algoCode) -> Text.empty()
                                // 1. Result and Steps Buttons
                                .append(Text.literal("\n\n[RESULT]  ")
                                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN).withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/run_algo " + algoCode + " false"))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Jump to Solution")))))
                                .append(Text.literal("[STEPS]")
                                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/run_algo " + algoCode + " true"))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Visualize the Process")))))

                                // 2. The New RESET Button
                                .append(Text.literal("\n\n       [RESET]")
                                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hide_road"))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Clear the particles")))));

                        // --- PAGE 1: A* Star ---
                        Text page1 = Text.empty()
                                .append(Text.literal("A* Algorithm\n\n").setStyle(Style.EMPTY.withBold(true).withUnderline(true)))
                                .append(Text.literal("Uses heuristics (distance to goal) to find the shortest path efficiently."))
                                .append(createButtons.apply("astar"));

                        // --- PAGE 2: Dijkstra ---
                        Text page2 = Text.empty()
                                .append(Text.literal("Dijkstra\n\n").setStyle(Style.EMPTY.withBold(true).withUnderline(true)))
                                .append(Text.literal("Explores all directions equally. Guarantees shortest path but is slower."))
                                .append(createButtons.apply("dijkstra"));

                        // --- PAGE 3: Bellman-Ford ---
                        Text page3 = Text.empty()
                                .append(Text.literal("Bellman-Ford\n\n").setStyle(Style.EMPTY.withBold(true).withUnderline(true)))
                                .append(Text.literal("Can handle negative weights. Checks all edges multiple times."))
                                .append(createButtons.apply("bellman_ford"));

                        // --- PAGE 4: Greedy ---
                        Text page4 = Text.empty()
                                .append(Text.literal("Greedy Search\n\n").setStyle(Style.EMPTY.withBold(true).withUnderline(true)))
                                .append(Text.literal("Moves towards the goal immediately. Very fast, but not always optimal."))
                                .append(createButtons.apply("greedy"));

                        // --- COMPILE BOOK ---
                        WrittenBookContentComponent content = new WrittenBookContentComponent(
                                RawFilteredPair.of("Algorithm Control"), "Instructor", 0,
                                List.of(RawFilteredPair.of(page1), RawFilteredPair.of(page2), RawFilteredPair.of(page3), RawFilteredPair.of(page4)),
                                true
                        );
                        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, content);
                        context.getSource().getPlayer().giveItemStack(book);
                        return 1;
                    }));
        });
    }
}
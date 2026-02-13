package net.ralubog.mcmaps_maze.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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

public class ExerciseLesson1 {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 1. Give Book
            dispatcher.register(CommandManager.literal("give_ex_1_book")
                    .executes(context -> {
                        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

                        Text p1 = Text.empty().append(Text.literal("Ex 1: Definitions\n\n").setStyle(Style.EMPTY.withBold(true)))
                                .append("What does 'E' stand for in G=(V,E)?\n\n")
                                .append(createChoice("A", "Entities", 1))
                                .append(createChoice("B", "Edges", 1));

                        Text p2 = Text.empty().append(Text.literal("Ex 2: Connectivity\n\n").setStyle(Style.EMPTY.withBold(true)))
                                .append("Connect the two pillars.\n\n")
                                .append(createStartButton(2))
                                .append(createCheckButton(2));

                        Text p3 = Text.empty().append(Text.literal("Ex 3: Degree\n\n").setStyle(Style.EMPTY.withBold(true)))
                                .append("In-Degree = 3, Out-Degree = 1.\nWhat is the Total Degree?\n\n")
                                .append(createChoice("A", "2", 3))
                                .append(createChoice("B", "3", 3))
                                .append(createChoice("C", "4", 3));

                        Text p4 = Text.empty().append(Text.literal("Ex 4: Trees\n\n").setStyle(Style.EMPTY.withBold(true)))
                                .append("Break 1 chain to stop the Cycle.\n\n")
                                .append(createStartButton(4))
                                .append(createCheckButton(4));

                        Text p5 = Text.empty().append(Text.literal("Ex 5: Directions\n\n").setStyle(Style.EMPTY.withBold(true)))
                                .append("Directed Edges are also called:\n\n")
                                .append(createChoice("A", "Arrows/Arcs", 5))
                                .append(createChoice("B", "Lines", 5));

                        Text p6 = Text.empty().append(Text.literal("Ex 6: Coloring\n\n").setStyle(Style.EMPTY.withBold(true)))
                                .append("Color nodes Red/Blue so neighbors have diff colors.\n\n")
                                .append(createStartButton(6))
                                .append(createCheckButton(6));

                        Text p7 = Text.empty().append(Text.literal("Ex 7: Optimization\n\n").setStyle(Style.EMPTY.withBold(true)))
                                .append("Remove chains to make a Tree (No cycles, still connected).\n\n")
                                .append(createStartButton(7))
                                .append(createCheckButton(7));

                        Text p8 = Text.empty().append(Text.literal("Ex 8: Isolation\n\n").setStyle(Style.EMPTY.withBold(true)))
                                .append("Connect the lonely node.\n\n")
                                .append(createStartButton(8))
                                .append(createCheckButton(8));

                        Text p9 = Text.empty().append(Text.literal("Ex 9: Isolation\n\n").setStyle(Style.EMPTY.withBold(true)))
                                .append("Degree of isolated node?\n\n")
                                .append(createChoice("A", "1", 9))
                                .append(createChoice("B", "0", 9));

                        Text p10 = Text.empty().append(Text.literal("Ex 10: Cycles\n\n").setStyle(Style.EMPTY.withBold(true)))
                                .append("Add chains to close the loop.\n\n")
                                .append(createStartButton(10))
                                .append(createCheckButton(10));

                        WrittenBookContentComponent content = new WrittenBookContentComponent(
                                RawFilteredPair.of("Exercises: Lesson 1"), "Instructor", 0,
                                List.of(RawFilteredPair.of(p1), RawFilteredPair.of(p2), RawFilteredPair.of(p3), RawFilteredPair.of(p4), RawFilteredPair.of(p5),
                                        RawFilteredPair.of(p6), RawFilteredPair.of(p7), RawFilteredPair.of(p8), RawFilteredPair.of(p9), RawFilteredPair.of(p10)),
                                true
                        );
                        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, content);
                        context.getSource().getPlayer().giveItemStack(book);
                        return 1;
                    }));

            // 2. Start
            dispatcher.register(CommandManager.literal("ex_start")
                    .then(CommandManager.argument("id", IntegerArgumentType.integer())
                            .executes(context -> {
                                ExerciseLogic.startExercise(context.getSource().getPlayer(), IntegerArgumentType.getInteger(context, "id"));
                                return 1;
                            })));

            // 3. Answer
            dispatcher.register(CommandManager.literal("ex_answer")
                    .then(CommandManager.argument("id", IntegerArgumentType.integer())
                            .then(CommandManager.argument("choice", StringArgumentType.word())
                                    .executes(context -> {
                                        ExerciseLogic.checkAnswer(context.getSource().getPlayer(), IntegerArgumentType.getInteger(context, "id"), StringArgumentType.getString(context, "choice"));
                                        return 1;
                                    }))));

            // 4. Check
            dispatcher.register(CommandManager.literal("ex_check")
                    .then(CommandManager.argument("id", IntegerArgumentType.integer())
                            .executes(context -> {
                                ExerciseLogic.checkAnswer(context.getSource().getPlayer(), IntegerArgumentType.getInteger(context, "id"), "");
                                return 1;
                            })));
        });
    }

    private static Text createChoice(String letter, String text, int id) {
        return Text.literal("[" + letter + "] " + text + "\n")
                .setStyle(Style.EMPTY.withColor(Formatting.BLUE)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ex_answer " + id + " " + letter)));
    }

    private static Text createStartButton(int id) {
        return Text.literal("[START PUZZLE]\n")
                .setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ex_start " + id)));
    }

    private static Text createCheckButton(int id) {
        return Text.literal("[CHECK ANSWER]\n")
                .setStyle(Style.EMPTY.withColor(Formatting.GREEN).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ex_check " + id)));
    }
}
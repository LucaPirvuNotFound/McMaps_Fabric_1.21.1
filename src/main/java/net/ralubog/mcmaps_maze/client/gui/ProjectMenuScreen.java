package net.ralubog.mcmaps_maze.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.ralubog.mcmaps_maze.MapManager;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class ProjectMenuScreen extends Screen {
    private enum Tab { MAPS, ALGORITHMS, MULTI_ALGO, CODE }
    private Tab currentTab = Tab.MAPS;

    // --- MAPS STATE ---
    private final int[] canvasSizes = {12, 20, 30};
    private int selectedSizeIndex = 0;
    private int mapPage = 0;
    private boolean isCreatingMap = false;

    // --- ALGORITHM STATE ---
    private static class AlgoInfo {
        String id, name, desc;
        AlgoInfo(String id, String name, String desc) {
            this.id = id; this.name = name; this.desc = desc;
        }
    }

    private final List<AlgoInfo> algorithms = new ArrayList<>();
    private AlgoInfo selectedAlgo;

    private final Map<String, Boolean> multiAlgoToggles = new HashMap<>();

    // --- MANUAL DEBUGGER STATE ---
    public static final int[] stepSizes = {1, 2, 5, 10, 50, 100};
    public static int selectedStepIndex = 0;

    // --- AUTO PLAY STATE ---
    public static final double[] autoDelays = {0, 0.5, 1.0, 2.0, 5.0, 10.0};
    public static int selectedDelayIndex = 0;

    public ProjectMenuScreen() {
        super(Text.literal("Project Menu"));

        algorithms.add(new AlgoInfo("astar", "A* Algorithm", "Uses heuristics to find the shortest path efficiently."));
        algorithms.add(new AlgoInfo("dijkstra", "Dijkstra", "Explores all directions equally. Guarantees shortest path."));
        algorithms.add(new AlgoInfo("bellman_ford", "Bellman-Ford", "Can handle negative weights. Checks edges multiple times."));
        algorithms.add(new AlgoInfo("greedy", "Greedy Search", "Moves towards the goal immediately. Very fast."));

        selectedAlgo = algorithms.get(0);

        for (AlgoInfo algo : algorithms) {
            multiAlgoToggles.put(algo.id, algo.id.equals("astar"));
        }
    }

    @Override
    protected void init() {
        this.clearChildren();
        int startX = this.width / 2 - 160;
        int startY = 20;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("MAPS").formatted(currentTab == Tab.MAPS ? Formatting.YELLOW : Formatting.WHITE), btn -> switchTab(Tab.MAPS))
                .dimensions(startX, startY, 70, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("ALGORITHMS").formatted(currentTab == Tab.ALGORITHMS ? Formatting.YELLOW : Formatting.WHITE), btn -> switchTab(Tab.ALGORITHMS))
                .dimensions(startX + 75, startY, 90, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("MULTI-ALGO").formatted(currentTab == Tab.MULTI_ALGO ? Formatting.YELLOW : Formatting.WHITE), btn -> switchTab(Tab.MULTI_ALGO))
                .dimensions(startX + 170, startY, 90, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("CODE").formatted(currentTab == Tab.CODE ? Formatting.YELLOW : Formatting.WHITE), btn -> switchTab(Tab.CODE))
                .dimensions(startX + 265, startY, 55, 20).build());

        startY += 35;

        if (currentTab == Tab.MAPS) {
            initMapsTab(startX + 10, startY);
        } else if (currentTab == Tab.ALGORITHMS) {
            initAlgorithmsTab(startX + 10, startY);
        } else if (currentTab == Tab.MULTI_ALGO) {
            initMultiAlgoTab(startX + 10, startY);
        } else if (currentTab == Tab.CODE) {
            initCodeTab(startX + 10, startY);
        }
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        this.isCreatingMap = false;
        this.init();
    }

    private void initMapsTab(int startX, int startY) {
        if (isCreatingMap) {
            ButtonWidget sizeBtn = ButtonWidget.builder(Text.literal("Size: " + canvasSizes[selectedSizeIndex] + "x" + canvasSizes[selectedSizeIndex]), btn -> {
                selectedSizeIndex = (selectedSizeIndex + 1) % canvasSizes.length;
                this.init();
            }).dimensions(startX, startY + 20, 150, 20).build();
            this.addDrawableChild(sizeBtn);

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Generate Canvas").formatted(Formatting.GREEN), btn -> {
                sendCommand("gen_canvas " + canvasSizes[selectedSizeIndex]);
                this.close();
            }).dimensions(startX, startY + 50, 100, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Back").formatted(Formatting.RED), btn -> {
                isCreatingMap = false;
                this.init();
            }).dimensions(startX + 110, startY + 50, 60, 20).build());

        } else {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Create New Map").formatted(Formatting.GREEN), btn -> {
                isCreatingMap = true;
                this.init();
            }).dimensions(startX, startY, 145, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Save Actual Map").formatted(Formatting.AQUA), btn -> {
                sendCommand("save_map");
                this.close();
            }).dimensions(startX + 155, startY, 145, 20).build());

            int mapStartY = startY + 35;
            List<Integer> allMaps = new ArrayList<>(List.of(1, 2, 3));

            Set<Integer> savedIds = MapManager.getSavedLevelIds();
            List<Integer> sortedSaves = new ArrayList<>(savedIds);
            Collections.sort(sortedSaves);
            for(int id : sortedSaves) {
                if(id > 3) allMaps.add(id);
            }

            int maxPerPage = 5;
            int totalPages = (int) Math.ceil((double) allMaps.size() / maxPerPage);
            if (mapPage >= totalPages) mapPage = Math.max(0, totalPages - 1);

            for (int i = mapPage * maxPerPage; i < Math.min((mapPage + 1) * maxPerPage, allMaps.size()); i++) {
                int id = allMaps.get(i);
                String label = id == 1 ? "Novice" : id == 2 ? "Expert" : id == 3 ? "Master" : "Custom Save";
                Formatting color = id == 1 ? Formatting.DARK_GREEN : id == 2 ? Formatting.DARK_PURPLE : id == 3 ? Formatting.RED : Formatting.GOLD;

                this.addDrawableChild(ButtonWidget.builder(Text.literal("Level " + id + ": " + label).formatted(color), btn -> {
                    sendCommand("gen_platform " + id);
                    this.close();
                }).dimensions(startX, mapStartY, 300, 20).build());
                mapStartY += 24;
            }

            if (mapPage > 0) {
                this.addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"), btn -> {
                    mapPage--; this.init();
                }).dimensions(startX, mapStartY + 5, 60, 20).build());
            }
            if (mapPage < totalPages - 1) {
                this.addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), btn -> {
                    mapPage++; this.init();
                }).dimensions(startX + 240, mapStartY + 5, 60, 20).build());
            }
        }
    }

    private void initAlgorithmsTab(int startX, int startY) {
        int listY = startY;

        for (AlgoInfo algo : algorithms) {
            boolean isSelected = (selectedAlgo == algo);
            this.addDrawableChild(ButtonWidget.builder(Text.literal(algo.name).formatted(isSelected ? Formatting.YELLOW : Formatting.WHITE), btn -> {
                selectedAlgo = algo;
                this.init();
            }).dimensions(startX, listY, 100, 20).build());
            listY += 24;
        }

        int rightX = startX + 110;
        int btnY = startY + 40;

        // RESULT AND RESET
        // RESULT AND RESET
        this.addDrawableChild(ButtonWidget.builder(Text.literal("RESULT").formatted(Formatting.GREEN), btn -> {
            // Using your friend's find_path command with default option and realtime=false
            sendCommand("find_path default false " + selectedAlgo.id);
            this.close();
        }).dimensions(rightX, btnY, 55, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("RESET").formatted(Formatting.RED), btn -> {
            sendCommand("algo_reset");
            this.close();
        }).dimensions(rightX + 60, btnY, 50, 20).build());

        // PREPARE
        int manualY = btnY + 25;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("PREPARE MANUAL STEPS").formatted(Formatting.AQUA), btn -> {
            sendCommand("algo_debug " + selectedAlgo.id);
        }).dimensions(rightX, manualY, 165, 20).build());

        // MANUAL STEP SIZE
        int stepY = manualY + 25;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Step Size: " + stepSizes[selectedStepIndex]), btn -> {
            selectedStepIndex = (selectedStepIndex + 1) % stepSizes.length;
            this.init();
        }).dimensions(rightX, stepY, 80, 20).build());

        // AUTO PLAY ROW
        int autoY = stepY + 25;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("START AUTO").formatted(Formatting.GOLD), btn -> {
            sendCommand("algo_auto " + autoDelays[selectedDelayIndex]);
            this.close();
        }).dimensions(rightX, autoY, 80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Delay: " + autoDelays[selectedDelayIndex] + "s"), btn -> {
            selectedDelayIndex = (selectedDelayIndex + 1) % autoDelays.length;
            this.init();
        }).dimensions(rightX + 85, autoY, 80, 20).build());
    }

    private void initMultiAlgoTab(int startX, int startY) {
        int listY = startY + 15;

        for (AlgoInfo algo : algorithms) {
            boolean isOn = multiAlgoToggles.getOrDefault(algo.id, false);

            this.addDrawableChild(ButtonWidget.builder(Text.literal(isOn ? "[ON]" : "[OFF]").formatted(isOn ? Formatting.GREEN : Formatting.GRAY), btn -> {
                multiAlgoToggles.put(algo.id, !isOn);
                this.init();
            }).dimensions(startX, listY, 40, 20).build());

            listY += 25;
        }

        int rightX = startX + 130;
        int btnY = startY + 40;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("PREPARE MULTI-ALGO RACE").formatted(Formatting.AQUA), btn -> {
            StringBuilder algoString = new StringBuilder();
            for (Map.Entry<String, Boolean> entry : multiAlgoToggles.entrySet()) {
                if (entry.getValue()) {
                    algoString.append("-").append(entry.getKey());
                }
            }
            if (algoString.length() == 0) algoString.append("astar");

            sendCommand("algo_debug " + algoString.toString());
        }).dimensions(rightX, btnY, 165, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("RESET").formatted(Formatting.RED), btn -> {
            sendCommand("algo_reset");
            this.close();
        }).dimensions(rightX, btnY + 25, 60, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Step Size: " + stepSizes[selectedStepIndex]), btn -> {
            selectedStepIndex = (selectedStepIndex + 1) % stepSizes.length;
            this.init();
        }).dimensions(rightX + 65, btnY + 25, 100, 20).build());

        // AUTO PLAY ROW
        int autoY = btnY + 50;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("START AUTO").formatted(Formatting.GOLD), btn -> {
            sendCommand("algo_auto " + autoDelays[selectedDelayIndex]);
            this.close();
        }).dimensions(rightX, autoY, 80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Delay: " + autoDelays[selectedDelayIndex] + "s"), btn -> {
            selectedDelayIndex = (selectedDelayIndex + 1) % autoDelays.length;
            this.init();
        }).dimensions(rightX + 85, autoY, 80, 20).build());
    }

    private void initCodeTab(int startX, int startY) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Coming Soon..."), btn -> {})
                .dimensions(startX + 100, startY + 40, 100, 20).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentTab == Tab.ALGORITHMS || currentTab == Tab.MULTI_ALGO) {
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                sendCommand("algo_step " + stepSizes[selectedStepIndex]);
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
                sendCommand("algo_step_back " + stepSizes[selectedStepIndex]);
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_B) {
                sendCommand("algo_pause");
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_C) {
                sendCommand("algo_resume");
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0xD9000000);
        super.render(context, mouseX, mouseY, delta);

        int startX = this.width / 2 - 150;
        int startY = 55;

        if (currentTab == Tab.MAPS) {
            if (isCreatingMap) {
                context.drawText(this.textRenderer, "Configure New Map", startX, startY, 0xFFFF55, true);
                context.drawText(this.textRenderer, "(More options coming soon...)", startX, startY + 85, 0xAAAAAA, false);
            } else {
                context.drawText(this.textRenderer, "Available Maps:", startX, startY + 25, 0xFFFFFF, false);
            }
        } else if (currentTab == Tab.ALGORITHMS) {
            int rightX = startX + 110;
            context.drawText(this.textRenderer, selectedAlgo.name, rightX, startY, 0xFFFF55, true);

            List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(selectedAlgo.desc), 190);
            int textY = startY + 15;
            for (OrderedText line : lines) {
                context.drawText(this.textRenderer, line, rightX, textY, 0xAAAAAA, false);
                textY += this.textRenderer.fontHeight + 2;
            }

            int hintY = startY + 40 + 25 + 25 + 25 + 25;
            context.drawText(this.textRenderer, "Arrows to Step | B to Pause | C to Resume", rightX - 10, hintY, 0xAAAAAA, false);

        } else if (currentTab == Tab.MULTI_ALGO) {
            context.drawText(this.textRenderer, "Parallel Race Simulator", startX + 130, startY, 0xFFFF55, true);

            int textY = startY + 21;
            for (AlgoInfo algo : algorithms) {
                context.drawText(this.textRenderer, algo.name, startX + 45, textY, 0xFFFFFF, false);
                textY += 25;
            }

            int hintY = startY + 40 + 25 + 25 + 25 + 10;
            context.drawText(this.textRenderer, "Arrows to Step | B to Pause | C to Resume", startX + 110, hintY, 0xAAAAAA, false);

        } else if (currentTab == Tab.CODE) {
            context.drawText(this.textRenderer, "Code Section is currently in development.", startX + 35, startY + 20, 0xAAAAAA, false);
        }
    }

    private void sendCommand(String command) {
        if (this.client != null && this.client.player != null) {
            this.client.player.networkHandler.sendCommand(command);
        }
    }
}
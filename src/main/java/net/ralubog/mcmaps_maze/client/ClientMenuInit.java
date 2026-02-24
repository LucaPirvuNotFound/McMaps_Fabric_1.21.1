package net.ralubog.mcmaps_maze.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.ralubog.mcmaps_maze.client.gui.ProjectMenuScreen;

public class ClientMenuInit implements ClientModInitializer {

    private static KeyBinding menuKeyBinding;
    private static KeyBinding stepForwardKey;
    private static KeyBinding stepBackKey;
    private static KeyBinding pauseAutoKey;
    private static KeyBinding resumeAutoKey;

    @Override
    public void onInitializeClient() {
        // 1. Register the Menu KeyBinding (G)
        menuKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mcmaps_maze.open_menu",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G,
                "category.mcmaps_maze.main"
        ));

        // 2. Register Arrow Keys for Algorithm Stepping
        stepForwardKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mcmaps_maze.step_forward",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT,
                "category.mcmaps_maze.main"
        ));

        stepBackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mcmaps_maze.step_back",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT,
                "category.mcmaps_maze.main"
        ));

        // 3. Register Auto-Play Controls (B for Pause, C for Continue)
        pauseAutoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mcmaps_maze.pause_auto",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B,
                "category.mcmaps_maze.main"
        ));

        resumeAutoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mcmaps_maze.resume_auto",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N,
                "category.mcmaps_maze.main"
        ));

        // 4. Listen for the key presses every client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKeyBinding.wasPressed()) {
                if (client.player != null) client.setScreen(new ProjectMenuScreen());
            }

            while (stepForwardKey.wasPressed()) {
                if (client.player != null) {
                    int size = ProjectMenuScreen.stepSizes[ProjectMenuScreen.selectedStepIndex];
                    client.player.networkHandler.sendCommand("algo_step " + size);
                }
            }

            while (stepBackKey.wasPressed()) {
                if (client.player != null) {
                    int size = ProjectMenuScreen.stepSizes[ProjectMenuScreen.selectedStepIndex];
                    client.player.networkHandler.sendCommand("algo_step_back " + size);
                }
            }

            // NEW: Send Pause Command
            while (pauseAutoKey.wasPressed()) {
                if (client.player != null) {
                    client.player.networkHandler.sendCommand("algo_pause");
                }
            }

            // NEW: Send Resume Command
            while (resumeAutoKey.wasPressed()) {
                if (client.player != null) {
                    client.player.networkHandler.sendCommand("algo_resume");
                }
            }
        });

        // Backup command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("projectmenu")
                    .executes(context -> {
                        MinecraftClient.getInstance().send(() -> {
                            MinecraftClient.getInstance().setScreen(new ProjectMenuScreen());
                        });
                        return 1;
                    }));
        });
    }
}
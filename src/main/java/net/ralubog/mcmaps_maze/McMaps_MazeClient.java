package net.ralubog.mcmaps_maze;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.ralubog.mcmaps_maze.item.custom.RoadWandItem;

public class McMaps_MazeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.options.pickItemKey.isPressed()) {
                if (client.player.getMainHandStack().getItem() instanceof RoadWandItem) {
                    RoadWandItem.startPos = null;
                    RoadWandItem.endPos = null;
                    client.player.sendMessage(Text.literal("Selection Cleared").formatted(Formatting.RED), true);
                }
            }
        });

        // In your Client Initializer
        WorldRenderEvents.END.register(context -> {
            if (RoadWandItem.startPos != null) {
                renderBeam(context, RoadWandItem.startPos, 0.9f, 0.6f, 0.1f); // Gold-ish
            }
            if (RoadWandItem.endPos != null) {
                renderBeam(context, RoadWandItem.endPos, 0.1f, 0.6f, 0.9f); // Cyan-ish
            }
        });



    }

    private void renderBeam(WorldRenderContext context, BlockPos pos, float r, float g, float b) {
        // Note: Rendering a real beacon beam involves using the BeaconBlockEntityRenderer
        // or drawing a custom cylinder in the BufferBuilder.
        // For a simple mod, many developers use 'debug' lines or a specific particle stream.
    }
}

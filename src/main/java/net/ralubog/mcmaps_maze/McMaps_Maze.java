package net.ralubog.mcmaps_maze;

import com.jcraft.jorbis.Block;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.ralubog.mcmaps_maze.commands.Create_Particle;
import net.ralubog.mcmaps_maze.commands.Create_Particle_Array;
import net.ralubog.mcmaps_maze.commands.utils.Road_Manager;
import net.ralubog.mcmaps_maze.commands.Hide_Particle_Array;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.util.Map;

public class McMaps_Maze implements ModInitializer {
	public static final String MOD_ID = "mcmaps_maze";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Create_Particle.register(); // Add this line
		Create_Particle_Array.register();
		Hide_Particle_Array.register();
		registerTickEvent(); // This keeps the particles "alive"
	}

	private void registerTickEvent() {
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			if (!Road_Manager.isVisible) return;

			for (ServerWorld world : server.getWorlds()) {
				// Loop through the waypoints and connect them

				for (int i = 0; i < Road_Manager.WAYPOINTS.size() - 1; i++) {
					Vec3d start = Vec3d.of(Road_Manager.WAYPOINTS.get(i));
					Vec3d end = Vec3d.of(Road_Manager.WAYPOINTS.get(i + 1));

					double distance = start.distanceTo(end);
					Vec3d direction = end.subtract(start).normalize();

					// Draw particles every 0.5 blocks between points
					for (double d = 0; d < distance; d += 0.5) {
						Vec3d particlePos = start.add(direction.multiply(d));
						world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
								particlePos.x, particlePos.y + 1.2, particlePos.z,
								1, 0, 0, 0, 0);
					}
				}
			}
		});
	}
}
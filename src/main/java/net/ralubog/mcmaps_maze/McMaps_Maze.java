package net.ralubog.mcmaps_maze;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.ralubog.mcmaps_maze.commands.*;
import net.ralubog.mcmaps_maze.commands.utils.Road_Manager;
import net.ralubog.mcmaps_maze.item.ModItems;
import net.ralubog.mcmaps_maze.item.custom.RoadWandItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

		// PENTRU ITEME CUSTOM
		ModItems.registerModItems();

		//------------------------------------------
		MapManager.loadLevels();
		Create_Particle.register();
		GenerateMenu.register();
		RunAlgorithm.register();
		AlgorithmMenu.register();
		Lesson1Graphs.register();
		VisualizeGraph.register();
		ExerciseLesson1.register();
		MapDesigner.register();

		GeneratePlatform.register();
		Create_Particle_Array.register();
		Hide_Particle_Array.register();
		registerTickEvent(); // This keeps the particles "alive"

	}

	private void registerTickEvent() {
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			if (!Road_Manager.isVisible) return;

			for (ServerWorld world : server.getWorlds()) {
				// Loop through the waypoints and connect them
				if (!Road_Manager.WAYPOINTS_ASTAR.isEmpty())
					show_particles(Road_Manager.WAYPOINTS_ASTAR, world, ParticleTypes.HAPPY_VILLAGER);

				if (!Road_Manager.WAYPOINTS_DIJKSTRA.isEmpty())
					show_particles(Road_Manager.WAYPOINTS_DIJKSTRA, world, ParticleTypes.END_ROD);

				if (!Road_Manager.WAYPOINTS_BELLMAN_FORD.isEmpty())
					show_particles(Road_Manager.WAYPOINTS_BELLMAN_FORD, world, ParticleTypes.WITCH);

				if (!Road_Manager.WAYPOINTS_GREEDY.isEmpty())
					show_particles(Road_Manager.WAYPOINTS_GREEDY, world, ParticleTypes.SQUID_INK);

			}
		});
	}

	private void show_particles(List<BlockPos> waypoints, ServerWorld world, SimpleParticleType particle) {
		for (int i = 0; i < waypoints.size() - 1; i++) {
			Vec3d start = Vec3d.of(waypoints.get(i));
			Vec3d end = Vec3d.of(waypoints.get(i + 1));

			double distance = start.distanceTo(end);
			Vec3d direction = end.subtract(start).normalize();

			// Draw particles every 0.5 blocks between points
			for (double d = 0; d < distance; d += 0.5) {
				Vec3d particlePos = start.add(direction.multiply(d));
				world.spawnParticles(particle,
						particlePos.x, particlePos.y + 1.2, particlePos.z,
						1, 0, 0, 0, 0);
			}
		}
	}
}
package net.ralubog.mcmaps_maze;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapManager {

    private static final Path LEVEL_PATH = FabricLoader.getInstance().getConfigDir().resolve("mcmaps_levels.json");
    private static final Path PLACEMENTS_PATH = FabricLoader.getInstance().getConfigDir().resolve("mcmaps_placements.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Stores the actual Map Data (Level ID -> Blocks String)
    private static Map<Integer, String[]> loadedLevels = new HashMap<>();

    // Stores the Location Links (X Coordinate -> Level ID)
    private static Map<Integer, Integer> platformPlacements = new HashMap<>();

    // --- LEVEL DATA ---
    public static void saveLevel(int levelId, String[] mapData) {
        loadedLevels.put(levelId, mapData);
        saveJson(LEVEL_PATH, loadedLevels);
    }

    public static String[] getLevel(int levelId) {
        return loadedLevels.get(levelId);
    }

    public static Set<Integer> getSavedLevelIds() {
        return loadedLevels.keySet();
    }

    public static int getNextAvailableId() {
        int maxId = 3;
        for (int id : loadedLevels.keySet()) {
            if (id > maxId) maxId = id;
        }
        return maxId + 1;
    }

    // --- PLACEMENT TRACKING (New!) ---

    // Call this when you generate a platform (e.g., "X=200 is Level 6")
    public static void linkPlatform(int x, int levelId) {
        platformPlacements.put(x, levelId);
        saveJson(PLACEMENTS_PATH, platformPlacements);
    }

    // Call this when you create a blank canvas (Removes the link)
    public static void unlinkPlatform(int x) {
        platformPlacements.remove(x);
        saveJson(PLACEMENTS_PATH, platformPlacements);
    }

    // Call this when saving to see if an ID already exists
    public static int getLinkedLevelId(int x) {
        return platformPlacements.getOrDefault(x, -1);
    }

    // --- LOAD/SAVE HELPERS ---
    public static void loadLevels() {
        loadedLevels = loadJson(LEVEL_PATH, new TypeToken<Map<Integer, String[]>>(){});
        platformPlacements = loadJson(PLACEMENTS_PATH, new TypeToken<Map<Integer, Integer>>(){});
        if (platformPlacements == null) platformPlacements = new HashMap<>();
        if (loadedLevels == null) loadedLevels = new HashMap<>();
    }

    private static <T> void saveJson(Path path, T data) {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(data, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static <T> T loadJson(Path path, TypeToken<T> typeToken) {
        if (!Files.exists(path)) return null;
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, typeToken.getType());
        } catch (IOException e) { e.printStackTrace(); return null; }
    }
}
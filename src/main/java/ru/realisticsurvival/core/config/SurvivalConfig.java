package ru.realisticsurvival.core.config;

import net.fabricmc.loader.api.FabricLoader;
import ru.realisticsurvival.RealisticSurvival;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** User-editable defaults. Server-authoritative gameplay configuration will be added with the systems. */
public final class SurvivalConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("realistic-survival.properties");
    private static final Properties VALUES = new Properties();

    private SurvivalConfig() {}

    public static synchronized void load() {
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.notExists(FILE)) writeDefaults();
            try (var reader = Files.newBufferedReader(FILE)) {
                VALUES.load(reader);
            }
            RealisticSurvival.LOGGER.info("Loaded Realistic Survival config from {}", FILE);
        } catch (IOException exception) {
            RealisticSurvival.LOGGER.error("Could not load Realistic Survival configuration", exception);
        }
    }

    private static void writeDefaults() throws IOException {
        VALUES.setProperty("hud.enabled", "true");
        VALUES.setProperty("hud.compact", "false");
        VALUES.setProperty("world.fire.spread", "realistic");
        VALUES.setProperty("world.fire.forest", "full");
        VALUES.setProperty("world.fire.structures", "full");
        VALUES.setProperty("world.fire.frequency", "normal");
        VALUES.setProperty("world.fire.explosions", "realistic");
        VALUES.setProperty("world.fire.player_damage", "hardcore");
        VALUES.setProperty("world.fire.npc", "advanced");
        VALUES.setProperty("world.fire.nature_recovery", "gradual");
        try (var writer = Files.newBufferedWriter(FILE)) {
            VALUES.store(writer, "Realistic Survival settings. Change values carefully.");
        }
    }

    public static boolean getBoolean(String key, boolean fallback) {
        return Boolean.parseBoolean(VALUES.getProperty(key, Boolean.toString(fallback)));
    }

    public static String getString(String key, String fallback) {
        return VALUES.getProperty(key, fallback);
    }
}

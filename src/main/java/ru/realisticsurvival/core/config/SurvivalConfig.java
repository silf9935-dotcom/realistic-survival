package ru.realisticsurvival.core.config;

import net.fabricmc.loader.api.FabricLoader;
import ru.realisticsurvival.RealisticSurvival;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** User-editable settings for the survival simulation and HUD. */
public final class SurvivalConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("realistic-survival.properties");
    private static final Properties VALUES = new Properties();

    private SurvivalConfig() {}

    public static synchronized void load() {
        VALUES.clear();
        setDefaults();
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.exists(FILE)) {
                try (var reader = Files.newBufferedReader(FILE)) {
                    VALUES.load(reader);
                }
            }
            try (var writer = Files.newBufferedWriter(FILE)) {
                VALUES.store(writer, "Realistic Survival settings. Changes apply after restarting the world/server.");
            }
            RealisticSurvival.LOGGER.info("Loaded Realistic Survival config from {}", FILE);
        } catch (IOException exception) {
            RealisticSurvival.LOGGER.error("Could not load Realistic Survival configuration", exception);
        }
    }

    private static void setDefaults() {
        VALUES.setProperty("hud.enabled", "true");
        VALUES.setProperty("hud.compact", "false");
        VALUES.setProperty("survival.cold_fatigue", "true");
        VALUES.setProperty("survival.hot_thirst", "true");
        VALUES.setProperty("survival.unsafe_water", "true");
        VALUES.setProperty("survival.unsafe_water_chance", "15");
        VALUES.setProperty("injuries.enabled", "true");
        VALUES.setProperty("injuries.damage_threshold", "4.0");
        VALUES.setProperty("injuries.severe_hit_threshold", "8.0");
        VALUES.setProperty("injuries.treatment_cooldown_seconds", "10");
        VALUES.setProperty("world.fire.spread", "realistic");
        VALUES.setProperty("world.fire.forest", "full");
        VALUES.setProperty("world.fire.structures", "full");
        VALUES.setProperty("world.fire.frequency", "normal");
        VALUES.setProperty("world.fire.explosions", "realistic");
        VALUES.setProperty("world.fire.player_damage", "hardcore");
        VALUES.setProperty("world.fire.npc", "advanced");
        VALUES.setProperty("world.fire.nature_recovery", "gradual");
        VALUES.setProperty("world.npc.faction_wars", "true");
        VALUES.setProperty("world.npc.faction_war_scan_ticks", "40");
        VALUES.setProperty("world.npc.faction_detection_radius", "24");
        VALUES.setProperty("world.npc.spawn_interval_seconds", "300");
        VALUES.setProperty("world.npc.spawn_radius", "8");
        VALUES.setProperty("world.npc.population_radius", "64");
        VALUES.setProperty("world.npc.population_cap", "6");
        VALUES.setProperty("world.npc.group_min", "2");
        VALUES.setProperty("world.npc.group_max", "3");
        VALUES.setProperty("world.npc.patrols_enabled", "true");
        VALUES.setProperty("world.npc.patrol_speed", "0.85");
        VALUES.setProperty("world.npc.checkpoints_enabled", "true");
        VALUES.setProperty("world.npc.checkpoint_alert_radius", "12");
        VALUES.setProperty("world.npc.checkpoint_guard_radius", "20");
    }

    public static boolean getBoolean(String key, boolean fallback) {
        return Boolean.parseBoolean(VALUES.getProperty(key, Boolean.toString(fallback)));
    }

    public static int getInt(String key, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(VALUES.getProperty(key, Integer.toString(fallback)).trim())));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static double getDouble(String key, double fallback, double min, double max) {
        try {
            double value = Double.parseDouble(VALUES.getProperty(key, Double.toString(fallback)).trim());
            if (!Double.isFinite(value)) return fallback;
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static String getString(String key, String fallback) {
        return VALUES.getProperty(key, fallback);
    }
}

package ru.realisticsurvival.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.phys.AABB;
import ru.realisticsurvival.core.config.SurvivalConfig;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent faction territory control and scheduled skirmish events.
 *
 * Place an invisible armor stand tagged rs_territory and optionally one of
 * rs_territory_start_bandits, rs_territory_start_military or
 * rs_territory_start_neutral. Ownership and capture progress are stored on
 * the marker itself, so they survive world saves.
 */
public final class TerritorySystem {
    private static final String TERRITORY_TAG = "rs_territory";
    private static final String START_PREFIX = "rs_territory_start_";
    private static final String OWNER_PREFIX = "rs_territory_owner_";
    private static final String PROGRESS_PREFIX = "rs_territory_progress_";
    private static final String EVENT_TIME_PREFIX = "rs_territory_event_time_";
    private static final String BANDIT_TAG = "rs_npc_bandit";
    private static final String MILITARY_TAG = "rs_npc_military";
    private static final String EVENT_TAG = "rs_territory_event";
    private static final String NEUTRAL = "neutral";
    private static final String PLAYER = "player";

    private TerritorySystem() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (!SurvivalConfig.getBoolean("world.npc.territories_enabled", true)) return;
            if (level.getGameTime() % 20L != 0L) return;

            Set<UUID> processed = new HashSet<>();
            for (ServerPlayer player : level.players()) {
                AABB scan = new AABB(player.blockPosition()).inflate(96);
                for (ArmorStand marker : level.getEntitiesOfClass(ArmorStand.class, scan,
                        stand -> stand.getTags().contains(TERRITORY_TAG))) {
                    if (!processed.add(marker.getUUID())) continue;
                    initializeOwner(marker);
                    tickCapture(level, marker);
                    tickSkirmish(level, marker);
                }
            }
        });
    }

    private static void initializeOwner(ArmorStand marker) {
        if (hasOwner(marker)) return;
        String initial = marker.getTags().contains(START_PREFIX + "bandits") ? "bandits"
                : marker.getTags().contains(START_PREFIX + "military") ? "military" : NEUTRAL;
        setOwner(marker, initial);
    }

    private static void tickCapture(ServerLevel level, ArmorStand marker) {
        int radius = SurvivalConfig.getInt("world.npc.territory_capture_radius", 12, 4, 32);
        int requiredSeconds = SurvivalConfig.getInt("world.npc.territory_capture_seconds", 30, 5, 300);
        AABB area = new AABB(marker.blockPosition()).inflate(radius);

        boolean bandits = !level.getEntitiesOfClass(Pillager.class, area,
                mob -> mob.isAlive() && mob.getTags().contains(BANDIT_TAG)).isEmpty();
        boolean military = !level.getEntitiesOfClass(Vindicator.class, area,
                mob -> mob.isAlive() && mob.getTags().contains(MILITARY_TAG)).isEmpty();
        boolean players = level.getEntitiesOfClass(ServerPlayer.class, area,
                player -> !player.isCreative() && !player.isSpectator()).size() > 0;

        String candidate = null;
        if (bandits && !military) candidate = "bandits";
        else if (military && !bandits) candidate = "military";
        else if (!bandits && !military && players) candidate = PLAYER;

        if (candidate == null) {
            clearProgress(marker);
            if (bandits && military) announceContested(level, marker, radius);
            return;
        }

        String owner = getOwner(marker);
        if (candidate.equals(owner)) {
            clearProgress(marker);
            return;
        }

        int progress = readProgress(marker, candidate) + 1;
        writeProgress(marker, candidate, progress);
        if (progress < requiredSeconds) return;

        setOwner(marker, candidate);
        clearProgress(marker);
        String label = ownerName(candidate);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(marker.blockPosition()).inflate(radius + 24),
                p -> !p.isSpectator())) {
            player.displayClientMessage(Component.literal("§6Территория захвачена: §f" + label), false);
        }
    }

    private static void tickSkirmish(ServerLevel level, ArmorStand marker) {
        int interval = SurvivalConfig.getInt("world.npc.territory_event_interval_seconds", 600, 120, 3600);
        long now = level.getGameTime();
        long last = readEventTime(marker);
        if (last < 0L) {
            writeEventTime(marker, now);
            return;
        }
        if (now - last < interval * 20L) return;
        writeEventTime(marker, now);

        int radius = SurvivalConfig.getInt("world.npc.population_radius", 64, 16, 128);
        int cap = SurvivalConfig.getInt("world.npc.population_cap", 6, 1, 24);
        AABB populationArea = new AABB(marker.blockPosition()).inflate(radius);
        int banditCount = level.getEntitiesOfClass(Pillager.class, populationArea,
                mob -> mob.getTags().contains(BANDIT_TAG)).size();
        int militaryCount = level.getEntitiesOfClass(Vindicator.class, populationArea,
                mob -> mob.getTags().contains(MILITARY_TAG)).size();
        if (banditCount >= cap || militaryCount >= cap) return;

        BlockPos firstPos = findSpawnPosition(level, marker.blockPosition());
        BlockPos secondPos = findSpawnPosition(level, marker.blockPosition());
        if (firstPos == null || secondPos == null) return;

        Pillager bandit = EntityType.PILLAGER.spawn(level, firstPos, EntitySpawnReason.EVENT);
        Vindicator soldier = EntityType.VINDICATOR.spawn(level, secondPos, EntitySpawnReason.EVENT);
        if (bandit == null || soldier == null) return;

        bandit.addTag(EVENT_TAG);
        soldier.addTag(EVENT_TAG);
        bandit.setPersistenceRequired();
        soldier.setPersistenceRequired();
        bandit.setTarget(soldier);
        soldier.setTarget(bandit);

        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(marker.blockPosition()).inflate(48), p -> !p.isSpectator())) {
            player.displayClientMessage(Component.literal("§cРядом с территорией началась перестрелка между бандитами и военными!"), false);
        }
    }

    private static BlockPos findSpawnPosition(ServerLevel level, BlockPos center) {
        for (int attempt = 0; attempt < 16; attempt++) {
            int x = center.getX() + level.random.nextInt(17) - 8;
            int z = center.getZ() + level.random.nextInt(17) - 8;
            for (int y = center.getY() + 6; y >= center.getY() - 6; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                        && level.getBlockState(pos.below()).isSolid()
                        && level.getWorldBorder().isWithinBounds(pos)) return pos;
            }
        }
        return null;
    }

    private static boolean hasOwner(ArmorStand marker) {
        for (String tag : marker.getTags()) if (tag.startsWith(OWNER_PREFIX)) return true;
        return false;
    }

    private static String getOwner(ArmorStand marker) {
        for (String tag : marker.getTags()) {
            if (tag.startsWith(OWNER_PREFIX)) return tag.substring(OWNER_PREFIX.length());
        }
        return NEUTRAL;
    }

    private static void setOwner(ArmorStand marker, String owner) {
        marker.getTags().stream().filter(tag -> tag.startsWith(OWNER_PREFIX))
                .toList().forEach(marker::removeTag);
        marker.addTag(OWNER_PREFIX + owner);
    }

    private static int readProgress(ArmorStand marker, String owner) {
        String prefix = PROGRESS_PREFIX + owner + "_";
        for (String tag : marker.getTags()) {
            if (!tag.startsWith(prefix)) continue;
            try { return Math.max(0, Integer.parseInt(tag.substring(prefix.length()))); }
            catch (NumberFormatException ignored) { return 0; }
        }
        return 0;
    }

    private static void writeProgress(ArmorStand marker, String owner, int progress) {
        clearProgress(marker);
        marker.addTag(PROGRESS_PREFIX + owner + "_" + progress);
    }

    private static void clearProgress(ArmorStand marker) {
        marker.getTags().stream().filter(tag -> tag.startsWith(PROGRESS_PREFIX))
                .toList().forEach(marker::removeTag);
    }

    private static long readEventTime(ArmorStand marker) {
        for (String tag : marker.getTags()) {
            if (!tag.startsWith(EVENT_TIME_PREFIX)) continue;
            try { return Long.parseLong(tag.substring(EVENT_TIME_PREFIX.length())); }
            catch (NumberFormatException ignored) { return -1L; }
        }
        return -1L;
    }

    private static void writeEventTime(ArmorStand marker, long time) {
        marker.getTags().stream().filter(tag -> tag.startsWith(EVENT_TIME_PREFIX))
                .toList().forEach(marker::removeTag);
        marker.addTag(EVENT_TIME_PREFIX + time);
    }

    private static String ownerName(String owner) {
        return switch (owner) {
            case "bandits" -> "Бандиты";
            case "military" -> "Военные";
            case PLAYER -> "Выжившие";
            default -> "Нейтральная";
        };
    }

    private static void announceContested(ServerLevel level, ArmorStand marker, int radius) {
        if (level.getGameTime() % 200L != 0L) return;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(marker.blockPosition()).inflate(radius + 24), p -> !p.isSpectator())) {
            player.displayClientMessage(Component.literal("§eТерритория оспаривается: бандиты и военные ведут бой."), true);
        }
    }
}

package ru.realisticsurvival.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import ru.realisticsurvival.core.config.SurvivalConfig;

/**
 * Turns the abstract camp stock into a small, persistent food reserve.
 * One stored unit feeds a nearby owner once per in-game minute.
 */
public final class SurvivorCampSystem {
    private static final String OWNER_PREFIX = "rs_survivor_owner_";
    private static final String CAMP_PREFIX = "rs_survivor_camp_";
    private static final String STOCK_PREFIX = "rs_survivor_stock_";
    private static long ticks;

    private SurvivorCampSystem() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!SurvivalConfig.getBoolean("npcs.survivors_enabled", true)
                    || ++ticks % 1200L != 0L) return;

            for (ServerPlayer owner : server.getPlayerList().getPlayers()) {
                if (owner.getFoodData().getFoodLevel() >= 18) continue;
                String ownerTag = OWNER_PREFIX + owner.getUUID();

                for (Villager survivor : owner.level().getEntitiesOfClass(Villager.class,
                        owner.getBoundingBox().inflate(32.0),
                        villager -> villager.isAlive() && villager.getTags().contains(ownerTag))) {
                    String campTag = survivor.getTags().stream()
                            .filter(tag -> tag.startsWith(CAMP_PREFIX)).findFirst().orElse(null);
                    if (campTag == null) continue;

                    BlockPos camp = parseCamp(campTag);
                    if (camp == null || owner.blockPosition().distSqr(camp) > 64.0) continue;

                    int stock = readStock(survivor);
                    if (stock <= 0) continue;

                    owner.getFoodData().eat(2, 0.3F);
                    survivor.getTags().stream().filter(tag -> tag.startsWith(STOCK_PREFIX))
                            .toList().forEach(survivor::removeTag);
                    survivor.addTag(STOCK_PREFIX + (stock - 1));
                    owner.displayClientMessage(Component.literal(
                            "Лагерь поделился запасами. Осталось единиц: " + (stock - 1) + "."), true);
                    break;
                }
            }
        });
    }

    private static BlockPos parseCamp(String tag) {
        String[] coordinates = tag.substring(CAMP_PREFIX.length()).split("_");
        if (coordinates.length != 3) return null;
        try {
            return new BlockPos(Integer.parseInt(coordinates[0]),
                    Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int readStock(Villager survivor) {
        for (String tag : survivor.getTags()) {
            if (!tag.startsWith(STOCK_PREFIX)) continue;
            try {
                return Math.max(0, Math.min(64,
                        Integer.parseInt(tag.substring(STOCK_PREFIX.length()))));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}

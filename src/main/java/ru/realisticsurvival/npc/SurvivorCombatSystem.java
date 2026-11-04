package ru.realisticsurvival.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import ru.realisticsurvival.core.config.SurvivalConfig;

/**
 * Gives recruited survivors a basic defensive combat role.
 * This deliberately uses vanilla targeting/pathfinding rather than a custom AI goal,
 * so it remains compatible with the existing villager-based companion prototype.
 */
public final class SurvivorCombatSystem {
    private static long ticks;

    private SurvivorCombatSystem() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!SurvivalConfig.getBoolean("npcs.survivor_combat_enabled", true)
                    || !SurvivalConfig.getBoolean("npcs.survivors_enabled", true)
                    || ++ticks % 10 != 0) return;

            for (ServerPlayer owner : server.getPlayerList().getPlayers()) {
                String ownerTag = "rs_survivor_owner_" + owner.getUUID();
                var level = owner.serverLevel();
                var hostiles = level.getEntitiesOfClass(Monster.class,
                        new AABB(owner.blockPosition()).inflate(14.0),
                        hostile -> hostile.isAlive() && !hostile.isRemoved());

                for (Villager companion : level.getEntitiesOfClass(Villager.class,
                        owner.getBoundingBox().inflate(32.0),
                        villager -> villager.isAlive() && villager.getTags().contains(ownerTag))) {
                    Monster threat = hostiles.stream()
                            .filter(hostile -> hostile.distanceToSqr(owner) <= 14.0 * 14.0)
                            .filter(hostile -> hostile.getTarget() == owner
                                    || hostile.getTarget() == companion
                                    || hostile.distanceToSqr(companion) < 10.0 * 10.0)
                            .min(java.util.Comparator.comparingDouble(companion::distanceToSqr))
                            .orElse(null);

                    if (threat != null) {
                        companion.setTarget(threat);
                    } else if (companion.getTarget() instanceof Monster current
                            && (!current.isAlive() || current.distanceToSqr(owner) > 20.0 * 20.0)) {
                        companion.setTarget(null);
                    }
                }
            }
        });
    }
}

package ru.realisticsurvival.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
                var level = owner.level();
                var hostiles = level.getEntitiesOfClass(Monster.class,
                        new AABB(owner.blockPosition()).inflate(14.0),
                        hostile -> hostile.isAlive() && !hostile.isRemoved());

                for (Villager companion : level.getEntitiesOfClass(Villager.class,
                        owner.getBoundingBox().inflate(32.0),
                        villager -> villager.isAlive() && villager.getTags().contains(ownerTag))) {
                    boolean guard = companion.getTags().contains("rs_survivor_role_guard");
                    boolean medic = companion.getTags().contains("rs_survivor_role_medic");
                    if (medic && companion.distanceToSqr(owner) <= 8.0 * 8.0
                            && owner.getHealth() < owner.getMaxHealth() * 0.65f && ticks % 40 == 0) {
                        owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
                    }
                    double defenseRadius = guard ? 22.0 : 14.0;
                    Monster threat = hostiles.stream()
                            .filter(hostile -> hostile.distanceToSqr(owner) <= defenseRadius * defenseRadius)
                            .filter(hostile -> hostile.getTarget() == owner
                                    || hostile.getTarget() == companion
                                    || hostile.distanceToSqr(companion) < 10.0 * 10.0)
                            .min(java.util.Comparator.comparingDouble((Monster hostile) -> companion.distanceToSqr(hostile)))
                            .orElse(null);

                    if (threat != null && (guard || threat.getTarget() == owner || threat.getTarget() == companion)) {
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

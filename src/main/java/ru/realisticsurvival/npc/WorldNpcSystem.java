package ru.realisticsurvival.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * The only NPC groups supported by the mod:
 * bandits (vanilla pillagers), military (vanilla vindicators), and
 * explicitly marked story characters (villagers).
 */
public final class WorldNpcSystem {
    public static final String STORY_TAG = "rs_story_npc";
    private static final String BANDIT_TAG = "rs_npc_bandit";
    private static final String MILITARY_TAG = "rs_npc_military";
    private static final String STORY_STAGE_PREFIX = "rs_story_stage_";

    private WorldNpcSystem() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Pillager) {
                entity.addTag(BANDIT_TAG);
                setDefaultName(entity, "Бандит");
            } else if (entity instanceof Vindicator) {
                entity.addTag(MILITARY_TAG);
                setDefaultName(entity, "Военный");
            }
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || world.isClientSide()
                    || !(entity instanceof Villager villager)
                    || !villager.getTags().contains(STORY_TAG)) {
                return InteractionResult.PASS;
            }

            int stage = readStage(villager);
            String name = villager.hasCustomName() ? villager.getCustomName().getString() : "Незнакомец";
            switch (stage) {
                case 0 -> serverPlayer.displayClientMessage(Component.literal(
                        name + ": «Тише. В этих местах опасно. Бандиты замечены у дорог, военные перекрыли старый тракт.»"), false);
                case 1 -> serverPlayer.displayClientMessage(Component.literal(
                        name + ": «Мне нужны припасы и сведения. Найди безопасный путь к заброшенному аванпосту — там начнётся наше дело.»"), false);
                default -> serverPlayer.displayClientMessage(Component.literal(
                        name + ": «Пока больше нечего рассказать. Возвращайся, когда узнаешь что-нибудь об аванпосте.»"), false);
            }
            setStage(villager, Math.min(2, stage + 1));
            return InteractionResult.SUCCESS;
        });
    }

    private static void setDefaultName(Entity entity, String name) {
        if (!entity.hasCustomName()) {
            entity.setCustomName(Component.literal(name));
            entity.setCustomNameVisible(true);
        }
    }

    private static int readStage(Villager villager) {
        for (String tag : villager.getTags()) {
            if (!tag.startsWith(STORY_STAGE_PREFIX)) continue;
            try {
                return Math.max(0, Math.min(2,
                        Integer.parseInt(tag.substring(STORY_STAGE_PREFIX.length()))));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static void setStage(Villager villager, int stage) {
        villager.getTags().stream().filter(tag -> tag.startsWith(STORY_STAGE_PREFIX))
                .toList().forEach(villager::removeTag);
        villager.addTag(STORY_STAGE_PREFIX + stage);
    }
}

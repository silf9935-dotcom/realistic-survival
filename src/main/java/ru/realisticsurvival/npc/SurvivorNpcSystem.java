package ru.realisticsurvival.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import ru.realisticsurvival.core.config.SurvivalConfig;

public final class SurvivorNpcSystem {
    private static final String OWNER_PREFIX = "rs_survivor_owner_";
    private static final String STAY_TAG = "rs_survivor_stay";
    private static long ticks;
    private SurvivorNpcSystem() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || world.isClientSide()
                    || !SurvivalConfig.getBoolean("npcs.survivors_enabled", true)
                    || !(entity instanceof Villager villager) || !serverPlayer.isShiftKeyDown()) return InteractionResult.PASS;
            String ownerTag = OWNER_PREFIX + serverPlayer.getUUID();
            if (villager.getTags().contains(ownerTag)) {
                if (villager.getTags().contains(STAY_TAG)) {
                    villager.removeTag(STAY_TAG);
                    serverPlayer.displayClientMessage(Component.literal("Выживший снова следует за вами."), false);
                } else {
                    villager.addTag(STAY_TAG);
                    serverPlayer.displayClientMessage(Component.literal("Выживший останется здесь."), false);
                }
                return InteractionResult.SUCCESS;
            }
            if (villager.getTags().stream().anyMatch(tag -> tag.startsWith(OWNER_PREFIX))) return InteractionResult.FAIL;
            if (!serverPlayer.getAbilities().instabuild && (!serverPlayer.getItemInHand(hand).is(Items.EMERALD)
                    || serverPlayer.getItemInHand(hand).getCount() < 5)) {
                serverPlayer.displayClientMessage(Component.literal("Присядьте и предложите 5 изумрудов, чтобы завербовать выжившего."), true);
                return InteractionResult.FAIL;
            }
            if (!serverPlayer.getAbilities().instabuild) serverPlayer.getItemInHand(hand).shrink(5);
            villager.addTag(ownerTag);
            villager.setCustomName(Component.literal("Выживший"));
            villager.setCustomNameVisible(true);
            serverPlayer.displayClientMessage(Component.literal("Выживший присоединился к вашей группе!"), false);
            return InteractionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!SurvivalConfig.getBoolean("npcs.survivors_enabled", true) || ++ticks % 10 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                String ownerTag = OWNER_PREFIX + player.getUUID();
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(48.0),
                        candidate -> candidate instanceof Villager && candidate.getTags().contains(ownerTag)
                                && !candidate.getTags().contains(STAY_TAG))) {
                    Villager villager = (Villager) entity;
                    double distance = villager.distanceToSqr(player);
                    if (distance > 6.25) villager.getNavigation().moveTo(player, 1.05);
                    else villager.getNavigation().stop();
                }
            }
        });
    }
}

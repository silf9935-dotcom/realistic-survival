package ru.realisticsurvival.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.realisticsurvival.core.config.SurvivalConfig;
import ru.realisticsurvival.item.MedicalItems;

public final class SurvivorNpcSystem {
    private static final String OWNER_PREFIX = "rs_survivor_owner_";
    private static final String STAY_TAG = "rs_survivor_stay";
    private static final String TRUST_PREFIX = "rs_survivor_trust_";
    private static long ticks;

    private SurvivorNpcSystem() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || world.isClientSide()
                    || !SurvivalConfig.getBoolean("npcs.survivors_enabled", true)
                    || !(entity instanceof Villager villager)) return InteractionResult.PASS;

            String ownerTag = OWNER_PREFIX + serverPlayer.getUUID();
            ItemStack held = serverPlayer.getItemInHand(hand);
            if (villager.getTags().contains(ownerTag)) {
                if (serverPlayer.isShiftKeyDown()) {
                    if (villager.getTags().contains(STAY_TAG)) {
                        villager.removeTag(STAY_TAG);
                        serverPlayer.displayClientMessage(Component.literal("Выживший снова следует за вами."), false);
                    } else {
                        villager.addTag(STAY_TAG);
                        villager.getNavigation().stop();
                        serverPlayer.displayClientMessage(Component.literal("Выживший останется здесь."), false);
                    }
                    return InteractionResult.SUCCESS;
                }
                if (held.is(Items.BREAD)) {
                    held.shrink(1);
                    int trust = changeTrust(villager, ownerTag, SurvivalConfig.getInt("npcs.bread_trust", 10, 1, 50));
                    serverPlayer.displayClientMessage(Component.literal("Выживший благодарен за еду. Доверие: " + trust + "/100."), false);
                    return InteractionResult.SUCCESS;
                }
                if (held.is(Items.EMERALD) && held.getCount() >= SurvivalConfig.getInt("npcs.bandage_price", 8, 1, 64)) {
                    int trust = readTrust(villager, ownerTag);
                    if (trust < SurvivalConfig.getInt("npcs.trade_min_trust", 20, 0, 100)) {
                        serverPlayer.displayClientMessage(Component.literal("Сначала заслужите доверие выжившего: принесите ему хлеб."), true);
                        return InteractionResult.FAIL;
                    }
                    held.shrink(SurvivalConfig.getInt("npcs.bandage_price", 8, 1, 64));
                    ItemStack bandage = new ItemStack(MedicalItems.BANDAGE);
                    if (!serverPlayer.getInventory().add(bandage)) serverPlayer.drop(bandage, false);
                    serverPlayer.displayClientMessage(Component.literal("Выживший обменял 8 изумрудов на бинт."), false);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }

            if (villager.getTags().stream().anyMatch(tag -> tag.startsWith(OWNER_PREFIX))) {
                serverPlayer.displayClientMessage(Component.literal("Этот выживший уже состоит в чужой группе."), true);
                return InteractionResult.FAIL;
            }
            if (!serverPlayer.isShiftKeyDown()) return InteractionResult.PASS;
            if (!serverPlayer.getAbilities().instabuild && (!held.is(Items.EMERALD) || held.getCount() < 5)) {
                serverPlayer.displayClientMessage(Component.literal("Присядьте и предложите 5 изумрудов, чтобы завербовать выжившего."), true);
                return InteractionResult.FAIL;
            }
            if (!serverPlayer.getAbilities().instabuild) held.shrink(5);
            villager.addTag(ownerTag);
            villager.addTag(TRUST_PREFIX + serverPlayer.getUUID() + "_" + SurvivalConfig.getInt("npcs.starting_trust", 10, 0, 100));
            villager.setCustomName(Component.literal("Выживший"));
            villager.setCustomNameVisible(true);
            serverPlayer.displayClientMessage(Component.literal("Выживший присоединился к вашей группе! Доверие: " + SurvivalConfig.getInt("npcs.starting_trust", 10, 0, 100) + "/100."), false);
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
                    if (villager.distanceToSqr(player) > 6.25) villager.getNavigation().moveTo(player, 1.05);
                    else villager.getNavigation().stop();
                }
            }
        });
    }

    private static int readTrust(Villager villager, String ownerTag) {
        String prefix = TRUST_PREFIX + ownerTag.substring(OWNER_PREFIX.length()) + "_";
        for (String tag : villager.getTags()) {
            if (tag.startsWith(prefix)) {
                try { return Math.max(0, Math.min(100, Integer.parseInt(tag.substring(prefix.length())))); }
                catch (NumberFormatException ignored) { return 0; }
            }
        }
        return 10;
    }

    private static int changeTrust(Villager villager, String ownerTag, int amount) {
        int trust = Math.min(100, readTrust(villager, ownerTag) + amount);
        String prefix = TRUST_PREFIX + ownerTag.substring(OWNER_PREFIX.length()) + "_";
        villager.getTags().stream().filter(tag -> tag.startsWith(prefix)).toList().forEach(villager::removeTag);
        villager.addTag(prefix + trust);
        return trust;
    }
}

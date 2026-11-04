package ru.realisticsurvival.npc;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.realisticsurvival.core.config.SurvivalConfig;

/**
 * Adds lightweight survivor jobs, faction identity and a persistent three-stage supply quest.
 * Data is stored on the villager as scoreboard tags, so it survives world saves.
 */
public final class SurvivorProgressionSystem {
    private static final String OWNER_PREFIX = "rs_survivor_owner_";
    private static final String ROLE_PREFIX = "rs_survivor_role_";
    private static final String FACTION_PREFIX = "rs_survivor_faction_";
    private static final String QUEST_PREFIX = "rs_survivor_chain_";
    private static final String CAMP_PREFIX = "rs_survivor_camp_";
    private static final String STOCK_PREFIX = "rs_survivor_stock_";

    private SurvivorProgressionSystem() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer owner) || world.isClientSide()
                    || !SurvivalConfig.getBoolean("npcs.survivors_enabled", true)
                    || !(entity instanceof Villager villager)) return InteractionResult.PASS;

            String ownerTag = OWNER_PREFIX + owner.getUUID();
            if (!villager.getTags().contains(ownerTag)) return InteractionResult.PASS;

            ItemStack held = owner.getItemInHand(hand);
            if (held.is(Items.CAMPFIRE) || held.is(Items.SOUL_CAMPFIRE)) {
                String camp = CAMP_PREFIX + villager.blockPosition().getX() + "_" + villager.blockPosition().getY() + "_" + villager.blockPosition().getZ();
                setSingleTag(villager, CAMP_PREFIX, camp);
                if (!owner.getAbilities().instabuild) held.shrink(1);
                owner.displayClientMessage(Component.literal("Лагерь отмечен у спутника. Он запомнит эту точку после перезагрузки мира."), false);
                return InteractionResult.SUCCESS;
            }
            if (held.is(Items.CHEST) || held.is(Items.BARREL)) {
                int stock = readNumber(villager, STOCK_PREFIX);
                setSingleTag(villager, STOCK_PREFIX, STOCK_PREFIX + Math.min(64, stock + 8));
                if (!owner.getAbilities().instabuild) held.shrink(1);
                owner.displayClientMessage(Component.literal("Запасы лагеря увеличены. Условный резерв: " + Math.min(64, stock + 8) + " единиц."), false);
                return InteractionResult.SUCCESS;
            }
            if (held.is(Items.BREAD) && villager.getTags().stream().anyMatch(tag -> tag.startsWith(CAMP_PREFIX))) {
                int stock = readNumber(villager, STOCK_PREFIX);
                int added = Math.min(held.getCount(), 64 - stock);
                setSingleTag(villager, STOCK_PREFIX, STOCK_PREFIX + (stock + added));
                if (!owner.getAbilities().instabuild) held.shrink(added);
                owner.displayClientMessage(Component.literal("Еда передана в лагерь. Запас: " + (stock + added) + " единиц."), false);
                return InteractionResult.SUCCESS;
            }
            if (held.is(Items.IRON_SWORD)) return assignRole(owner, villager, held, "guard", "Охранник", "npcs.role_item_consumption");
            if (held.is(Items.IRON_PICKAXE)) return assignRole(owner, villager, held, "scavenger", "Собиратель", "npcs.role_item_consumption");
            if (held.is(Items.SHEARS)) return assignRole(owner, villager, held, "medic", "Медик", "npcs.role_item_consumption");

            if (held.is(Items.WHITE_BANNER)) return assignFaction(owner, villager, held, "north", "Север");
            if (held.is(Items.RED_BANNER)) return assignFaction(owner, villager, held, "ash", "Пепел");
            if (held.is(Items.BLUE_BANNER)) return assignFaction(owner, villager, held, "free", "Вольные");

            if (held.is(Items.IRON_INGOT)) return advanceQuest(owner, villager, held, 0, 4,
                    Items.IRON_INGOT, 3, Items.EMERALD, "Снабжение лагеря", "Отлично. Металл пойдёт на укрепление лагеря.");
            if (held.is(Items.ROTTEN_FLESH)) return advanceQuest(owner, villager, held, 1, 8,
                    Items.ROTTEN_FLESH, 5, Items.BREAD, "Очистка окрестностей", "Запасы пополнены. Теперь у нас есть еда.");
            if (held.is(Items.COOKED_BEEF)) return advanceQuest(owner, villager, held, 2, 8,
                    Items.COOKED_BEEF, 1, Items.GOLDEN_APPLE, "Последний запас", "Ты доказал, что на тебя можно положиться.");

            if (held.isEmpty()) {
                String role = readValue(villager, ROLE_PREFIX, "без профессии");
                String faction = readValue(villager, FACTION_PREFIX, "без фракции");
                int stage = readQuest(villager, ownerTag);
                String camp = villager.getTags().stream().filter(tag -> tag.startsWith(CAMP_PREFIX)).findFirst().map(tag -> tag.substring(CAMP_PREFIX.length())).orElse("не разбит");
                int stock = readNumber(villager, STOCK_PREFIX);
                String schedule = owner.level().getDayTime() % 24000L < 12000L ? "дневная смена" : "ночной отдых";
                owner.displayClientMessage(Component.literal("Выживший [" + role + ", " + faction + "] Доверие: "
                        + readTrust(villager, ownerTag) + "/100. Сейчас: " + schedule
                        + ". Цепочка снабжения: " + Math.min(stage + 1, 3) + "/3. Лагерь: " + camp + ", запас: " + stock + "."), false);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    private static InteractionResult assignRole(ServerPlayer owner, Villager villager, ItemStack held,
                                                  String id, String label, String configKey) {
        setSingleTag(villager, ROLE_PREFIX, ROLE_PREFIX + id);
        if (SurvivalConfig.getBoolean(configKey, false) && !owner.getAbilities().instabuild) held.shrink(1);
        villager.setCustomName(Component.literal(label + " — Выживший"));
        villager.setCustomNameVisible(true);
        owner.displayClientMessage(Component.literal("Назначена роль: " + label
                + ". Охранник помогает в бою, медик и собиратель отмечены для дальнейших специализаций."), false);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult assignFaction(ServerPlayer owner, Villager villager, ItemStack held,
                                                     String id, String label) {
        setSingleTag(villager, FACTION_PREFIX, FACTION_PREFIX + id);
        if (!owner.getAbilities().instabuild) held.shrink(1);
        owner.displayClientMessage(Component.literal("Выживший вступил во фракцию «" + label + "»."), false);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult advanceQuest(ServerPlayer owner, Villager villager, ItemStack held,
                                                   int requiredStage, int amount, Item requiredItem,
                                                   int rewardCount, Item rewardItem, String title, String response) {
        String ownerTag = OWNER_PREFIX + owner.getUUID();
        int stage = readQuest(villager, ownerTag);
        if (stage != requiredStage) {
            if (stage >= 3) owner.displayClientMessage(Component.literal("Цепочка поручений завершена. Спасибо за помощь!"), true);
            else owner.displayClientMessage(Component.literal("Сначала выполни этап " + (stage + 1)
                    + ": " + questHint(stage) + "."), true);
            return InteractionResult.SUCCESS;
        }
        if (!held.is(requiredItem) || held.getCount() < amount) {
            owner.displayClientMessage(Component.literal(title + ": нужно " + amount + " предметов."), true);
            return InteractionResult.SUCCESS;
        }
        if (!owner.getAbilities().instabuild) held.shrink(amount);
        villager.getTags().stream().filter(tag -> tag.startsWith(QUEST_PREFIX + owner.getUUID() + "_"))
                .toList().forEach(villager::removeTag);
        villager.addTag(QUEST_PREFIX + owner.getUUID() + "_" + (requiredStage + 1));
        ItemStack reward = new ItemStack(rewardItem, rewardCount);
        if (!owner.getInventory().add(reward)) owner.drop(reward, false);
        owner.displayClientMessage(Component.literal("Поручение «" + title + "» выполнено. " + response), false);
        return InteractionResult.SUCCESS;
    }

    private static String questHint(int stage) {
        return switch (stage) {
            case 0 -> "принеси 4 железных слитка";
            case 1 -> "принеси 8 гнилой плоти";
            default -> "принеси 8 жареной говядины";
        };
    }

    private static int readQuest(Villager villager, String ownerTag) {
        String prefix = QUEST_PREFIX + ownerTag.substring(OWNER_PREFIX.length()) + "_";
        for (String tag : villager.getTags()) {
            if (tag.startsWith(prefix)) {
                try { return Math.max(0, Math.min(3, Integer.parseInt(tag.substring(prefix.length())))); }
                catch (NumberFormatException ignored) { return 0; }
            }
        }
        return 0;
    }

    private static int readTrust(Villager villager, String ownerTag) {
        String prefix = "rs_survivor_trust_" + ownerTag.substring(OWNER_PREFIX.length()) + "_";
        for (String tag : villager.getTags()) {
            if (tag.startsWith(prefix)) {
                try { return Math.max(0, Math.min(100, Integer.parseInt(tag.substring(prefix.length())))); }
                catch (NumberFormatException ignored) { return 0; }
            }
        }
        return 0;
    }

    private static int readNumber(Villager villager, String prefix) {\n        for (String tag : villager.getTags()) {\n            if (tag.startsWith(prefix)) {\n                try { return Math.max(0, Math.min(64, Integer.parseInt(tag.substring(prefix.length())))); }\n                catch (NumberFormatException ignored) { return 0; }\n            }\n        }\n        return 0;\n    }\n\n    private static String readValue(Villager villager, String prefix, String fallback) {
        return villager.getTags().stream().filter(tag -> tag.startsWith(prefix))
                .map(tag -> tag.substring(prefix.length())).findFirst().orElse(fallback);
    }

    private static void setSingleTag(Villager villager, String prefix, String value) {
        villager.getTags().stream().filter(tag -> tag.startsWith(prefix)).toList().forEach(villager::removeTag);
        villager.addTag(value);
    }
}

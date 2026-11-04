package ru.realisticsurvival.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Three focused NPC groups: hostile bandits, armed military and marked story characters.
 * Story quest state is stored directly on each character as scoreboard tags.
 */
public final class WorldNpcSystem {
    public static final String STORY_TAG = "rs_story_npc";
    private static final String BANDIT_TAG = "rs_npc_bandit";
    private static final String MILITARY_TAG = "rs_npc_military";
    private static final String STORY_STAGE_PREFIX = "rs_story_stage_";
    private static final String STORY_ROUTE_PREFIX = "rs_story_route_";

    private WorldNpcSystem() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Pillager pillager && !pillager.getTags().contains(BANDIT_TAG)) {
                pillager.addTag(BANDIT_TAG);
                setDefaultName(pillager, "Бандит");
                pillager.setCustomNameVisible(true);
                pillager.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                pillager.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
                pillager.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
                pillager.setPersistenceRequired();
            } else if (entity instanceof Vindicator vindicator
                    && !vindicator.getTags().contains(MILITARY_TAG)) {
                vindicator.addTag(MILITARY_TAG);
                setDefaultName(vindicator, "Военный");
                vindicator.setCustomNameVisible(true);
                vindicator.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                vindicator.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                vindicator.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                vindicator.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                vindicator.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                vindicator.setPersistenceRequired();
            } else if (entity instanceof Villager villager && villager.getTags().contains(STORY_TAG)) {
                villager.setPersistenceRequired();
            }
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || world.isClientSide()
                    || !(entity instanceof Villager villager)
                    || !villager.getTags().contains(STORY_TAG)) {
                return InteractionResult.PASS;
            }
            handleStoryInteraction(serverPlayer, villager, hand);
            return InteractionResult.SUCCESS;
        });
    }

    private static void handleStoryInteraction(ServerPlayer player, Villager character, InteractionHand hand) {
        int stage = readStage(character);
        String name = character.hasCustomName() ? character.getCustomName().getString() : "Незнакомец";

        if (stage == 0) {
            tell(player, name + ": «Тише. Бандиты держат дороги, а военные перекрыли старый тракт. Мне нужен человек, которому можно доверять.»");
            setStage(character, 1);
            tell(player, "Новое задание: выбери, как помочь связному — передай бумагу для разведданных или изумруд для припасов.");
            return;
        }

        if (stage == 1) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(Items.PAPER)) {
                consumeOne(player, hand, held);
                character.addTag(STORY_ROUTE_PREFIX + "intel");
                give(player, new ItemStack(Items.COMPASS));
                tell(player, name + ": «Хорошо. Эти сведения помогут обойти патрули. Возьми компас — он пригодится в пути.»");
                setStage(character, 2);
                tell(player, "Задание: принеси 5 железных слитков для подготовки к вылазке.");
            } else if (held.is(Items.EMERALD)) {
                consumeOne(player, hand, held);
                character.addTag(STORY_ROUTE_PREFIX + "supplies");
                give(player, new ItemStack(Items.BREAD, 3));
                tell(player, name + ": «Изумруд решает многое. Вот припасы, они тебе понадобятся.»");
                setStage(character, 2);
                tell(player, "Задание: принеси 5 железных слитков для подготовки к вылазке.");
            } else {
                tell(player, name + ": «Бумага — для разведданных. Изумруд — на припасы. Решай сам.»");
            }
            return;
        }

        if (stage == 2) {
            if (countItem(player, Items.IRON_INGOT) < 5) {
                tell(player, name + ": «Нужно ещё 5 железных слитков. Без снаряжения мы не выдвинемся.»");
                return;
            }
            removeItems(player, Items.IRON_INGOT, 5);
            give(player, new ItemStack(Items.GOLDEN_APPLE));
            give(player, new ItemStack(Items.EMERALD, 3));
            setStage(character, 3);
            tell(player, name + ": «Отличная работа. Теперь у нас есть шанс пройти через блокпост. Забери награду — скоро понадобится продолжить.»");
            tell(player, "Задание выполнено. Продолжение истории появится в следующем обновлении.");
            return;
        }

        String route = hasRoute(character, "intel") ? "Ты выбрал путь разведки. " : "Ты выбрал путь снабжения. ";
        tell(player, name + ": «" + route + "Мы ещё встретимся, когда будет готов следующий этап операции.»");
    }

    private static boolean hasRoute(Villager villager, String route) {
        return villager.getTags().contains(STORY_ROUTE_PREFIX + route);
    }

    private static void tell(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), false);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void consumeOne(ServerPlayer player, InteractionHand hand, ItemStack held) {
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
            if (held.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
        }
    }

    private static int countItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void removeItems(ServerPlayer player, net.minecraft.world.item.Item item, int amount) {
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) break;
            if (!stack.is(item)) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
    }

    private static void setDefaultName(Entity entity, String name) {
        if (!entity.hasCustomName()) {
            entity.setCustomName(Component.literal(name));
        }
    }

    private static int readStage(Villager villager) {
        for (String tag : villager.getTags()) {
            if (!tag.startsWith(STORY_STAGE_PREFIX)) continue;
            try {
                return Math.max(0, Math.min(3,
                        Integer.parseInt(tag.substring(STORY_STAGE_PREFIX.length())));
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

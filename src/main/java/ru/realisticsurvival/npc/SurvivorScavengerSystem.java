package ru.realisticsurvival.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import ru.realisticsurvival.core.config.SurvivalConfig;

/**
 * Scavengers recover dropped provisions near their camp and add them to its
 * persistent abstract stock. Collection is intentionally limited to food.
 */
public final class SurvivorScavengerSystem {
    private static final String OWNER_PREFIX = "rs_survivor_owner_";
    private static final String ROLE_TAG = "rs_survivor_role_scavenger";
    private static final String CAMP_PREFIX = "rs_survivor_camp_";
    private static final String STOCK_PREFIX = "rs_survivor_stock_";
    private static long ticks;

    private SurvivorScavengerSystem() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!SurvivalConfig.getBoolean("npcs.survivors_enabled", true)
                    || ++ticks % 40L != 0L) return;

            for (var owner : server.getPlayerList().getPlayers()) {
                String ownerTag = OWNER_PREFIX + owner.getUUID();
                for (Villager scavenger : owner.level().getEntitiesOfClass(Villager.class,
                        owner.getBoundingBox().inflate(48.0),
                        villager -> villager.isAlive()
                                && villager.getTags().contains(ownerTag)
                                && villager.getTags().contains(ROLE_TAG)
                                && villager.getTags().stream().anyMatch(tag -> tag.startsWith(CAMP_PREFIX)))) {
                    int stock = readStock(scavenger);
                    if (stock >= 64) continue;

                    for (ItemEntity dropped : owner.level().getEntitiesOfClass(ItemEntity.class,
                            new AABB(scavenger.blockPosition()).inflate(8.0),
                            item -> item.isAlive() && isProvision(item.getItem()))) {
                        int room = 64 - stock;
                        int collected = Math.min(room, dropped.getItem().getCount());
                        if (collected <= 0) break;

                        ItemStack remainder = dropped.getItem().copy();
                        remainder.shrink(collected);
                        if (remainder.isEmpty()) dropped.discard();
                        else dropped.setItem(remainder);

                        stock += collected;
                        setStock(scavenger, stock);
                        if (stock >= 64) break;
                    }
                }
            }
        });
    }

    private static boolean isProvision(ItemStack stack) {
        return stack.is(Items.BREAD) || stack.is(Items.COOKED_BEEF)
                || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.COOKED_CHICKEN)
                || stack.is(Items.COOKED_MUTTON) || stack.is(Items.COOKED_RABBIT)
                || stack.is(Items.BAKED_POTATO) || stack.is(Items.CARROT)
                || stack.is(Items.POTATO) || stack.is(Items.APPLE)
                || stack.is(Items.MELON_SLICE) || stack.is(Items.BEETROOT)
                || stack.is(Items.SWEET_BERRIES);
    }

    private static int readStock(Villager villager) {
        for (String tag : villager.getTags()) {
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

    private static void setStock(Villager villager, int stock) {
        villager.getTags().stream().filter(tag -> tag.startsWith(STOCK_PREFIX))
                .toList().forEach(villager::removeTag);
        villager.addTag(STOCK_PREFIX + Math.max(0, Math.min(64, stock)));
    }
}

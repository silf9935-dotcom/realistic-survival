package ru.realisticsurvival.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.RandomSource;
import ru.realisticsurvival.core.config.SurvivalConfig;
import java.util.HashSet;
import java.util.Set;

/**
 * Three focused NPC groups: hostile bandits, armed military and marked story characters.
 * Spawn markers are invisible armor stands tagged with rs_spawn_bandits or rs_spawn_military.
 */
public final class WorldNpcSystem {
    public static final String STORY_TAG = "rs_story_npc";
    public static final String BANDIT_SPAWN_TAG = "rs_spawn_bandits";
    public static final String MILITARY_SPAWN_TAG = "rs_spawn_military";
    public static final String MILITARY_CHECKPOINT_TAG = "rs_checkpoint_military";
    private static final String BANDIT_PATROL_PREFIX = "rs_patrol_bandits_";
    private static final String MILITARY_PATROL_PREFIX = "rs_patrol_military_";
    private static final String PATROL_ACTIVE_TAG = "rs_patrol_active";
    private static final String PATROL_STEP_PREFIX = "rs_patrol_step_";
    private static final String CHECKPOINT_ALERT_PREFIX = "rs_checkpoint_alert_";
    private static final int PATROL_WAYPOINT_COUNT = 4;
    private static final String BANDIT_TAG = "rs_npc_bandit";
    private static final String MILITARY_TAG = "rs_npc_military";
    private static final String STORY_STAGE_PREFIX = "rs_story_stage_";
    private static final String STORY_ROUTE_PREFIX = "rs_story_route_";
    private static final int PLAYER_SCAN_RADIUS = 96;

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

        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.getDifficulty() == Difficulty.PEACEFUL) return;

            long gameTime = level.getGameTime();
            int warInterval = SurvivalConfig.getInt("world.npc.faction_war_scan_ticks", 40, 20, 200);
            if (gameTime % warInterval == 0L) {
                if (SurvivalConfig.getBoolean("world.npc.faction_wars", true)) tickFactionWar(level);
                if (SurvivalConfig.getBoolean("world.npc.patrols_enabled", true)) tickPatrols(level);
                if (SurvivalConfig.getBoolean("world.npc.checkpoints_enabled", true)) tickCheckpoints(level);
            }

            int spawnIntervalSeconds = SurvivalConfig.getInt("world.npc.spawn_interval_seconds", 300, 60, 3600);
            if (gameTime % (spawnIntervalSeconds * 20L) != 0L) return;

            Set<java.util.UUID> processedMarkers = new HashSet<>();
            for (ServerPlayer player : level.players()) {
                for (ArmorStand marker : level.getEntitiesOfClass(ArmorStand.class,
                        new AABB(player.blockPosition()).inflate(PLAYER_SCAN_RADIUS),
                        stand -> stand.getTags().contains(BANDIT_SPAWN_TAG)
                                || stand.getTags().contains(MILITARY_SPAWN_TAG)
                                || stand.getTags().contains(MILITARY_CHECKPOINT_TAG))) {
                    if (!processedMarkers.add(marker.getUUID())) continue;
                    boolean bandits = marker.getTags().contains(BANDIT_SPAWN_TAG);
                    spawnMarkerGroup(level, marker, bandits);
                }
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

    private static void spawnMarkerGroup(ServerLevel level, ArmorStand marker, boolean bandits) {
        int populationRadius = SurvivalConfig.getInt("world.npc.population_radius", 64, 16, 128);
        int populationCap = SurvivalConfig.getInt("world.npc.population_cap", 6, 1, 24);
        AABB area = new AABB(marker.blockPosition()).inflate(populationRadius);
        int current = bandits
                ? level.getEntitiesOfClass(Pillager.class, area,
                    mob -> mob.getTags().contains(BANDIT_TAG)).size()
                : level.getEntitiesOfClass(Vindicator.class, area,
                    mob -> mob.getTags().contains(MILITARY_TAG)).size();
        int minGroup = SurvivalConfig.getInt("world.npc.group_min", 2, 1, 8);
        int maxGroup = SurvivalConfig.getInt("world.npc.group_max", 3, minGroup, 8);
        int amount = Math.min(minGroup + level.random.nextInt(maxGroup - minGroup + 1),
                Math.max(0, populationCap - current));
        for (int i = 0; i < amount; i++) {
            BlockPos spawnPos = findSpawnPosition(level, marker.blockPosition(), level.random);
            if (spawnPos == null) break;
            if (bandits) {
                Pillager pillager = EntityType.PILLAGER.spawn(level, spawnPos, EntitySpawnReason.EVENT);
                if (pillager != null) {
                    pillager.setPatrolTarget(marker.blockPosition());
                    pillager.setPatrolLeader(i == 0);
                    pillager.addTag(PATROL_ACTIVE_TAG);
                    pillager.addTag(PATROL_STEP_PREFIX + "0");
                }
            } else {
                Vindicator soldier = EntityType.VINDICATOR.spawn(level, spawnPos, EntitySpawnReason.EVENT);
                if (soldier != null) {
                    soldier.addTag(PATROL_ACTIVE_TAG);
                    soldier.addTag(PATROL_STEP_PREFIX + "0");
                }
            }
        }
    }

    private static void tickPatrols(ServerLevel level) {
        Set<java.util.UUID> seen = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            AABB area = new AABB(player.blockPosition()).inflate(PLAYER_SCAN_RADIUS);
            java.util.List<ArmorStand> waypoints = level.getEntitiesOfClass(ArmorStand.class, area,
                    stand -> hasPatrolWaypoint(stand, true) || hasPatrolWaypoint(stand, false));
            for (Pillager mob : level.getEntitiesOfClass(Pillager.class, area,
                    entity -> entity.getTags().contains(BANDIT_TAG) && entity.getTags().contains(PATROL_ACTIVE_TAG))) {
                if (seen.add(mob.getUUID())) tickPatrolMob(mob, waypoints, true);
            }
            for (Vindicator mob : level.getEntitiesOfClass(Vindicator.class, area,
                    entity -> entity.getTags().contains(MILITARY_TAG) && entity.getTags().contains(PATROL_ACTIVE_TAG))) {
                if (seen.add(mob.getUUID())) tickPatrolMob(mob, waypoints, false);
            }
        }
    }

    private static boolean hasPatrolWaypoint(ArmorStand stand, boolean bandits) {
        String prefix = bandits ? BANDIT_PATROL_PREFIX : MILITARY_PATROL_PREFIX;
        for (int i = 0; i < PATROL_WAYPOINT_COUNT; i++) if (stand.getTags().contains(prefix + i)) return true;
        return false;
    }

    private static void tickPatrolMob(Mob mob, java.util.List<ArmorStand> waypoints, boolean bandits) {
        String prefix = bandits ? BANDIT_PATROL_PREFIX : MILITARY_PATROL_PREFIX;
        java.util.List<ArmorStand> route = new java.util.ArrayList<>();
        for (int i = 0; i < PATROL_WAYPOINT_COUNT; i++) {
            for (ArmorStand stand : waypoints) if (stand.getTags().contains(prefix + i)) route.add(stand);
        }
        if (route.isEmpty()) return;
        int step = readPatrolStep(mob);
        ArmorStand destination = null;
        for (ArmorStand stand : route) if (stand.getTags().contains(prefix + step)) { destination = stand; break; }
        if (destination == null) {
            destination = route.get(0);
            setPatrolStep(mob, patrolWaypointIndex(destination, prefix));
        }
        if (mob.distanceToSqr(destination) <= 9.0) {
            int routePosition = route.indexOf(destination);
            ArmorStand nextWaypoint = route.get((routePosition + 1) % route.size());
            destination = nextWaypoint;
            setPatrolStep(mob, patrolWaypointIndex(nextWaypoint, prefix));
        }
        if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
            double speed = SurvivalConfig.getDouble("world.npc.patrol_speed", 0.85, 0.3, 1.5);
            mob.getNavigation().moveTo(destination.getX(), destination.getY(), destination.getZ(), speed);
        }
    }

    private static int patrolWaypointIndex(ArmorStand stand, String prefix) {
        for (int i = 0; i < PATROL_WAYPOINT_COUNT; i++) if (stand.getTags().contains(prefix + i)) return i;
        return 0;
    }

    private static int readPatrolStep(Mob mob) {
        for (String tag : mob.getTags()) if (tag.startsWith(PATROL_STEP_PREFIX)) {
            try { return Math.max(0, Math.min(PATROL_WAYPOINT_COUNT - 1, Integer.parseInt(tag.substring(PATROL_STEP_PREFIX.length())))); }
            catch (NumberFormatException ignored) { return 0; }
        }
        return 0;
    }

    private static void setPatrolStep(Mob mob, int step) {
        mob.getTags().stream().filter(tag -> tag.startsWith(PATROL_STEP_PREFIX)).toList().forEach(mob::removeTag);
        mob.addTag(PATROL_STEP_PREFIX + step);
    }

    private static void tickCheckpoints(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            AABB scan = new AABB(player.blockPosition()).inflate(PLAYER_SCAN_RADIUS);
            for (ArmorStand checkpoint : level.getEntitiesOfClass(ArmorStand.class, scan,
                    stand -> stand.getTags().contains(MILITARY_CHECKPOINT_TAG))) {
                String alertTag = CHECKPOINT_ALERT_PREFIX + checkpoint.getUUID();
                double distance = player.distanceToSqr(checkpoint);
                int alertRadius = SurvivalConfig.getInt("world.npc.checkpoint_alert_radius", 12, 4, 32);
                if (distance <= (double) alertRadius * alertRadius && !player.getTags().contains(alertTag)
                        && !player.isCreative() && !player.isSpectator()) {
                    player.addTag(alertTag);
                    player.displayClientMessage(Component.literal("Военный блокпост впереди. Не приближайся без подготовки."), true);
                } else if (distance > (double) (alertRadius + 4) * (alertRadius + 4)) player.removeTag(alertTag);
                int guardRadius = SurvivalConfig.getInt("world.npc.checkpoint_guard_radius", 20, 8, 40);
                if (distance <= (double) guardRadius * guardRadius && !player.isCreative() && !player.isSpectator()) {
                    AABB guardArea = new AABB(checkpoint.blockPosition()).inflate(guardRadius);
                    for (Vindicator guard : level.getEntitiesOfClass(Vindicator.class, guardArea,
                            mob -> mob.getTags().contains(MILITARY_TAG) && (mob.getTarget() == null || !mob.getTarget().isAlive())))
                        guard.setTarget(player);
                }
            }
        }
    }
    private static void tickFactionWar(ServerLevel level) {
        Set<java.util.UUID> seenBandits = new HashSet<>();
        Set<java.util.UUID> seenMilitary = new HashSet<>();
        java.util.List<Pillager> bandits = new java.util.ArrayList<>();
        java.util.List<Vindicator> military = new java.util.ArrayList<>();

        for (ServerPlayer player : level.players()) {
            AABB activeArea = new AABB(player.blockPosition()).inflate(PLAYER_SCAN_RADIUS);
            for (Pillager pillager : level.getEntitiesOfClass(Pillager.class, activeArea,
                    mob -> mob.getTags().contains(BANDIT_TAG))) {
                if (seenBandits.add(pillager.getUUID())) bandits.add(pillager);
            }
            for (Vindicator vindicator : level.getEntitiesOfClass(Vindicator.class, activeArea,
                    mob -> mob.getTags().contains(MILITARY_TAG))) {
                if (seenMilitary.add(vindicator.getUUID())) military.add(vindicator);
            }
        }

        double detectionRadius = SurvivalConfig.getInt("world.npc.faction_detection_radius", 24, 8, 64);
        for (Pillager bandit : bandits) acquireFactionTarget(bandit, military, detectionRadius);
        for (Vindicator soldier : military) acquireFactionTarget(soldier, bandits, detectionRadius);
    }

    private static void acquireFactionTarget(Mob mob, java.util.List<? extends Mob> enemies,
                                               double detectionRadius) {
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget != null && currentTarget.isAlive()) return;

        double maxDistance = detectionRadius * detectionRadius;
        Mob nearest = null;
        double nearestDistance = maxDistance;
        for (Mob enemy : enemies) {
            if (!enemy.isAlive() || enemy == mob || !mob.hasLineOfSight(enemy)) continue;
            double distance = mob.distanceToSqr(enemy);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = enemy;
            }
        }
        if (nearest != null) mob.setTarget(nearest);
    }

    private static BlockPos findSpawnPosition(ServerLevel level, BlockPos center, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int spawnRange = SurvivalConfig.getInt("world.npc.spawn_radius", 8, 2, 24);
            int x = center.getX() + random.nextInt(spawnRange * 2 + 1) - spawnRange;
            int z = center.getZ() + random.nextInt(spawnRange * 2 + 1) - spawnRange;
            int startY = center.getY() + 8;
            int minY = center.getY() - 8;
            for (int y = startY; y >= minY; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                        && level.getBlockState(pos.below()).isSolid()
                        && level.getWorldBorder().isWithinBounds(pos)) return pos;
            }
        }
        return null;
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
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static void consumeOne(ServerPlayer player, InteractionHand hand, ItemStack held) {
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
            if (held.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
        }
    }

    private static int countItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void removeItems(ServerPlayer player, net.minecraft.world.item.Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
    }

    private static void setDefaultName(Entity entity, String name) {
        if (!entity.hasCustomName()) entity.setCustomName(Component.literal(name));
    }

    private static int readStage(Villager villager) {
        for (String tag : villager.getTags()) {
            if (!tag.startsWith(STORY_STAGE_PREFIX)) continue;
            try {
                return Math.max(0, Math.min(3,
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

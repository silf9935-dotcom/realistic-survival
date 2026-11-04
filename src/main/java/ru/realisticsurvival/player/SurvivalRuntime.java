package ru.realisticsurvival.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * First playable survival loop. Values are stored as entity scoreboard tags,
 * so they survive disconnects and world saves without a custom networking layer.
 */
public final class SurvivalRuntime {
    private static final String TAG_PREFIX = "rs_vital_";
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static long ticks;

    private SurvivalRuntime() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            STATES.put(player.getUUID(), State.read(player));
            player.displayClientMessage(Component.literal("§6Realistic Survival §7— системы выживания активны."), true);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            State state = STATES.remove(player.getUUID());
            if (state != null) state.write(player);
        });
        ServerTickEvents.END_SERVER_TICK.register(SurvivalRuntime::tick);
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer
                    && !serverPlayer.isSpectator()
                    && serverPlayer.isShiftKeyDown()
                    && serverPlayer.getItemInHand(hand).isEmpty()
                    && world.getBlockState(hit.getBlockPos()).getFluidState().is(FluidTags.WATER)) {
                State state = STATES.computeIfAbsent(serverPlayer.getUUID(), ignored -> State.read(serverPlayer));
                long now = world.getGameTime();
                if (now - state.lastDrinkTick < 40) return InteractionResult.PASS;
                if (state.thirst >= 100) {
                    serverPlayer.displayClientMessage(Component.literal("§bЖажда уже утолена."), true);
                    return InteractionResult.PASS;
                }
                state.lastDrinkTick = now;
                state.thirst = Math.min(100, state.thirst + 20);
                state.write(serverPlayer);
                serverPlayer.displayClientMessage(Component.literal("§bВы утолили жажду: +20%"), true);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    private static void tick(MinecraftServer server) {
        ticks++;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            State state = STATES.computeIfAbsent(player.getUUID(), ignored -> State.read(player));

            if (ticks % 600 == 0) state.thirst = Math.max(0, state.thirst - 1);

            // Quiet rest: crouching while nearly motionless gradually reduces fatigue.
            if (!player.isSleeping() && player.isShiftKeyDown()
                    && player.getDeltaMovement().horizontalDistanceSqr() < 0.0025) {
                state.restTicks++;
                if (state.restTicks % 200 == 0) {
                    state.fatigue = Math.max(0, state.fatigue - 1);
                    state.stamina = Math.min(100, state.stamina + 2);
                }
            } else {
                state.restTicks = 0;
            }
            if (player.isSleeping()) {
                if (ticks % 120 == 0) state.fatigue = Math.max(0, state.fatigue - 1);
                if (ticks % 40 == 0) state.stamina = Math.min(100, state.stamina + 2);
                if (ticks % 600 == 0) state.sanity = Math.min(100, state.sanity + 1);
            } else {
                if (ticks % 1200 == 0) state.fatigue = Math.min(100, state.fatigue + 1);
                if (player.isSprinting() && ticks % 300 == 0) {
                    state.fatigue = Math.min(100, state.fatigue + 1);
                    state.thirst = Math.max(0, state.thirst - 1);
                }
            }

            // Core temperature is stored in tenths of a degree Celsius (370 = 37.0 C).
            if (ticks % 100 == 0) {
                float biomeTemperature = player.level().getBiome(player.blockPosition()).value().getBaseTemperature();
                if (player.isOnFire()) {
                    state.bodyTemperature = Math.min(430, state.bodyTemperature + 2);
                } else if (player.isInWater()) {
                    state.bodyTemperature = Math.max(250, state.bodyTemperature - 2);
                } else if (biomeTemperature >= 1.5F) {
                    state.bodyTemperature = Math.min(430, state.bodyTemperature + 1);
                } else if (biomeTemperature <= 0.2F && player.level().isRainingAt(player.blockPosition())) {
                    state.bodyTemperature = Math.max(250, state.bodyTemperature - 1);
                } else if (player.isSprinting()) {
                    state.bodyTemperature = Math.min(430, state.bodyTemperature + 1);
                } else if (ticks % 200 == 0) {
                    if (state.bodyTemperature < 370) state.bodyTemperature++;
                    else if (state.bodyTemperature > 370) state.bodyTemperature--;
                }
                // Heat stress increases fluid loss; cold stress makes physical recovery harder.
                if (state.bodyTemperature >= 390 && ticks % 300 == 0) {
                    state.thirst = Math.max(0, state.thirst - 1);
                }
            }
            if (state.bodyTemperature <= 350) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 0, false, false, true));
            }
            if (state.bodyTemperature <= 330) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 45, 0, false, false, true));
            }
            if (state.bodyTemperature >= 390) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45, 0, false, false, true));
            }
            if (state.bodyTemperature >= 410) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 45, 0, false, false, true));
            }

            int hunger = player.getFoodData().getFoodLevel();
            boolean exhaustedByHunger = hunger <= 6;

            if (hunger <= 6 && ticks % 600 == 0 && !player.isSleeping()) {
                state.fatigue = Math.min(100, state.fatigue + 1);
            }

            if (player.isSprinting()) {
                int sprintInterval = exhaustedByHunger ? 3 : 5;
                if (state.thirst < 30) sprintInterval = Math.max(2, sprintInterval - 1);
                if (ticks % sprintInterval == 0) state.stamina = Math.max(0, state.stamina - 1);
            } else {
                int recoveryInterval = exhaustedByHunger ? 6 : 3;
                if (state.thirst < 30) recoveryInterval *= 2;
                if (state.bodyTemperature <= 340) recoveryInterval *= 2;
                if (state.bodyTemperature >= 400) recoveryInterval *= 2;
                if (ticks % recoveryInterval == 0) state.stamina = Math.min(100, state.stamina + 1);
            }

            if (state.fatigue >= 70 && ticks % 1800 == 0) {
                state.sanity = Math.max(0, state.sanity - 1);
            }

            if (state.stamina == 0 && player.isSprinting()) {
                player.setSprinting(false);
            }
            if (state.thirst < 15) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45, 0, false, false, true));
            }
            if (state.fatigue >= 90) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 45, 0, false, false, true));
            }
            if (state.sanity <= 15 && ticks % 200 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0, false, false, true));
            }
            if (state.sanity <= 5 && ticks % 400 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, false, true));
            }
            if (state.thirst == 0 && ticks % 1200 == 0) {
                player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - 1));
            }

            if (ticks % 20 == 0) {
                state.write(player);
                player.displayClientMessage(Component.literal(
                    "§cГолод §f" + player.getFoodData().getFoodLevel() * 5 + "%  " +
                    "§bЖажда §f" + state.thirst + "%  " +
                    "§eВыносливость §f" + state.stamina + "%  " +
                    "§6Усталость §f" + state.fatigue + "%  " +
                    "§dРассудок §f" + state.sanity + "%  " +
                    "§eТемпература §f" + String.format(java.util.Locale.ROOT, "%.1f", state.bodyTemperature / 10.0) + "°C"
                ), true);
            }
        }
    }

    private static final class State {
        int thirst = 100;
        int fatigue = 0;
        int stamina = 100;
        int sanity = 100;
        int bodyTemperature = 370;
        long lastDrinkTick = -1000L;
        int restTicks = 0;

        static State read(ServerPlayer player) {
            State state = new State();
            state.thirst = readValue(player, "thirst", 100);
            state.fatigue = readValue(player, "fatigue", 0);
            state.stamina = readValue(player, "stamina", 100);
            state.sanity = readValue(player, "sanity", 100);
            state.bodyTemperature = readTemperature(player);
            return state;
        }

        void write(ServerPlayer player) {
            writeValue(player, "thirst", thirst);
            writeValue(player, "fatigue", fatigue);
            writeValue(player, "stamina", stamina);
            writeValue(player, "sanity", sanity);
            writeTemperature(player, bodyTemperature);
        }

        private static int readTemperature(ServerPlayer player) {
            String prefix = TAG_PREFIX + "temperature_";
            for (String tag : player.getTags()) {
                if (tag.startsWith(prefix)) {
                    try { return Math.max(250, Math.min(430, Integer.parseInt(tag.substring(prefix.length())))); }
                    catch (NumberFormatException ignored) { return 370; }
                }
            }
            return 370;
        }

        private static void writeTemperature(ServerPlayer player, int value) {
            String prefix = TAG_PREFIX + "temperature_";
            player.getTags().stream().filter(tag -> tag.startsWith(prefix)).toList().forEach(player::removeTag);
            player.addTag(prefix + Math.max(250, Math.min(430, value)));
        }

        private static int readValue(ServerPlayer player, String key, int fallback) {
            String prefix = TAG_PREFIX + key + "_";
            for (String tag : player.getTags()) {
                if (tag.startsWith(prefix)) {
                    try { return Math.max(0, Math.min(100, Integer.parseInt(tag.substring(prefix.length())))); }
                    catch (NumberFormatException ignored) { return fallback; }
                }
            }
            return fallback;
        }

        private static void writeValue(ServerPlayer player, String key, int value) {
            String prefix = TAG_PREFIX + key + "_";
            player.getTags().stream().filter(tag -> tag.startsWith(prefix)).toList().forEach(player::removeTag);
            player.addTag(prefix + Math.max(0, Math.min(100, value)));
        }
    }
}

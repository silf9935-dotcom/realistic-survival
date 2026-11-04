package ru.realisticsurvival.player;


import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import ru.realisticsurvival.core.config.SurvivalConfig;
import ru.realisticsurvival.item.MedicalItems;

/**
 * Lightweight injury layer for the beta: hard hits create persistent wounds;
 * craftable bandages and paper can be used to treat wounds.
 */
public final class InjurySystem {
    private static final String TAG = "rs_injury_";
    private static long ticks;
    private static final Map<UUID, Long> NEXT_TREATMENT_TICK = new HashMap<>();

    private InjurySystem() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!SurvivalConfig.getBoolean("injuries.enabled", true)) return;
            if (!(entity instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return;
            double injuryThreshold = SurvivalConfig.getDouble("injuries.damage_threshold", 4.0, 0.0, 40.0);
            double severeThreshold = SurvivalConfig.getDouble("injuries.severe_hit_threshold", 8.0, injuryThreshold, 80.0);
            if (damageTaken >= injuryThreshold) {
                int severity = Math.min(3, read(player) + (damageTaken >= severeThreshold ? 2 : 1));
                write(player, severity);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§cПолучена травма! Тяжесть: " + severity + "/3. Используй бумагу как импровизированную повязку."), true);
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || !SurvivalConfig.getBoolean("injuries.enabled", true)
                    || !(player instanceof ServerPlayer serverPlayer)
                    || !(stack.is(MedicalItems.BANDAGE) || stack.is(Items.PAPER)) || read(serverPlayer) <= 0) {
                return InteractionResult.PASS;
            }
            long readyAt = NEXT_TREATMENT_TICK.getOrDefault(serverPlayer.getUUID(), 0L);
            if (ticks < readyAt) {
                long seconds = Math.max(1L, (readyAt - ticks + 19L) / 20L);
                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§eПодожди перед следующей перевязкой: " + seconds + " сек."), true);
                return InteractionResult.FAIL;
            }
            int severity = read(serverPlayer);
            write(serverPlayer, severity - 1);
            int cooldownSeconds = SurvivalConfig.getInt("injuries.treatment_cooldown_seconds", 10, 0, 300);
            NEXT_TREATMENT_TICK.put(serverPlayer.getUUID(), ticks + cooldownSeconds * 20L);
            if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true, true));
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    severity == 1 ? "§aТы перевязал рану." : "§eРана обработана, но нужна ещё одна повязка."), true);
            return InteractionResult.SUCCESS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!SurvivalConfig.getBoolean("injuries.enabled", true)) return;
            ticks++;
            if (ticks % 40 != 0) return;
            NEXT_TREATMENT_TICK.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                int severity = read(player);
                if (severity >= 2) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, false, false, true));
                }
                if (severity >= 3) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 0, false, false, true));
                }
            }
        });
    }

    public static int getSeverity(ServerPlayer player) {
        return read(player);
    }

    private static int read(ServerPlayer player) {
        String prefix = TAG;
        for (String tag : player.getTags()) {
            if (tag.startsWith(prefix)) {
                try { return Math.max(0, Math.min(3, Integer.parseInt(tag.substring(prefix.length())))); }
                catch (NumberFormatException ignored) { return 0; }
            }
        }
        return 0;
    }

    private static void write(ServerPlayer player, int severity) {
        player.getTags().stream().filter(tag -> tag.startsWith(TAG)).toList().forEach(player::removeTag);
        int clamped = Math.max(0, Math.min(3, severity));
        if (clamped > 0) player.addTag(TAG + clamped);
    }
}

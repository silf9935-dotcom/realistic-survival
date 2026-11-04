package ru.realisticsurvival.player;


import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import ru.realisticsurvival.core.config.SurvivalConfig;

/**
 * Lightweight injury layer for the beta: hard hits create persistent wounds;
 * an ordinary paper item can be used as an improvised bandage.
 */
public final class InjurySystem {
    private static final String TAG = "rs_injury_";
    private static long ticks;

    private InjurySystem() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!SurvivalConfig.getBoolean("injuries.enabled", true)) return;
            if (!(entity instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return;
            if (damageTaken >= 4.0F) {
                int severity = Math.min(3, read(player) + (damageTaken >= 8.0F ? 2 : 1));
                write(player, severity);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§cПолучена травма! Тяжесть: " + severity + "/3. Используй бумагу как импровизированную повязку."), true);
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || !SurvivalConfig.getBoolean("injuries.enabled", true)
                    || !(player instanceof ServerPlayer serverPlayer)
                    || !stack.is(Items.PAPER) || read(serverPlayer) <= 0) {
                return InteractionResult.PASS;
            }
            int severity = read(serverPlayer);
            write(serverPlayer, severity - 1);
            if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true, true));
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    severity == 1 ? "§aТы перевязал рану." : "§eРана обработана, но нужна ещё одна повязка."), true);
            return InteractionResult.SUCCESS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ticks++;
            if (ticks % 40 != 0) return;
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

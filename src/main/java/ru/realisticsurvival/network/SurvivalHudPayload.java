package ru.realisticsurvival.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SurvivalHudPayload(int health, int hunger, int thirst, int stamina, int fatigue, int sanity,
                                 int temperatureTenths, int injuryMask, int carryWeightTenths)
        implements CustomPacketPayload {
    public static final Type<SurvivalHudPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("realistic_survival", "survival_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SurvivalHudPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SurvivalHudPayload::health,
            ByteBufCodecs.VAR_INT, SurvivalHudPayload::hunger,
            ByteBufCodecs.VAR_INT, SurvivalHudPayload::thirst,
            ByteBufCodecs.VAR_INT, SurvivalHudPayload::stamina,
            ByteBufCodecs.VAR_INT, SurvivalHudPayload::fatigue,
            ByteBufCodecs.VAR_INT, SurvivalHudPayload::sanity,
            ByteBufCodecs.VAR_INT, SurvivalHudPayload::temperatureTenths,
            ByteBufCodecs.VAR_INT, SurvivalHudPayload::injuryMask,
            ByteBufCodecs.VAR_INT, SurvivalHudPayload::carryWeightTenths,
            SurvivalHudPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

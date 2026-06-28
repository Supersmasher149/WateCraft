package com.wally.kissmod.network;

import com.wally.kissmod.kissmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record KissStartPayload(UUID playerUUID, UUID targetUUID, int durationTicks) implements CustomPacketPayload {
    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UUID decode(RegistryFriendlyByteBuf buf) {
                    return buf.readUUID();
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, UUID value) {
                    buf.writeUUID(value);
                }
            };

    public static final CustomPacketPayload.Type<KissStartPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(kissmod.MODID, "kiss_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KissStartPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUID_STREAM_CODEC, KissStartPayload::playerUUID,
                    UUID_STREAM_CODEC, KissStartPayload::targetUUID,
                    ByteBufCodecs.VAR_INT, KissStartPayload::durationTicks,
                    KissStartPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

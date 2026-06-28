package com.wally.kissmod.network;

import com.wally.kissmod.kissmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record KissExecutePacket(UUID kisserUUID, UUID targetUUID, int durationTicks) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(kissmod.MODID, "kiss_execute");
    public static final CustomPacketPayload.Type<KissExecutePacket> TYPE = new CustomPacketPayload.Type<>(ID);

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

    public static final StreamCodec<RegistryFriendlyByteBuf, KissExecutePacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUID_STREAM_CODEC, KissExecutePacket::kisserUUID,
                    UUID_STREAM_CODEC, KissExecutePacket::targetUUID,
                    ByteBufCodecs.VAR_INT, KissExecutePacket::durationTicks,
                    KissExecutePacket::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

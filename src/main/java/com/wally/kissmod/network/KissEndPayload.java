package com.wally.kissmod.network;

import com.wally.kissmod.kissmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

public record KissEndPayload(UUID playerUUID, UUID partnerUUID) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(kissmod.MODID, "kiss_end");
    public static final CustomPacketPayload.Type<KissEndPayload> TYPE = new CustomPacketPayload.Type<>(ID);

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

    public static final StreamCodec<RegistryFriendlyByteBuf, KissEndPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUID_STREAM_CODEC, KissEndPayload::playerUUID,
                    ByteBufCodecs.optional(UUID_STREAM_CODEC), p -> Optional.ofNullable(p.partnerUUID()),
                    (uuid, optPartner) -> new KissEndPayload(uuid, optPartner.orElse(null))
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.wally.kissmod.network;

import com.wally.kissmod.kissmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record KissEndPayload(UUID playerUUID) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KissEndPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(kissmod.MODID, "kiss_end"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KissEndPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), KissEndPayload::playerUUID,
                    KissEndPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

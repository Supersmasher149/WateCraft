package com.wally.kissmod.network;

import com.wally.kissmod.kissmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record KissPromptPacket(UUID requesterUUID, String requesterName) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(kissmod.MODID, "kiss_prompt");
    public static final CustomPacketPayload.Type<KissPromptPacket> TYPE = new CustomPacketPayload.Type<>(ID);

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

    private static final StreamCodec<RegistryFriendlyByteBuf, String> STRING_STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public String decode(RegistryFriendlyByteBuf buf) {
                    return buf.readUtf();
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buf, String value) {
                    buf.writeUtf(value);
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, KissPromptPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUID_STREAM_CODEC, KissPromptPacket::requesterUUID,
                    STRING_STREAM_CODEC, KissPromptPacket::requesterName,
                    KissPromptPacket::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

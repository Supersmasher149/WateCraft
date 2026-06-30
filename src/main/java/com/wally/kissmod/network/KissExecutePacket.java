package com.wally.kissmod.network;

import com.wally.kissmod.kissmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record KissExecutePacket(UUID kisserUUID, UUID targetUUID, int durationTicks,
                                Vec3 kisserHugPos, Vec3 targetHugPos,
                                Vec3 originalPosKisser, Vec3 originalPosTarget) implements CustomPacketPayload {
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

    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public Vec3 decode(RegistryFriendlyByteBuf buf) {
                    if (!buf.readBoolean()) return null;
                    return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buf, Vec3 value) {
                    buf.writeBoolean(value != null);
                    if (value != null) {
                        buf.writeDouble(value.x);
                        buf.writeDouble(value.y);
                        buf.writeDouble(value.z);
                    }
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, KissExecutePacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public KissExecutePacket decode(RegistryFriendlyByteBuf buf) {
                    return new KissExecutePacket(
                            buf.readUUID(),
                            buf.readUUID(),
                            buf.readVarInt(),
                            VEC3_STREAM_CODEC.decode(buf),
                            VEC3_STREAM_CODEC.decode(buf),
                            VEC3_STREAM_CODEC.decode(buf),
                            VEC3_STREAM_CODEC.decode(buf)
                    );
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buf, KissExecutePacket packet) {
                    buf.writeUUID(packet.kisserUUID());
                    buf.writeUUID(packet.targetUUID());
                    buf.writeVarInt(packet.durationTicks());
                    VEC3_STREAM_CODEC.encode(buf, packet.kisserHugPos());
                    VEC3_STREAM_CODEC.encode(buf, packet.targetHugPos());
                    VEC3_STREAM_CODEC.encode(buf, packet.originalPosKisser());
                    VEC3_STREAM_CODEC.encode(buf, packet.originalPosTarget());
                }
            };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

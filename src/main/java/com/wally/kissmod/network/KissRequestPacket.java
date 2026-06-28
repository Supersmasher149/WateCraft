package com.wally.kissmod.network;

import com.wally.kissmod.Config;
import com.wally.kissmod.KissDetectionHandler;
import com.wally.kissmod.KissLog;
import com.wally.kissmod.KissPlayerData;
import com.wally.kissmod.ModAttachments;
import com.wally.kissmod.RequestManager;
import com.wally.kissmod.kissmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record KissRequestPacket(UUID targetUUID) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(kissmod.MODID, "kiss_request");
    public static final CustomPacketPayload.Type<KissRequestPacket> TYPE = new CustomPacketPayload.Type<>(ID);

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

    public static final StreamCodec<RegistryFriendlyByteBuf, KissRequestPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUID_STREAM_CODEC, KissRequestPacket::targetUUID,
                    KissRequestPacket::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KissRequestPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (player.getServer() == null) return;

            if (player.isFallFlying() || player.isSleeping() || player.isPassenger()) return;
            if (RequestManager.isOnCooldown(player.getUUID())) return;

            KissPlayerData data = player.getData(ModAttachments.kissData());
            if (data.isKissing()) return;
            if (data.isOptedOut()) return;
            if (data.getCooldownTicks() > 0) return;

            UUID targetUUID = payload.targetUUID();
            if (player.getUUID().equals(targetUUID)) return;

            ServerPlayer target = player.getServer().getPlayerList().getPlayer(targetUUID);
            if (target == null) return;

            double maxDist = KissDetectionHandler.effectiveMaxDistance();
            if (player.getEyePosition().distanceToSqr(target.getEyePosition()) > maxDist * maxDist) return;

            if (target.isFallFlying() || target.isSleeping() || target.isPassenger()) return;

            KissPlayerData targetData = target.getData(ModAttachments.kissData());
            if (targetData.isKissing()) return;
            if (targetData.isOptedOut()) return;
            if (targetData.getCooldownTicks() > 0) return;
            if (RequestManager.hasPendingRequest(targetUUID)) return;

            RequestManager.createRequest(player.getUUID(), targetUUID);
            KissLog.networking("request created: {} -> {}", player.getName().getString(), target.getName().getString());

            PacketDistributor.sendToPlayer(target, new KissPromptPacket(player.getUUID(), player.getName().getString()));
        });
    }
}

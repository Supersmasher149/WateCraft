package com.wally.kissmod.network;

import com.wally.kissmod.Config;
import com.wally.kissmod.KissDetectionHandler;
import com.wally.kissmod.KissLog;
import com.wally.kissmod.KissPlayerData;
import com.wally.kissmod.ModAttachments;
import com.wally.kissmod.RequestManager;
import com.wally.kissmod.kissmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record KissResponsePacket(UUID requesterUUID, boolean accepted) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(kissmod.MODID, "kiss_response");
    public static final CustomPacketPayload.Type<KissResponsePacket> TYPE = new CustomPacketPayload.Type<>(ID);

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

    public static final StreamCodec<RegistryFriendlyByteBuf, KissResponsePacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUID_STREAM_CODEC, KissResponsePacket::requesterUUID,
                    ByteBufCodecs.BOOL, KissResponsePacket::accepted,
                    KissResponsePacket::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KissResponsePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (player.getServer() == null) return;

            UUID requesterUUID = payload.requesterUUID();

            if (!RequestManager.hasPendingRequest(player.getUUID())) return;
            if (!requesterUUID.equals(RequestManager.getRequester(player.getUUID()))) return;

            RequestManager.removeRequest(player.getUUID());

            if (!payload.accepted()) {
                RequestManager.setCooldown(player.getUUID());
                KissLog.networking("request declined: {} declined requester {}", player.getName().getString(), requesterUUID);
                return;
            }

            ServerPlayer requester = player.getServer().getPlayerList().getPlayer(requesterUUID);
            if (requester == null) return;

            if (!player.isAlive() || !requester.isAlive()) return;

            if (player.isFallFlying() || player.isSleeping() || player.isPassenger()) return;
            if (requester.isFallFlying() || requester.isSleeping() || requester.isPassenger()) return;

            double maxDist = KissDetectionHandler.effectiveMaxDistance();
            double dist = player.getEyePosition().distanceToSqr(requester.getEyePosition());
            if (dist > maxDist * maxDist) return;

            if (!KissDetectionHandler.hasLineOfSight(requester, player)) return;

            if (Config.REQUIRE_LOOK.get()) {
                Vec3 lookA = requester.getLookAngle();
                Vec3 lookB = player.getLookAngle();
                Vec3 aToB = player.getEyePosition().subtract(requester.getEyePosition()).normalize();
                Vec3 bToA = requester.getEyePosition().subtract(player.getEyePosition()).normalize();
                double dotA = lookA.dot(aToB);
                double dotB = lookB.dot(bToA);
                if (dotA <= 0.8 || dotB <= 0.8) return;
            }

            KissPlayerData requesterData = requester.getData(ModAttachments.kissData());
            if (requesterData.isKissing()) return;
            if (requesterData.isOptedOut()) return;

            KissPlayerData targetData = player.getData(ModAttachments.kissData());
            if (targetData.isKissing()) return;

            KissDetectionHandler.startKiss(requester, player);
        });
    }
}

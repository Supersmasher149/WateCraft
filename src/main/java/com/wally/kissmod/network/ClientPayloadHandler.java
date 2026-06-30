package com.wally.kissmod.network;

import com.wally.kissmod.KissPlayerData;
import com.wally.kissmod.KissPromptOverlay;
import com.wally.kissmod.ModAttachments;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class ClientPayloadHandler {
    public static void handleKissPrompt(final KissPromptPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.player().level().isClientSide()) return;
            KissPromptOverlay.showPrompt(payload.requesterUUID(), payload.requesterName());
        });
    }

    public static void handleKissExecute(final KissExecutePacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.player().level().isClientSide()) return;
            var level = Minecraft.getInstance().level;
            if (level == null) return;

            var kisser = level.getPlayerByUUID(payload.kisserUUID());
            var target = level.getPlayerByUUID(payload.targetUUID());

            if (kisser != null) {
                KissPlayerData data = kisser.getData(ModAttachments.kissData());
                data.setKissing(true);
                data.setTargetUUID(payload.targetUUID());
                data.setRemainingKissTicks(payload.durationTicks());
                data.setOriginalPos(payload.originalPosKisser());
                data.setHugPos(payload.kisserHugPos());
            }
            if (target != null) {
                KissPlayerData data = target.getData(ModAttachments.kissData());
                data.setKissing(true);
                data.setTargetUUID(payload.kisserUUID());
                data.setRemainingKissTicks(payload.durationTicks());
                data.setOriginalPos(payload.originalPosTarget());
                data.setHugPos(payload.targetHugPos());
            }
        });
    }

    public static void handleKissEnd(final KissEndPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.player().level().isClientSide()) return;
            var level = Minecraft.getInstance().level;
            if (level == null) return;

            clearKissState(level, payload.playerUUID());
            if (payload.partnerUUID() != null) {
                clearKissState(level, payload.partnerUUID());
            }
        });
    }

    private static void clearKissState(net.minecraft.client.multiplayer.ClientLevel level, UUID uuid) {
        var player = level.getPlayerByUUID(uuid);
        if (player != null) {
            KissPlayerData data = player.getData(ModAttachments.kissData());
            data.setKissing(false);
            data.setTargetUUID(null);
            data.setRemainingKissTicks(0);
        }
    }
}

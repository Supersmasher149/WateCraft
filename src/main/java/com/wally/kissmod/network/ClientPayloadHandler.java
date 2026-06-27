package com.wally.kissmod.network;

import com.wally.kissmod.KissPlayerData;
import com.wally.kissmod.ModAttachments;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleKissStart(final KissStartPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.player().level().isClientSide()) return;
            var player = Minecraft.getInstance().level.getPlayerByUUID(payload.playerUUID());
            if (player != null) {
                KissPlayerData data = player.getData(ModAttachments.kissData());
                data.setKissing(true);
                data.setTargetUUID(payload.targetUUID());
            }
        });
    }

    public static void handleKissEnd(final KissEndPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.player().level().isClientSide()) return;
            var player = Minecraft.getInstance().level.getPlayerByUUID(payload.playerUUID());
            if (player != null) {
                KissPlayerData data = player.getData(ModAttachments.kissData());
                data.setKissing(false);
                data.setTargetUUID(null);
            }
        });
    }
}

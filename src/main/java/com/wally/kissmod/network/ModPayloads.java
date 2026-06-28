package com.wally.kissmod.network;

import com.wally.kissmod.kissmod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPayloads {
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(kissmod.MODID).versioned("1.0");

        registrar.playToServer(
                KissRequestPacket.TYPE, KissRequestPacket.STREAM_CODEC,
                KissRequestPacket::handle
        );

        registrar.playToServer(
                KissResponsePacket.TYPE, KissResponsePacket.STREAM_CODEC,
                KissResponsePacket::handle
        );

        registrar.playToClient(
                KissPromptPacket.TYPE, KissPromptPacket.STREAM_CODEC,
                ClientPayloadHandler::handleKissPrompt
        );

        registrar.playToClient(
                KissExecutePacket.TYPE, KissExecutePacket.STREAM_CODEC,
                ClientPayloadHandler::handleKissExecute
        );

        registrar.playToClient(
                KissEndPayload.TYPE, KissEndPayload.STREAM_CODEC,
                ClientPayloadHandler::handleKissEnd
        );
    }
}

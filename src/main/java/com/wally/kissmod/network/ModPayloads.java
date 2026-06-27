package com.wally.kissmod.network;

import com.wally.kissmod.kissmod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPayloads {
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(kissmod.MODID).versioned("1.0");

        registrar.playToClient(
                KissStartPayload.TYPE, KissStartPayload.STREAM_CODEC,
                ClientPayloadHandler::handleKissStart
        );

        registrar.playToClient(
                KissEndPayload.TYPE, KissEndPayload.STREAM_CODEC,
                ClientPayloadHandler::handleKissEnd
        );
    }
}

package com.wally.kissmod;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = kissmod.MODID, dist = Dist.CLIENT)
public class kissmodClient {
    public kissmodClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(DebugRenderer::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(kissmodClient::onClientTick);
    }

    private static void onClientTick(final ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (var player : level.players()) {
            KissPlayerData data = player.getData(ModAttachments.kissData());
            if (!data.isKissing()) continue;
            if (data.getRemainingKissTicks() <= 0) continue;
            data.setRemainingKissTicks(data.getRemainingKissTicks() - 1);
        }
    }
}

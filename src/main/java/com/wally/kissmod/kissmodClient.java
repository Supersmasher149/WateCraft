package com.wally.kissmod;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = kissmod.MODID, dist = Dist.CLIENT)
public class kissmodClient {
    public kissmodClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        container.getEventBus().addListener(KeybindHandler::register);

        container.getEventBus().addListener((RegisterGuiLayersEvent event) -> {
            event.registerAboveAll(
                    ResourceLocation.fromNamespaceAndPath(kissmod.MODID, "kiss_prompt"),
                    KissPromptOverlay::render
            );
        });

        NeoForge.EVENT_BUS.addListener(KeybindHandler::onClientTick);
        NeoForge.EVENT_BUS.addListener(KissPromptOverlay::onKeyInput);
        NeoForge.EVENT_BUS.addListener(DebugRenderer::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(kissmodClient::onClientTick);
    }

    private static void onClientTick(final ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        while (KeybindHandler.STATS_KEY.consumeClick()) {
            if (mc.player == null) continue;
            if (mc.screen instanceof KissStatsScreen) {
                mc.setScreen(null);
            } else if (mc.screen == null) {
                mc.setScreen(new KissStatsScreen());
            }
        }

        var level = mc.level;
        if (level == null) return;
        if (mc.player == null || mc.player.tickCount % 5 != 0) return;
        for (var player : level.players()) {
            KissPlayerData data = player.getData(ModAttachments.kissData());
            if (!data.isKissing()) continue;
            if (data.getRemainingKissTicks() <= 0) continue;
            data.setRemainingKissTicks(data.getRemainingKissTicks() - 5);
            if (data.getRemainingKissTicks() < 0) data.setRemainingKissTicks(0);
        }
    }
}

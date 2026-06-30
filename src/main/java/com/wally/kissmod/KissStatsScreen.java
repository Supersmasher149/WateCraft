package com.wally.kissmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class KissStatsScreen extends Screen {
    private static final Component TITLE = Component.translatable("kissmod.stats_screen.title");

    public KissStatsScreen() {
        super(TITLE);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int totalKisses = mc.player.getData(ModAttachments.kissData()).getTotalKisses();
        String countText = "Kisses: " + totalKisses;

        int centerX = width / 2;
        int centerY = height / 2;

        guiGraphics.drawString(font, "\u2764", centerX - font.width("\u2764") / 2, centerY - 20, 0xFF5555, false);

        guiGraphics.drawString(font, countText, centerX - font.width(countText) / 2, centerY, 0xFFFFFF, false);
    }
}

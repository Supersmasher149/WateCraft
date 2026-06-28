package com.wally.kissmod;

import com.wally.kissmod.network.KissResponsePacket;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class KissPromptOverlay {
    private static boolean visible = false;
    private static UUID requesterUUID;
    private static String requesterName;
    private static long promptStartTime;

    public static void showPrompt(UUID uuid, String name) {
        visible = true;
        requesterUUID = uuid;
        requesterName = name;
        promptStartTime = System.currentTimeMillis();
    }

    private static void hidePrompt() {
        visible = false;
        requesterUUID = null;
        requesterName = null;
    }

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!visible) return;

        if (System.currentTimeMillis() - promptStartTime > 5000) {
            autoDecline();
            return;
        }

        var window = Minecraft.getInstance().getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        int panelWidth = 200;
        int panelHeight = 40;
        int x = (screenWidth - panelWidth) / 2;
        int y = screenHeight - 80;

        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, 0x88000000);

        var font = Minecraft.getInstance().font;
        String line1 = requesterName + " wants to kiss you!";
        String line2 = "[Y] Accept  [N] Decline";

        guiGraphics.drawString(font, line1, x + 10, y + 8, 0xFFFFFF, true);
        guiGraphics.drawString(font, line2, x + 10, y + 24, 0xCCCCCC, true);
    }

    public static void onKeyInput(InputEvent.Key event) {
        if (!visible) return;
        if (Minecraft.getInstance().screen != null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        int key = event.getKey();
        if (key == GLFW.GLFW_KEY_Y) {
            accept();
        } else if (key == GLFW.GLFW_KEY_N) {
            decline();
        }
    }

    private static void accept() {
        if (!visible) return;
        visible = false;
        PacketDistributor.sendToServer(new KissResponsePacket(requesterUUID, true));
        hidePrompt();
    }

    private static void decline() {
        if (!visible) return;
        visible = false;
        PacketDistributor.sendToServer(new KissResponsePacket(requesterUUID, false));
        hidePrompt();
    }

    private static void autoDecline() {
        if (!visible) return;
        visible = false;
        PacketDistributor.sendToServer(new KissResponsePacket(requesterUUID, false));
        hidePrompt();
    }
}

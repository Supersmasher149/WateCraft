package com.wally.kissmod;

import com.mojang.blaze3d.platform.InputConstants;
import com.wally.kissmod.network.KissRequestPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {
    public static final String KEY_CATEGORY = "key.categories.kissmod";
    public static final String KEY_KISS = "key.kissmod.kiss";
    public static final String KEY_STATS = "key.kissmod.stats";

    public static final KeyMapping KISS_KEY = new KeyMapping(
            KEY_KISS,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY
    );

    public static final KeyMapping STATS_KEY = new KeyMapping(
            KEY_STATS,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            KEY_CATEGORY
    );

    private static long lastPressTime = 0;
    private static final long RATE_LIMIT_MS = 500;

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(KISS_KEY);
        event.register(STATS_KEY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        while (KISS_KEY.consumeClick()) {
            long now = System.currentTimeMillis();
            if (now - lastPressTime < RATE_LIMIT_MS) continue;
            lastPressTime = now;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            if (mc.screen != null) return;

            Player target = raycastPlayer(mc);
            if (target != null) {
                PacketDistributor.sendToServer(new KissRequestPacket(target.getUUID()));
            }
        }
    }

    private static Player raycastPlayer(Minecraft mc) {
        Entity camera = mc.getCameraEntity();
        if (camera == null) camera = mc.player;
        if (camera == null) return null;

        double maxDist = KissDetectionHandler.effectiveMaxDistance();
        Vec3 eyePos = camera.getEyePosition();
        Vec3 lookVec = camera.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(maxDist));

        AABB searchBox = camera.getBoundingBox()
                .expandTowards(lookVec.scale(maxDist))
                .inflate(1.0);

        EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                mc.level,
                camera,
                eyePos,
                endPos,
                searchBox,
                e -> e instanceof Player && e != mc.player
        );

        if (hit != null) {
            return (Player) hit.getEntity();
        }

        return null;
    }
}

package com.wally.kissmod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Comparator;

public final class DebugRenderer {
    private DebugRenderer() {
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!Config.DEBUG_MODE.get()) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        Player player = minecraft.player;
        Player target = minecraft.level.players().stream()
                .filter(other -> other != player)
                .min(Comparator.comparingDouble(other -> other.distanceToSqr(player)))
                .orElse(null);

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
        Vec3 camera = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        Vec3 eye = player.getEyePosition(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        drawSmallBox(poseStack, lines, eye, 0.08D, 0.0F, 1.0F, 0.0F, 1.0F);
        drawLine(poseStack, lines, eye, eye.add(player.getLookAngle().scale(2.0D)), 0.0F, 1.0F, 0.0F, 1.0F);
        drawRadiusBox(poseStack, lines, eye, KissDetectionHandler.effectiveMaxDistance(), 1.0F, 1.0F, 0.0F, 0.55F);

        if (target != null) {
            Vec3 targetEye = target.getEyePosition(event.getPartialTick().getGameTimeDeltaPartialTick(false));
            drawSmallBox(poseStack, lines, targetEye, 0.08D, 1.0F, 0.0F, 0.0F, 1.0F);
            drawLine(poseStack, lines, targetEye, targetEye.add(target.getLookAngle().scale(2.0D)), 1.0F, 0.0F, 0.0F, 1.0F);
            drawLine(poseStack, lines, eye, targetEye, 1.0F, 1.0F, 1.0F, 0.9F);
            LevelRenderer.renderLineBox(poseStack, lines, target.getBoundingBox(), 0.0F, 0.35F, 1.0F, 1.0F);
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }

    private static void drawSmallBox(PoseStack poseStack, VertexConsumer consumer, Vec3 center, double size, float r, float g, float b, float a) {
        LevelRenderer.renderLineBox(poseStack, consumer, new AABB(
                center.x - size, center.y - size, center.z - size,
                center.x + size, center.y + size, center.z + size), r, g, b, a);
    }

    private static void drawRadiusBox(PoseStack poseStack, VertexConsumer consumer, Vec3 center, double radius, float r, float g, float b, float a) {
        LevelRenderer.renderLineBox(poseStack, consumer, new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius), r, g, b, a);
    }

    private static void drawLine(PoseStack poseStack, VertexConsumer consumer, Vec3 from, Vec3 to, float r, float g, float b, float a) {
        var matrix = poseStack.last().pose();
        consumer.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z).setColor(r, g, b, a).setNormal(0.0F, 1.0F, 0.0F);
    }
}

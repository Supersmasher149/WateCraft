package com.wally.kissmod;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class KissAnimator {
    private static final float ARM_WRAP_X = -1.2F;
    private static final float ARM_WRAP_Z = 0.2F;
    private static final float BODY_LEAN = 0.12F;
    private static final float HEAD_TILT_DOWN = 0.10F;
    private static final float HEAD_Z_TILT_MAX = 0.16F;

    public static void apply(PlayerModel<?> model, LivingEntity entity, KissPlayerData data, float ageInTicks) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        if (Config.DISABLE_HEAD_TILT.get()) return;
        if (!data.isKissing()) return;
        if (data.getKissPhase() == KissPhase.NONE) return;

        int duration = Config.ANIMATION_DURATION_TICKS.get();
        int remaining = data.getRemainingKissTicks();

        KissPhase phase = KissPlayerData.inferPhase(remaining, duration);
        float phaseProgress;
        switch (phase) {
            case ENTERING -> {
                int elapsed = duration - remaining;
                phaseProgress = Math.min(1.0F, (float) elapsed / KissPlayerData.ENTER_TICKS);
            }
            case EXITING -> {
                phaseProgress = Math.min(1.0F, 1.0F - (float) remaining / KissPlayerData.EXIT_TICKS);
            }
            default -> phaseProgress = 1.0F;
        }
        float intensity = easeInOut(phaseProgress);

        AbstractClientPlayer partner = findPartner(player, data.getTargetUUID());
        if (partner == null) return;

        Vec3 toPartner = partner.position().subtract(player.position()).normalize();
        double angleToPartner = Math.atan2(toPartner.x, toPartner.z);

        model.head.yRot += (float) angleToPartner * intensity * 0.3F;
        model.head.xRot += HEAD_TILT_DOWN * intensity;
        float headZTilt = (float) (-toPartner.x * HEAD_Z_TILT_MAX * intensity);
        model.head.zRot += headZTilt;

        model.body.xRot += BODY_LEAN * intensity;
        model.body.yRot += (float) angleToPartner * intensity * 0.15F;
        model.body.zRot += headZTilt * 0.4F;

        model.leftArm.xRot += ARM_WRAP_X * intensity;
        model.leftArm.zRot += -ARM_WRAP_Z * intensity;
        model.rightArm.xRot += ARM_WRAP_X * intensity;
        model.rightArm.zRot += ARM_WRAP_Z * intensity;
    }

    private static AbstractClientPlayer findPartner(AbstractClientPlayer player, UUID targetUUID) {
        if (targetUUID == null) return null;
        var level = player.level();
        if (level == null) return null;
        var entity = level.getPlayerByUUID(targetUUID);
        if (entity instanceof AbstractClientPlayer partner) return partner;
        return null;
    }

    private static float easeInOut(float t) {
        return t * t * (3 - 2 * t);
    }
}

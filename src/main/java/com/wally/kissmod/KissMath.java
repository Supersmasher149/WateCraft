package com.wally.kissmod;

import net.minecraft.world.phys.Vec3;

public final class KissMath {
    private KissMath() {
    }

    public static double distanceSqr(Vec3 a, Vec3 b) {
        return a.distanceToSqr(b);
    }

    public static boolean isWithinDistance(Vec3 a, Vec3 b, double maxDistance) {
        return distanceSqr(a, b) <= maxDistance * maxDistance;
    }

    public static double lookDot(Vec3 look, Vec3 from, Vec3 to) {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() == 0.0D) return 1.0D;
        return look.normalize().dot(direction.normalize());
    }

    public static boolean isLookingAt(Vec3 look, Vec3 from, Vec3 to, double threshold) {
        return lookDot(look, from, to) > threshold;
    }
}

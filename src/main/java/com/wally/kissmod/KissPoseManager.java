package com.wally.kissmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class KissPoseManager {

    public static final double HUG_DISTANCE = 0.4;

    public static Vec3[] computeHugPositions(Vec3 posA, Vec3 posB) {
        Vec3 midpoint = posA.add(posB).scale(0.5);
        Vec3 dir = new Vec3(posB.x - posA.x, 0, posB.z - posA.z).normalize();
        if (dir.lengthSqr() < 1e-7) {
            dir = new Vec3(0, 0, 1);
        }
        Vec3 hugA = midpoint.subtract(dir.scale(HUG_DISTANCE * 0.5));
        Vec3 hugB = midpoint.add(dir.scale(HUG_DISTANCE * 0.5));
        return new Vec3[]{hugA, hugB};
    }

    public static Vec3[] computeHugPositions(ServerPlayer a, ServerPlayer b) {
        return computeHugPositions(a.position(), b.position());
    }

    public static void tickPosition(ServerPlayer player, KissPlayerData data) {
        if (data.getOriginalPos() == null || data.getHugPos() == null) return;

        switch (data.getKissPhase()) {
            case ENTERING -> {
                data.setKissPhaseTicks(data.getKissPhaseTicks() + 1);
                float progress = Math.min(1.0F, (float) data.getKissPhaseTicks() / KissPlayerData.ENTER_TICKS);
                Vec3 pos = lerp(data.getOriginalPos(), data.getHugPos(), easeInOut(progress));
                teleport(player, pos);
                if (progress >= 1.0F) {
                    data.setKissPhase(KissPhase.HOLDING);
                    data.setKissPhaseTicks(0);
                }
            }
            case HOLDING -> {
                teleport(player, data.getHugPos());
            }
            case EXITING -> {
                data.setKissPhaseTicks(data.getKissPhaseTicks() + 1);
                float progress = Math.min(1.0F, (float) data.getKissPhaseTicks() / KissPlayerData.EXIT_TICKS);
                Vec3 pos = lerp(data.getHugPos(), data.getOriginalPos(), easeInOut(progress));
                teleport(player, pos);
            }
        }
    }

    private static void teleport(ServerPlayer player, Vec3 pos) {
        player.connection.teleport(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
    }

    private static Vec3 lerp(Vec3 from, Vec3 to, float t) {
        return new Vec3(
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t,
                from.z + (to.z - from.z) * t
        );
    }

    private static float easeInOut(float t) {
        return t * t * (3 - 2 * t);
    }
}

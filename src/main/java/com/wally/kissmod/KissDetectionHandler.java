package com.wally.kissmod;

import com.wally.kissmod.network.KissEndPayload;
import com.wally.kissmod.network.KissExecutePacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.UUID;

public class KissDetectionHandler {
    public static double effectiveMaxDistance() {
        double distance = Config.MAX_DISTANCE.get();
        return Config.DEBUG_MODE.get() ? distance * 3.0D : distance;
    }

    public static int effectiveCooldownTicks() {
        int cooldown = Config.COOLDOWN_SECONDS.get() * 20;
        return Config.DEBUG_MODE.get() ? Math.min(cooldown, 20) : cooldown;
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.tickCount % 5 != 0) return;
        if (player.isFallFlying() || player.isSleeping() || player.isPassenger()) return;

        KissPlayerData data = player.getData(ModAttachments.kissData());
        tickCooldown(data);

        if (data.isKissing()) {
            tickActiveKiss(player, data);
            return;
        }
        if (data.isOptedOut()) return;
        if (data.getCooldownTicks() > 0) return;

        double maxDist = effectiveMaxDistance();
        double maxDistSq = maxDist * maxDist;
        Vec3 eyePos = player.getEyePosition();
        Player target = player.level().players().stream()
                .filter(other -> other != player && other.getEyePosition().distanceToSqr(eyePos) <= maxDistSq)
                .min(Comparator.comparingDouble(other -> other.getEyePosition().distanceToSqr(eyePos)))
                .orElse(null);
        if (target == null) return;

        KissPlayerData targetData = target.getData(ModAttachments.kissData());
        if (targetData.isOptedOut()) return;
        if (targetData.isKissing() || targetData.getCooldownTicks() > 0) return;
        if (target.isFallFlying() || target.isSleeping() || target.isPassenger()) return;

        if (Config.REQUIRE_LOOK.get()) {
            Vec3 lookA = player.getLookAngle();
            Vec3 lookB = target.getLookAngle();
            Vec3 aToB = target.getEyePosition().subtract(eyePos).normalize();
            Vec3 bToA = eyePos.subtract(target.getEyePosition()).normalize();
            double dotA = lookA.dot(aToB);
            double dotB = lookB.dot(bToA);
            if (dotA <= 0.8 || dotB <= 0.8) return;
        }

        if (!hasLineOfSight(player, target)) return;

        startKiss(player, target);
    }

    private void tickCooldown(KissPlayerData data) {
        if (data.getCooldownTicks() > 0) {
            data.setCooldownTicks(data.getCooldownTicks() - 5);
            if (data.getCooldownTicks() < 0) data.setCooldownTicks(0);
        }
    }

    private void tickActiveKiss(Player player, KissPlayerData data) {
        UUID targetId = data.getTargetUUID();
        if (targetId == null) {
            endKiss(player);
            return;
        }

        if (data.getRemainingKissTicks() > 0) {
            data.setRemainingKissTicks(data.getRemainingKissTicks() - 5);
            if (data.getRemainingKissTicks() <= 0) {
                endKiss(player);
                return;
            }
        }

        Player target = player.level().getPlayerByUUID(targetId);
        if (target == null || !target.isAlive() || target.distanceToSqr(player) > 16.0) {
            endKiss(player);
            return;
        }
        KissPlayerData targetData = target.getData(ModAttachments.kissData());
        if (!targetData.isKissing() || !targetId.equals(targetData.getTargetUUID())) {
            endKiss(player);
            return;
        }
    }

    public static void startKiss(Player player, Player target) {
        int duration = Config.ANIMATION_DURATION_TICKS.get();

        KissPlayerData data = player.getData(ModAttachments.kissData());
        data.setKissing(true);
        data.setTargetUUID(target.getUUID());
        data.setRemainingKissTicks(duration);

        KissPlayerData targetData = target.getData(ModAttachments.kissData());
        targetData.setKissing(true);
        targetData.setTargetUUID(player.getUUID());
        targetData.setRemainingKissTicks(duration);

        data.setTotalKisses(data.getTotalKisses() + 1);
        targetData.setTotalKisses(targetData.getTotalKisses() + 1);

        var packet = new KissExecutePacket(player.getUUID(), target.getUUID(), duration);
        PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(), packet);

        Vec3 mid = player.getEyePosition().add(target.getEyePosition()).scale(0.5);
        if (Config.ENABLE_HEARTS.get()) {
            ServerLevel level = (ServerLevel) player.level();
            level.sendParticles(
                    ParticleTypes.HEART, mid.x, mid.y, mid.z, 6, 0.3, 0.3, 0.3, 0.0);
            double radius = 1.5;
            int count = 20;
            for (int i = 0; i < count; i++) {
                double angle = 2.0 * Math.PI * i / count;
                double x = mid.x + radius * Math.cos(angle);
                double z = mid.z + radius * Math.sin(angle);
                level.sendParticles(ParticleTypes.HEART, x, mid.y, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        player.level().playSound(null, mid.x, mid.y, mid.z,
                Sounds.KISS_SOUND.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    public static void endKiss(Player player) {
        KissPlayerData data = player.getData(ModAttachments.kissData());
        if (!data.isKissing()) return;

        UUID targetId = data.getTargetUUID();
        data.setKissing(false);
        data.setTargetUUID(null);
        data.setCooldownTicks(effectiveCooldownTicks());
        data.setRemainingKissTicks(0);

        RequestManager.setCooldown(player.getUUID());

        if (targetId != null) {
            Player target = player.level().getPlayerByUUID(targetId);
            if (target == null && player.getServer() != null) {
                target = player.getServer().getPlayerList().getPlayer(targetId);
            }
            if (target != null) {
                KissPlayerData targetData = target.getData(ModAttachments.kissData());
                targetData.setKissing(false);
                targetData.setTargetUUID(null);
                targetData.setCooldownTicks(effectiveCooldownTicks());
                targetData.setRemainingKissTicks(0);

                RequestManager.setCooldown(target.getUUID());

                if (target instanceof ServerPlayer spTarget) {
                    PacketDistributor.sendToPlayer(spTarget,
                            new KissEndPayload(spTarget.getUUID(), player.getUUID()));

                    if (Config.ENABLE_REGENERATION.get())
                        spTarget.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
                    if (Config.ENABLE_GLOWING.get())
                        spTarget.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                }
            }
        }

        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp,
                    new KissEndPayload(sp.getUUID(), targetId));

            if (Config.ENABLE_REGENERATION.get())
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            if (Config.ENABLE_GLOWING.get())
                sp.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        }
    }

    public static boolean hasLineOfSight(Player a, Player b) {
        Vec3 from = a.getEyePosition();
        Vec3 to = b.getEyePosition();
        Vec3 diff = to.subtract(from);
        double maxDist = effectiveMaxDistance();
        if (diff.lengthSqr() > maxDist * maxDist) return false;
        var hit = a.level().clip(new net.minecraft.world.level.ClipContext(
                from, to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, a));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) < 0.25;
    }

    @SubscribeEvent
    public void onPlayerDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            KissPlayerData data = player.getData(ModAttachments.kissData());
            if (data.isKissing()) endKiss(player);
        }
    }

    @SubscribeEvent
    public void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            KissPlayerData data = player.getData(ModAttachments.kissData());
            if (data.isKissing()) {
                UUID targetId = data.getTargetUUID();
                endKiss(player);
                if (targetId != null && player.getServer() != null) {
                    Player partner = player.getServer().getPlayerList().getPlayer(targetId);
                    if (partner != null) {
                        KissPlayerData partnerData = partner.getData(ModAttachments.kissData());
                        if (partnerData.isKissing()) {
                            endKiss(partner);
                        }
                    }
                }
            }
            RequestManager.removeAllForPlayer(player.getUUID());
        }
    }
}

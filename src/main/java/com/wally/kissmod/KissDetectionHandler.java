package com.wally.kissmod;

import com.wally.kissmod.network.KissEndPayload;
import com.wally.kissmod.network.KissStartPayload;
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

import java.util.UUID;

public class KissDetectionHandler {
    private static final double LOOK_THRESHOLD = 0.8;
    private static final double RAYCAST_RANGE = 3.0;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        kissmod.LOGGER.info("[kiss] tick for {}", player.getName().getString());
        if (player.level().isClientSide()) return;
        kissmod.LOGGER.info("[kiss] isServer, tickCount={}", player.tickCount);
        if (player.tickCount % 5 != 0) return;
        if (player.isFallFlying() || player.isSleeping() || player.isPassenger()) {
            kissmod.LOGGER.info("[kiss] blocked by state (flying={}, sleeping={}, passenger={})",
                    player.isFallFlying(), player.isSleeping(), player.isPassenger());
            return;
        }

        KissPlayerData data = player.getData(ModAttachments.kissData());

        tickCooldown(data);

        if (data.isKissing()) {
            kissmod.LOGGER.info("[kiss] already kissing, ticking active kiss");
            tickActiveKiss(player, data);
            return;
        }
        if (data.getCooldownTicks() > 0) {
            kissmod.LOGGER.info("[kiss] on cooldown for {} more ticks", data.getCooldownTicks());
            return;
        }

        Player target = player.level().getNearestPlayer(player, 2.0D);
        if (target == null) {
            kissmod.LOGGER.info("[kiss] no nearby player found");
            return;
        }
        kissmod.LOGGER.info("[kiss] found nearby player {}", target.getName().getString());

        KissPlayerData targetData = target.getData(ModAttachments.kissData());
        if (targetData.isKissing() || targetData.getCooldownTicks() > 0) {
            kissmod.LOGGER.info("[kiss] target is kissing={} or cooldown={}", targetData.isKissing(), targetData.getCooldownTicks());
            return;
        }
        if (target.isFallFlying() || target.isSleeping() || target.isPassenger()) {
            kissmod.LOGGER.info("[kiss] target blocked by state");
            return;
        }

        Vec3 eyeA = player.getEyePosition();
        Vec3 eyeB = target.getEyePosition();
        double maxDistSq = Config.MAX_DISTANCE.get() * Config.MAX_DISTANCE.get();
        double dist = eyeA.distanceToSqr(eyeB);
        kissmod.LOGGER.info("[kiss] eye distance={}, maxDistSq={}", dist, maxDistSq);
        if (dist > maxDistSq) {
            kissmod.LOGGER.info("[kiss] too far apart");
            return;
        }

        if (Config.REQUIRE_LOOK.get()) {
            Vec3 lookA = player.getLookAngle();
            Vec3 lookB = target.getLookAngle();
            Vec3 aToB = eyeB.subtract(eyeA).normalize();
            Vec3 bToA = eyeA.subtract(eyeB).normalize();
            double dotA = lookA.dot(aToB);
            double dotB = lookB.dot(bToA);
            kissmod.LOGGER.info("[kiss] look dot products: player={}, target={}, threshold={}", dotA, dotB, LOOK_THRESHOLD);
            if (dotA <= LOOK_THRESHOLD || dotB <= LOOK_THRESHOLD) {
                kissmod.LOGGER.info("[kiss] not looking at each other");
                return;
            }
        }

        if (!hasLineOfSight(player, target)) {
            kissmod.LOGGER.info("[kiss] no line of sight");
            return;
        }

        kissmod.LOGGER.info("[kiss] ALL CHECKS PASSED! Starting kiss!");
        startKiss(player, target);
    }

    private void tickCooldown(KissPlayerData data) {
        if (data.getCooldownTicks() > 0) {
            data.setCooldownTicks(data.getCooldownTicks() - 1);
            if (data.getCooldownTicks() < 0) data.setCooldownTicks(0);
        }
    }

    private void tickActiveKiss(Player player, KissPlayerData data) {
        UUID targetId = data.getTargetUUID();
        if (targetId == null) {
            endKiss(player);
            return;
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

    private void startKiss(Player player, Player target) {
        int duration = Config.ANIMATION_DURATION_TICKS.get();

        KissPlayerData data = player.getData(ModAttachments.kissData());
        data.setKissing(true);
        data.setTargetUUID(target.getUUID());

        KissPlayerData targetData = target.getData(ModAttachments.kissData());
        targetData.setKissing(true);
        targetData.setTargetUUID(player.getUUID());

        var packet = new KissStartPayload(player.getUUID(), target.getUUID(), duration);
        PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(), packet);

        Vec3 mid = player.getEyePosition().add(target.getEyePosition()).scale(0.5);
        if (Config.ENABLE_HEARTS.get()) {
            ((ServerLevel) player.level()).sendParticles(
                    ParticleTypes.HEART, mid.x, mid.y, mid.z, 6, 0.3, 0.3, 0.3, 0.0);
        }

        player.level().playSound(null, mid.x, mid.y, mid.z,
                Sounds.KISS_SOUND.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    private void endKiss(Player player) {
        KissPlayerData data = player.getData(ModAttachments.kissData());
        data.setKissing(false);
        UUID targetId = data.getTargetUUID();
        data.setTargetUUID(null);
        data.setCooldownTicks(Config.COOLDOWN_SECONDS.get() * 20);

        if (targetId != null) {
            Player target = player.level().getPlayerByUUID(targetId);
            if (target != null) {
                KissPlayerData targetData = target.getData(ModAttachments.kissData());
                targetData.setKissing(false);
                targetData.setTargetUUID(null);
                targetData.setCooldownTicks(Config.COOLDOWN_SECONDS.get() * 20);

                if (target instanceof ServerPlayer spTarget) {
                    PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(),
                            new KissEndPayload(spTarget.getUUID()));
                }
            }
        }

        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(),
                    new KissEndPayload(sp.getUUID()));

            if (Config.ENABLE_REGENERATION.get()) {
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            }
            if (Config.ENABLE_GLOWING.get()) {
                sp.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            }
        }
    }

    private boolean hasLineOfSight(Player a, Player b) {
        Vec3 from = a.getEyePosition();
        Vec3 to = b.getEyePosition();
        Vec3 diff = to.subtract(from);
        if (diff.lengthSqr() > RAYCAST_RANGE * RAYCAST_RANGE) return false;
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
            if (data.isKissing()) endKiss(player);
        }
    }
}

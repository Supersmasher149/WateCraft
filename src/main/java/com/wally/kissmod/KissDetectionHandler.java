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
    private static final double LOOK_THRESHOLD = 0.8; // Threshold for players to be looking at each other

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
        KissLog.detection("tick for {}", player.getName().getString());
        if (player.level().isClientSide()) return; // If on client side, skip processing
        KissLog.detection("isServer, tickCount={}", player.tickCount);
        if (player.tickCount % 5 != 0) return; // Only process every 5th tick to reduce load
        if (player.isFallFlying() || player.isSleeping() || player.isPassenger()) {
            KissLog.detection("blocked by state (flying={}, sleeping={}, passenger={})",
                    player.isFallFlying(), player.isSleeping(), player.isPassenger()); // Log if the player is in a blocking state
            return;
        }

        KissPlayerData data = player.getData(ModAttachments.kissData()); // Get the player's KissPlayerData

        tickCooldown(data); // Decrement the cooldown ticks for this player

        if (data.isKissing()) {
            KissLog.detection("already kissing, ticking active kiss");
            tickActiveKiss(player, data); // Continue the ongoing kiss interaction
            return;
        }
        if (data.isOptedOut()) return; // Player has opted out of starting kisses
        if (data.getCooldownTicks() > 0) {
            KissLog.detection("on cooldown for {} more ticks", data.getCooldownTicks());
            return;
        }

        double maxDist = effectiveMaxDistance();
        double maxDistSq = maxDist * maxDist;
        Vec3 eyePos = player.getEyePosition();
        Player target = player.level().players().stream()
                .filter(other -> other != player && other.getEyePosition().distanceToSqr(eyePos) <= maxDistSq)
                .min(java.util.Comparator.comparingDouble(other -> other.getEyePosition().distanceToSqr(eyePos)))
                .orElse(null);
        if (target == null) {
            KissLog.detection("no nearby player found");
            return;
        }
        KissLog.detection("found nearby player {}", target.getName().getString());

        KissPlayerData targetData = target.getData(ModAttachments.kissData()); // Get the target player's KissPlayerData
        if (targetData.isOptedOut()) {
            return; // Target has opted out of kissing
        }
        if (targetData.isKissing() || targetData.getCooldownTicks() > 0) {
            KissLog.detection("target is kissing={} or cooldown={}", targetData.isKissing(), targetData.getCooldownTicks());
            return;
        }
        if (target.isFallFlying() || target.isSleeping() || target.isPassenger()) {
            KissLog.detection("target blocked by state");
            return;
        }

        Vec3 eyeB = target.getEyePosition(); // Get the eye position of the target
        double dist = eyePos.distanceToSqr(eyeB); // Calculate the squared distance between players
        KissLog.detection("eye distance={}, maxDistSq={}", dist, maxDistSq);
        if (dist > maxDistSq) {
            KissLog.detection("too far apart");
            return;
        }

        if (Config.REQUIRE_LOOK.get()) {
            Vec3 lookA = player.getLookAngle(); // Get the player's look direction
            Vec3 lookB = target.getLookAngle(); // Get the target's look direction
            Vec3 aToB = eyeB.subtract(eyePos).normalize(); // Calculate the normalized vector from player to target
            Vec3 bToA = eyePos.subtract(eyeB).normalize(); // Calculate the normalized vector from target to player
            double dotA = lookA.dot(aToB); // Calculate the dot product between player's look direction and aToB
            double dotB = lookB.dot(bToA); // Calculate the dot product between target's look direction and bToA
            KissLog.detection("look dot products: player={}, target={}, threshold={}", dotA, dotB, LOOK_THRESHOLD);
            if (dotA <= LOOK_THRESHOLD || dotB <= LOOK_THRESHOLD) {
                KissLog.detection("not looking at each other");
                return;
            }
        }

        if (!hasLineOfSight(player, target)) {
            KissLog.detection("no line of sight");
            return;
        }

        kissmod.LOGGER.info(KissLog.DETECTION, "[KissCraft][Detection] starting kiss: {} -> {}", player.getName().getString(), target.getName().getString());
        startKiss(player, target); // Start the kiss interaction
    }

    private void tickCooldown(KissPlayerData data) {
        if (data.getCooldownTicks() > 0) {
            data.setCooldownTicks(data.getCooldownTicks() - 5); // Decrement the cooldown ticks by 5 (runs every 5 ticks)
            if (data.getCooldownTicks() < 0) data.setCooldownTicks(0); // Ensure cooldown ticks do not go negative
        }
    }

    private void tickActiveKiss(Player player, KissPlayerData data) {
        UUID targetId = data.getTargetUUID(); // Get the UUID of the target player
        if (targetId == null) {
            endKiss(player); // If no target UUID is set, end the kiss
            return;
        }

        // Decrement kiss duration and end if expired
        if (data.getRemainingKissTicks() > 0) {
            data.setRemainingKissTicks(data.getRemainingKissTicks() - 5);
            if (data.getRemainingKissTicks() <= 0) {
                endKiss(player);
                return;
            }
        }

        Player target = player.level().getPlayerByUUID(targetId); // Get the target player by UUID
        if (target == null || !target.isAlive() || target.distanceToSqr(player) > 16.0) {
            endKiss(player); // If the target player is not found or not alive, end the kiss
            return;
        }
        KissPlayerData targetData = target.getData(ModAttachments.kissData()); // Get the target player's KissPlayerData
        if (!targetData.isKissing() || !targetId.equals(targetData.getTargetUUID())) {
            endKiss(player); // If the target is not also kissing, end the kiss
            return;
        }
    }

    public static void startKiss(Player player, Player target) {
        int duration = Config.ANIMATION_DURATION_TICKS.get(); // Get the duration of the kiss animation

        KissPlayerData data = player.getData(ModAttachments.kissData()); // Get the player's KissPlayerData
        data.setKissing(true); // Set the player to be kissing
        data.setTargetUUID(target.getUUID()); // Set the target UUID for the player
        data.setRemainingKissTicks(duration); // Set the kiss duration

        KissPlayerData targetData = target.getData(ModAttachments.kissData()); // Get the target player's KissPlayerData
        targetData.setKissing(true); // Set the target to be kissing
        targetData.setTargetUUID(player.getUUID()); // Set the player UUID as the target for the target
        targetData.setRemainingKissTicks(duration); // Set the kiss duration

        var packet = new KissStartPayload(player.getUUID(), target.getUUID(), duration); // Create a packet to start the kiss
        PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(), packet); // Send the packet to all players in the dimension

        Vec3 mid = player.getEyePosition().add(target.getEyePosition()).scale(0.5); // Calculate the midpoint between the two players' eyes
        if (Config.ENABLE_HEARTS.get()) {
            ServerLevel level = (ServerLevel) player.level();
            level.sendParticles(
                    ParticleTypes.HEART, mid.x, mid.y, mid.z, 6, 0.3, 0.3, 0.3, 0.0); // Send heart particles at the midpoint
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
                Sounds.KISS_SOUND.get(), SoundSource.PLAYERS, 0.8f, 1.0f); // Play a kiss sound at the midpoint
    }

    public static void endKiss(Player player) {
        KissPlayerData data = player.getData(ModAttachments.kissData());
        if (!data.isKissing()) return;

        UUID targetId = data.getTargetUUID();
        data.setKissing(false);
        data.setTargetUUID(null);
        data.setCooldownTicks(effectiveCooldownTicks());
        data.setRemainingKissTicks(0);

        if (targetId != null) {
            Player target = player.level().getPlayerByUUID(targetId);
            // Fall back to server-wide lookup if target is in a different dimension
            if (target == null && player.getServer() != null) {
                target = player.getServer().getPlayerList().getPlayer(targetId);
            }
            if (target != null) {
                KissPlayerData targetData = target.getData(ModAttachments.kissData());
                targetData.setKissing(false);
                targetData.setTargetUUID(null);
                targetData.setCooldownTicks(effectiveCooldownTicks());
                targetData.setRemainingKissTicks(0);

                if (target instanceof ServerPlayer spTarget) {
                    if (Config.ENABLE_REGENERATION.get())
                        spTarget.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
                    if (Config.ENABLE_GLOWING.get())
                        spTarget.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                    PacketDistributor.sendToPlayersInDimension((ServerLevel) target.level(),
                            new KissEndPayload(spTarget.getUUID()));
                }
            }
        }

        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(),
                    new KissEndPayload(sp.getUUID()));

            if (Config.ENABLE_REGENERATION.get())
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            if (Config.ENABLE_GLOWING.get())
                sp.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        }
    }

    public static boolean hasLineOfSight(Player a, Player b) {
        Vec3 from = a.getEyePosition(); // Get the eye position of player A
        Vec3 to = b.getEyePosition(); // Get the eye position of player B
        Vec3 diff = to.subtract(from); // Calculate the vector from player A to player B
        double maxDist = effectiveMaxDistance();
        if (diff.lengthSqr() > maxDist * maxDist) return false; // If distance is greater than effective max distance, no line of sight
        var hit = a.level().clip(new net.minecraft.world.level.ClipContext(
                from, to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, a)); // Perform raycasting
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) < 0.25; // Check if there is line of sight
    }

    @SubscribeEvent
    public void onPlayerDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            KissPlayerData data = player.getData(ModAttachments.kissData()); // Get the player's KissPlayerData
            if (data.isKissing()) endKiss(player); // End the kiss if the player is damaged
        }
    }

    @SubscribeEvent
    public void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            KissPlayerData data = player.getData(ModAttachments.kissData());
            if (data.isKissing()) {
                UUID targetId = data.getTargetUUID();
                endKiss(player);
                // Ensure partner's state is cleared even if they are in a different dimension
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
        }
    }
}

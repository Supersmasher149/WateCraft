package com.wally.kissmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;

public class CommandHandler {

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("kissmod")
                .then(Commands.literal("toggle")
                        .executes(ctx -> {
                            Player player = ctx.getSource().getPlayerOrException();
                            KissPlayerData data = player.getData(ModAttachments.kissData());
                            boolean now = !data.isOptedOut();
                            data.setOptedOut(now);
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("Kissing " + (now ? "disabled" : "enabled")), false);
                            return 1;
                        })
                )
                .then(Commands.literal("debug")
                        .requires(source -> Config.DEBUG_MODE.get() && source.hasPermission(2))
                        .then(Commands.literal("state").executes(ctx -> debugState(ctx.getSource())))
                        .then(Commands.literal("cooldown")
                                .executes(ctx -> debugCooldown(ctx.getSource(), 0))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 600))
                                        .executes(ctx -> debugCooldown(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "seconds")))))
                        .then(Commands.literal("force").executes(ctx -> debugForce(ctx.getSource(), null))
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> debugForce(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
                        .then(Commands.literal("cancel").executes(ctx -> debugCancel(ctx.getSource())))
                        .then(Commands.literal("particles").executes(ctx -> debugParticles(ctx.getSource())))
                        .then(Commands.literal("animation")
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200))
                                        .executes(ctx -> debugAnimation(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "ticks")))))
                        .then(Commands.literal("target").executes(ctx -> debugTarget(ctx.getSource())))
                )
                .then(Commands.literal("stress")
                        .requires(source -> Config.DEBUG_MODE.get() && source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                                        .executes(ctx -> stressSpawn(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))))
                        .then(Commands.literal("rapid")
                                .then(Commands.argument("iterations", IntegerArgumentType.integer(1, 1000))
                                        .executes(ctx -> stressRapid(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "iterations")))))
                        .then(Commands.literal("packet")
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 10000))
                                        .executes(ctx -> stressPacket(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))))
                        .then(Commands.literal("report").executes(ctx -> stressReport(ctx.getSource())))
                        .then(Commands.literal("reset").executes(ctx -> stressReset(ctx.getSource())))
                )
        );
    }

    private static int debugState(CommandSourceStack source) throws CommandSyntaxException {
        Player player = source.getPlayerOrException();
        KissPlayerData data = player.getData(ModAttachments.kissData());
        source.sendSuccess(() -> Component.literal("kissing=" + data.isKissing()
                + ", target=" + data.getTargetUUID()
                + ", cooldownTicks=" + data.getCooldownTicks()
                + ", remainingKissTicks=" + data.getRemainingKissTicks()
                + ", optedOut=" + data.isOptedOut()), false);
        return 1;
    }

    private static int debugCooldown(CommandSourceStack source, int seconds) throws CommandSyntaxException {
        Player player = source.getPlayerOrException();
        player.getData(ModAttachments.kissData()).setCooldownTicks(seconds * 20);
        source.sendSuccess(() -> Component.literal("Set kiss cooldown to " + seconds + "s"), false);
        return 1;
    }

    private static int debugForce(CommandSourceStack source, String targetName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException() instanceof ServerPlayer sp ? sp : null;
        if (player == null) return 0;
        Player target = targetName == null ? nearestPlayer(player) : source.getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            source.sendFailure(Component.literal("No target player found"));
            return 0;
        }
        KissDetectionHandler.startKiss(player, target);
        source.sendSuccess(() -> Component.literal("Forced kiss with " + target.getName().getString()), true);
        return 1;
    }

    private static int debugCancel(CommandSourceStack source) throws CommandSyntaxException {
        Player player = source.getPlayerOrException();
        KissDetectionHandler.endKiss(player);
        source.sendSuccess(() -> Component.literal("Cancelled active kiss"), true);
        return 1;
    }

    private static int debugParticles(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException() instanceof ServerPlayer sp ? sp : null;
        if (player == null) return 0;
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.HEART, player.getX(), player.getEyeY(), player.getZ(), 20, 0.5, 0.5, 0.5, 0.0);
        source.sendSuccess(() -> Component.literal("Spawned debug particles"), false);
        return 1;
    }

    private static int debugAnimation(CommandSourceStack source, int ticks) throws CommandSyntaxException {
        Player player = source.getPlayerOrException();
        KissPlayerData data = player.getData(ModAttachments.kissData());
        data.setRemainingKissTicks(ticks);
        source.sendSuccess(() -> Component.literal("Set remaining animation ticks to " + ticks), false);
        return 1;
    }

    private static int debugTarget(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException() instanceof ServerPlayer sp ? sp : null;
        if (player == null) return 0;
        Player target = nearestPlayer(player);
        if (target == null) {
            source.sendFailure(Component.literal("No nearby target"));
            return 0;
        }
        double distance = Math.sqrt(player.getEyePosition().distanceToSqr(target.getEyePosition()));
        boolean lineOfSight = KissDetectionHandler.hasLineOfSight(player, target);
        source.sendSuccess(() -> Component.literal("target=" + target.getName().getString()
                + ", eyeDistance=" + distance
                + ", lineOfSight=" + lineOfSight), false);
        return 1;
    }

    private static Player nearestPlayer(ServerPlayer player) {
        return player.level().players().stream()
                .filter(other -> other != player)
                .min(Comparator.comparingDouble(other -> other.distanceToSqr(player)))
                .orElse(null);
    }

    private static int stressSpawn(CommandSourceStack source, int count) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException() instanceof ServerPlayer sp ? sp : null;
        if (player == null) return 0;
        ServerLevel level = (ServerLevel) player.level();
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D * i) / count;
            ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
            stand.setPos(player.getX() + Math.cos(angle) * 2.0D, player.getY(), player.getZ() + Math.sin(angle) * 2.0D);
            stand.setNoGravity(true);
            stand.setInvisible(false);
            level.addFreshEntity(stand);
        }
        source.sendSuccess(() -> Component.literal("Spawned " + count + " nearby armor stands"), true);
        return count;
    }

    private static int stressRapid(CommandSourceStack source, int iterations) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException() instanceof ServerPlayer sp ? sp : null;
        if (player == null) return 0;
        Player target = nearestPlayer(player);
        if (target == null) {
            source.sendFailure(Component.literal("No target player found"));
            return 0;
        }
        for (int i = 0; i < iterations; i++) {
            KissDetectionHandler.startKiss(player, target);
            KissDetectionHandler.endKiss(player);
        }
        StressTestState.recordRapidIterations(iterations);
        source.sendSuccess(() -> Component.literal("Ran " + iterations + " rapid start/stop iterations"), true);
        return iterations;
    }

    private static int stressPacket(CommandSourceStack source, int count) {
        StressTestState.recordPackets(count);
        source.sendSuccess(() -> Component.literal("Recorded packet spam simulation count=" + count), true);
        return count;
    }

    private static int stressReport(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Stress report: " + StressTestState.report()), false);
        return 1;
    }

    private static int stressReset(CommandSourceStack source) {
        StressTestState.reset();
        source.sendSuccess(() -> Component.literal("Stress counters reset"), false);
        return 1;
    }
}

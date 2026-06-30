package com.wally.kissmod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class KissPlayerData {
    private boolean kissing;
    private UUID targetUUID;
    private int cooldownTicks;
    private int remainingKissTicks;
    private boolean optedOut;
    private int totalKisses;

    private KissPhase kissPhase = KissPhase.NONE;
    private int kissPhaseTicks;
    private Vec3 originalPos;
    private Vec3 hugPos;

    public static final int ENTER_TICKS = 10;
    public static final int EXIT_TICKS = 10;

    public static KissPhase inferPhase(int remainingTicks, int duration) {
        if (remainingTicks <= EXIT_TICKS) return KissPhase.EXITING;
        if (remainingTicks > duration - ENTER_TICKS) return KissPhase.ENTERING;
        return KissPhase.HOLDING;
    }

    public KissPlayerData() {
        this.kissing = false;
        this.targetUUID = null;
        this.cooldownTicks = 0;
        this.remainingKissTicks = 0;
        this.optedOut = false;
        this.totalKisses = 0;
    }

    public KissPlayerData(boolean kissing, UUID targetUUID, int cooldownTicks, int remainingKissTicks, boolean optedOut, int totalKisses) {
        this.kissing = kissing;
        this.targetUUID = targetUUID;
        this.cooldownTicks = cooldownTicks;
        this.remainingKissTicks = remainingKissTicks;
        this.optedOut = optedOut;
        this.totalKisses = totalKisses;
    }

    public boolean isKissing() {
        return kissing;
    }

    public void setKissing(boolean v) {
        this.kissing = v;
        if (!v) {
            this.kissPhase = KissPhase.NONE;
            this.kissPhaseTicks = 0;
            this.originalPos = null;
            this.hugPos = null;
        }
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public void setTargetUUID(UUID v) {
        this.targetUUID = v;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public void setCooldownTicks(int v) {
        this.cooldownTicks = v;
    }

    public int getRemainingKissTicks() {
        return remainingKissTicks;
    }

    public void setRemainingKissTicks(int v) {
        this.remainingKissTicks = v;
    }

    public boolean isOptedOut() {
        return optedOut;
    }

    public void setOptedOut(boolean v) {
        this.optedOut = v;
    }

    public int getTotalKisses() {
        return totalKisses;
    }

    public void setTotalKisses(int v) {
        this.totalKisses = v;
    }

    public KissPhase getKissPhase() {
        return kissPhase;
    }

    public void setKissPhase(KissPhase phase) {
        this.kissPhase = phase;
    }

    public int getKissPhaseTicks() {
        return kissPhaseTicks;
    }

    public void setKissPhaseTicks(int ticks) {
        this.kissPhaseTicks = ticks;
    }

    public Vec3 getOriginalPos() {
        return originalPos;
    }

    public void setOriginalPos(Vec3 pos) {
        this.originalPos = pos;
    }

    public Vec3 getHugPos() {
        return hugPos;
    }

    public void setHugPos(Vec3 pos) {
        this.hugPos = pos;
    }

    public static final Codec<KissPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("kissing").forGetter(KissPlayerData::isKissing),
            Codec.STRING.optionalFieldOf("targetUUID").forGetter(data -> Optional.ofNullable(data.getTargetUUID()).map(UUID::toString)),
            Codec.INT.fieldOf("cooldownTicks").forGetter(KissPlayerData::getCooldownTicks),
            Codec.INT.optionalFieldOf("remainingKissTicks", 0).forGetter(KissPlayerData::getRemainingKissTicks),
            Codec.BOOL.optionalFieldOf("optedOut", false).forGetter(KissPlayerData::isOptedOut),
            Codec.INT.optionalFieldOf("totalKisses", 0).forGetter(KissPlayerData::getTotalKisses)
    ).apply(instance, (kissing, targetUUID, cooldownTicks, remainingKissTicks, optedOut, totalKisses) -> new KissPlayerData(
            kissing,
            targetUUID.filter(s -> !s.isEmpty()).map(UUID::fromString).orElse(null),
            cooldownTicks,
            remainingKissTicks,
            optedOut,
            totalKisses
    )));
}

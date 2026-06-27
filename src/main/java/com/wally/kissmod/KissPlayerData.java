package com.wally.kissmod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.UUID;

public class KissPlayerData {
    private boolean kissing;
    private UUID targetUUID;
    private int cooldownTicks;
    private int remainingKissTicks;
    private boolean optedOut;

    public KissPlayerData() {
        this.kissing = false;
        this.targetUUID = null;
        this.cooldownTicks = 0;
        this.remainingKissTicks = 0;
        this.optedOut = false;
    }

    public KissPlayerData(boolean kissing, UUID targetUUID, int cooldownTicks, int remainingKissTicks, boolean optedOut) {
        this.kissing = kissing;
        this.targetUUID = targetUUID;
        this.cooldownTicks = cooldownTicks;
        this.remainingKissTicks = remainingKissTicks;
        this.optedOut = optedOut;
    }

    // Getter for the "kissing" status
    public boolean isKissing() {
        return kissing;
    }

    // Setter for the "kissing" status
    public void setKissing(boolean v) {
        this.kissing = v;
    }

    // Getter for the target UUID
    public UUID getTargetUUID() {
        return targetUUID;
    }

    // Setter for the target UUID
    public void setTargetUUID(UUID v) {
        this.targetUUID = v;
    }

    // Getter for the remaining cooldown ticks
    public int getCooldownTicks() {
        return cooldownTicks;
    }

    // Setter for the remaining cooldown ticks
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

    public static final Codec<KissPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("kissing").forGetter(KissPlayerData::isKissing),
            Codec.STRING.optionalFieldOf("targetUUID").forGetter(data -> Optional.ofNullable(data.getTargetUUID()).map(UUID::toString)),
            Codec.INT.fieldOf("cooldownTicks").forGetter(KissPlayerData::getCooldownTicks),
            Codec.INT.optionalFieldOf("remainingKissTicks", 0).forGetter(KissPlayerData::getRemainingKissTicks),
            Codec.BOOL.optionalFieldOf("optedOut", false).forGetter(KissPlayerData::isOptedOut)
    ).apply(instance, (kissing, targetUUID, cooldownTicks, remainingKissTicks, optedOut) -> new KissPlayerData(
            kissing,
            targetUUID.filter(s -> !s.isEmpty()).map(UUID::fromString).orElse(null),
            cooldownTicks,
            remainingKissTicks,
            optedOut
    )));
}

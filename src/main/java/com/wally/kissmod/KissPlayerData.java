package com.wally.kissmod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

public class KissPlayerData {
    private boolean kissing;
    private UUID targetUUID;
    private int cooldownTicks;

    public KissPlayerData() {
        this.kissing = false;
        this.targetUUID = null;
        this.cooldownTicks = 0;
    }

    public KissPlayerData(boolean kissing, UUID targetUUID, int cooldownTicks) {
        this.kissing = kissing;
        this.targetUUID = targetUUID;
        this.cooldownTicks = cooldownTicks;
    }

    public boolean isKissing() { return kissing; }
    public void setKissing(boolean v) { this.kissing = v; }

    public UUID getTargetUUID() { return targetUUID; }
    public void setTargetUUID(UUID v) { this.targetUUID = v; }

    public int getCooldownTicks() { return cooldownTicks; }
    public void setCooldownTicks(int v) { this.cooldownTicks = v; }

    public static final Codec<KissPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("kissing").forGetter(KissPlayerData::isKissing),
            Codec.STRING.xmap(UUID::fromString, UUID::toString).optionalFieldOf("targetUUID", null).forGetter(KissPlayerData::getTargetUUID),
            Codec.INT.fieldOf("cooldownTicks").forGetter(KissPlayerData::getCooldownTicks)
    ).apply(instance, KissPlayerData::new));
}

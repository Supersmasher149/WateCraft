package com.wally.kissmod.gametest;

import com.mojang.serialization.JsonOps;
import com.wally.kissmod.KissMath;
import com.wally.kissmod.KissPlayerData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("kissmod")
@PrefixGameTestTemplate(false)
public class KissDevelopmentGameTests {
    @GameTest(template = "empty")
    public static void mathDistanceAndLookValidation(GameTestHelper helper) {
        Vec3 a = new Vec3(0.0D, 1.6D, 0.0D);
        Vec3 b = new Vec3(0.0D, 1.6D, 1.0D);
        helper.assertTrue(KissMath.isWithinDistance(a, b, 1.5D), "Expected eye positions to be within range");
        helper.assertTrue(KissMath.isLookingAt(new Vec3(0.0D, 0.0D, 1.0D), a, b, 0.8D), "Expected forward look vector to face target");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void capabilitySerializationRoundTrips(GameTestHelper helper) {
        UUID target = UUID.fromString("00000000-0000-0000-0000-000000000001");
        KissPlayerData data = new KissPlayerData(true, target, 40, 60, true);
        var encoded = KissPlayerData.CODEC.encodeStart(JsonOps.INSTANCE, data).result();
        helper.assertTrue(encoded.isPresent(), "Expected KissPlayerData to encode");
        var decoded = KissPlayerData.CODEC.parse(JsonOps.INSTANCE, encoded.orElseThrow()).result();
        helper.assertTrue(decoded.isPresent(), "Expected KissPlayerData to decode");
        helper.assertTrue(decoded.orElseThrow().isKissing(), "Expected kissing flag to round-trip");
        helper.assertTrue(target.equals(decoded.orElseThrow().getTargetUUID()), "Expected target UUID to round-trip");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void missingTargetUuidStaysCompatible(GameTestHelper helper) {
        var json = com.google.gson.JsonParser.parseString("{\"kissing\":false,\"cooldownTicks\":0}");
        var decoded = KissPlayerData.CODEC.parse(JsonOps.INSTANCE, json).result();
        helper.assertTrue(decoded.isPresent(), "Expected old attachment data without targetUUID to decode");
        helper.assertTrue(decoded.orElseThrow().getTargetUUID() == null, "Expected missing targetUUID to decode as null");
        helper.succeed();
    }
}

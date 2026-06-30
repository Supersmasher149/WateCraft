package com.wally.kissmod.gametest;

import com.mojang.serialization.JsonOps;
import com.wally.kissmod.KissPhase;
import com.wally.kissmod.KissPlayerData;
import com.wally.kissmod.KissPoseManager;
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
    public static void distanceValidation(GameTestHelper helper) {
        Vec3 a = new Vec3(0.0D, 1.6D, 0.0D);
        Vec3 b = new Vec3(0.0D, 1.6D, 1.0D);
        helper.assertTrue(a.distanceToSqr(b) <= 1.5D * 1.5D, "Expected eye positions to be within range");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void capabilitySerializationRoundTrips(GameTestHelper helper) {
        UUID target = UUID.fromString("00000000-0000-0000-0000-000000000001");
        KissPlayerData data = new KissPlayerData(true, target, 40, 60, true, 0);
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

    // ---- Hug position math tests ----

    @GameTest(template = "empty")
    public static void hugPositionsComputedCorrectly(GameTestHelper helper) {
        Vec3 posA = new Vec3(0, 0, 0);
        Vec3 posB = new Vec3(0, 0, 3);
        Vec3[] hugs = KissPoseManager.computeHugPositions(posA, posB);

        double expectedMidZ = 1.5;
        double halfDist = KissPoseManager.HUG_DISTANCE * 0.5;
        helper.assertTrue(Math.abs(hugs[0].z - (expectedMidZ - halfDist)) < 0.01,
                "Expected hugA z at midpoint minus half distance");
        helper.assertTrue(Math.abs(hugs[1].z - (expectedMidZ + halfDist)) < 0.01,
                "Expected hugB z at midpoint plus half distance");
        helper.assertTrue(Math.abs(hugs[0].x) < 0.01 && Math.abs(hugs[1].x) < 0.01,
                "Expected no x-axis offset for z-aligned players");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hugPositionsSameSpot(GameTestHelper helper) {
        Vec3 posA = new Vec3(0, 0, 0);
        Vec3 posB = new Vec3(0, 0, 0);
        Vec3[] hugs = KissPoseManager.computeHugPositions(posA, posB);

        double halfDist = KissPoseManager.HUG_DISTANCE * 0.5;
        helper.assertTrue(Math.abs(hugs[0].z - (-halfDist)) < 0.01,
                "Expected fallback z direction on identical positions");
        helper.assertTrue(Math.abs(hugs[1].z - halfDist) < 0.01,
                "Expected fallback z direction on identical positions");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hugPositionsDifferentY(GameTestHelper helper) {
        Vec3 posA = new Vec3(0, 1, 0);
        Vec3 posB = new Vec3(0, 2, 3);
        Vec3[] hugs = KissPoseManager.computeHugPositions(posA, posB);

        helper.assertTrue(Math.abs(hugs[0].y - 1.5) < 0.01, "Expected hugA Y to be midpoint average");
        helper.assertTrue(Math.abs(hugs[1].y - 1.5) < 0.01, "Expected hugB Y to be midpoint average");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hugPositionsXAxis(GameTestHelper helper) {
        Vec3 posA = new Vec3(0, 0, 0);
        Vec3 posB = new Vec3(5, 0, 0);
        Vec3[] hugs = KissPoseManager.computeHugPositions(posA, posB);

        double expectedMidX = 2.5;
        double halfDist = KissPoseManager.HUG_DISTANCE * 0.5;
        helper.assertTrue(Math.abs(hugs[0].x - (expectedMidX - halfDist)) < 0.01,
                "Expected x-axis hug positions to be correct");
        helper.assertTrue(Math.abs(hugs[1].x - (expectedMidX + halfDist)) < 0.01,
                "Expected x-axis hug positions to be correct");
        helper.assertTrue(Math.abs(hugs[0].z) < 0.01 && Math.abs(hugs[1].z) < 0.01,
                "Expected no z-axis offset for x-aligned players");
        helper.succeed();
    }

    // ---- Phase inference tests ----

    @GameTest(template = "empty")
    public static void phaseInferenceEntering(GameTestHelper helper) {
        int duration = 60;
        // remaining > duration - ENTER_TICKS → ENTERING
        helper.assertTrue(KissPlayerData.inferPhase(60, duration) == KissPhase.ENTERING,
                "Expected ENTERING at remaining == duration");
        helper.assertTrue(KissPlayerData.inferPhase(51, duration) == KissPhase.ENTERING,
                "Expected ENTERING when remaining > duration - 10");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void phaseInferenceHolding(GameTestHelper helper) {
        int duration = 60;
        // remaining in (EXIT_TICKS, duration - ENTER_TICKS] → HOLDING
        helper.assertTrue(KissPlayerData.inferPhase(50, duration) == KissPhase.HOLDING,
                "Expected HOLDING at remaining == duration - ENTER_TICKS");
        helper.assertTrue(KissPlayerData.inferPhase(30, duration) == KissPhase.HOLDING,
                "Expected HOLDING in middle of duration");
        helper.assertTrue(KissPlayerData.inferPhase(11, duration) == KissPhase.HOLDING,
                "Expected HOLDING when remaining > EXIT_TICKS");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void phaseInferenceExiting(GameTestHelper helper) {
        int duration = 60;
        // remaining <= EXIT_TICKS → EXITING
        helper.assertTrue(KissPlayerData.inferPhase(10, duration) == KissPhase.EXITING,
                "Expected EXITING at remaining == EXIT_TICKS");
        helper.assertTrue(KissPlayerData.inferPhase(5, duration) == KissPhase.EXITING,
                "Expected EXITING below EXIT_TICKS");
        helper.assertTrue(KissPlayerData.inferPhase(0, duration) == KissPhase.EXITING,
                "Expected EXITING at remaining == 0");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void phaseInferenceShortDuration(GameTestHelper helper) {
        // Duration shorter than phase windows — test edge behavior
        int shortDuration = 15;
        helper.assertTrue(KissPlayerData.inferPhase(15, shortDuration) == KissPhase.ENTERING,
                "Expected ENTERING when entire short duration is entering phase");
        helper.assertTrue(KissPlayerData.inferPhase(6, shortDuration) == KissPhase.EXITING,
                "Expected EXITING when remaining <= ENTER_TICKS and near zero");
        helper.succeed();
    }

    // ---- PlayerData cleanup tests ----

    @GameTest(template = "empty")
    public static void kissingFalseClearsPhaseAndPositions(GameTestHelper helper) {
        KissPlayerData data = new KissPlayerData();
        data.setKissing(true);
        data.setKissPhase(KissPhase.HOLDING);
        data.setKissPhaseTicks(5);
        data.setOriginalPos(new Vec3(1, 2, 3));
        data.setHugPos(new Vec3(4, 5, 6));

        data.setKissing(false);

        helper.assertTrue(data.getKissPhase() == KissPhase.NONE, "Expected phase reset to NONE");
        helper.assertTrue(data.getKissPhaseTicks() == 0, "Expected phase ticks reset to 0");
        helper.assertTrue(data.getOriginalPos() == null, "Expected originalPos cleared");
        helper.assertTrue(data.getHugPos() == null, "Expected hugPos cleared");
        helper.succeed();
    }

    // ---- Data model fields ----

    @GameTest(template = "empty")
    public static void enteringPhaseSetsPhaseTicksToZero(GameTestHelper helper) {
        KissPlayerData data = new KissPlayerData();
        data.setKissPhase(KissPhase.ENTERING);
        data.setKissPhaseTicks(0);
        helper.assertTrue(data.getKissPhase() == KissPhase.ENTERING, "Expected entering phase");
        helper.assertTrue(data.getKissPhaseTicks() == 0, "Expected phase ticks start at 0");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void constantsArePositive(GameTestHelper helper) {
        helper.assertTrue(KissPlayerData.ENTER_TICKS > 0, "Expected ENTER_TICKS > 0");
        helper.assertTrue(KissPlayerData.EXIT_TICKS > 0, "Expected EXIT_TICKS > 0");
        helper.assertTrue(KissPoseManager.HUG_DISTANCE > 0, "Expected HUG_DISTANCE > 0");
        helper.succeed();
    }

    // ---- Backward compat with new CODEC fields ----

    @GameTest(template = "empty")
    public static void oldDataWithoutNewFieldsStillDecodes(GameTestHelper helper) {
        var json = com.google.gson.JsonParser.parseString("{\"kissing\":false,\"cooldownTicks\":0,\"remainingKissTicks\":0}");
        var decoded = KissPlayerData.CODEC.parse(JsonOps.INSTANCE, json).result();
        helper.assertTrue(decoded.isPresent(), "Expected old data without new fields to decode");
        helper.assertTrue(decoded.orElseThrow().getKissPhase() == KissPhase.NONE, "Expected default phase NONE");
        helper.succeed();
    }
}

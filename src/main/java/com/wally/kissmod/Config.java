package com.wally.kissmod;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Detection category
    public static final ModConfigSpec.DoubleValue MAX_DISTANCE;
    public static final ModConfigSpec.BooleanValue REQUIRE_LOOK;
    public static final ModConfigSpec.IntValue COOLDOWN_SECONDS;

    // Animation category
    public static final ModConfigSpec.IntValue ANIMATION_DURATION_TICKS;
    public static final ModConfigSpec.BooleanValue DISABLE_HEAD_TILT;

    // Effects category
    public static final ModConfigSpec.BooleanValue ENABLE_HEARTS;
    public static final ModConfigSpec.BooleanValue ENABLE_REGENERATION;
    public static final ModConfigSpec.BooleanValue ENABLE_GLOWING;

    static {
        BUILDER.push("detection");
        MAX_DISTANCE = BUILDER
                .comment("Maximum 3D distance between eyes for a kiss to register")
                .defineInRange("max_head_distance", 1.5, 0.1, 10.0);
        REQUIRE_LOOK = BUILDER
                .comment("Require players to look at each other")
                .define("require_look_at_each_other", true);
        COOLDOWN_SECONDS = BUILDER
                .comment("Cooldown in seconds between kisses")
                .defineInRange("cooldown_seconds", 5, 1, 600);
        BUILDER.pop();

        BUILDER.push("animation");
        ANIMATION_DURATION_TICKS = BUILDER
                .comment("Duration of the kiss animation in ticks (1 second = 20 ticks)")
                .defineInRange("animation_duration_ticks", 60, 10, 200);
        DISABLE_HEAD_TILT = BUILDER
                .comment("Disable the head tilt animation entirely")
                .define("disable_head_tilt", false);
        BUILDER.pop();

        BUILDER.push("effects");
        ENABLE_HEARTS = BUILDER
                .comment("Spawn heart particles when kissing")
                .define("enable_hearts", true);
        ENABLE_REGENERATION = BUILDER
                .comment("Apply Regeneration I when a kiss finishes")
                .define("apply_regeneration_effect", true);
        ENABLE_GLOWING = BUILDER
                .comment("Apply Glowing effect when a kiss finishes")
                .define("enable_glowing", true);
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    public static void onConfigLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            kissmod.LOGGER.info("kissmod config reloaded");
        }
    }
}

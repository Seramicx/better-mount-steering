package com.bettermountsteering;

import net.minecraftforge.common.ForgeConfigSpec;

public class BetterMountSteeringConfig {

    public static final ForgeConfigSpec CLIENT_CONFIG;

    public static final ForgeConfigSpec.DoubleValue MOUNT_TURN_SPEED;
    public static final ForgeConfigSpec.BooleanValue SMOOTH_LOCKON_MOUNT_TURN;
    public static final ForgeConfigSpec.DoubleValue BLO_LOCKON_TURN_SMOOTHNESS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Mount Steering Settings").push("mount");

        MOUNT_TURN_SPEED = builder
                .comment(
                    "Per-tick turn factor for the decoupled mount-rotate (NON locked-on) body lerp.",
                    "Lower = smoother/slower turn. 0.15 = very smooth, 0.25 = balanced, 0.5 = snappy."
                )
                .defineInRange("mountTurnSpeed", 0.25, 0.05, 1.0);

        SMOOTH_LOCKON_MOUNT_TURN = builder
                .comment(
                    "Replace BLO's instant 8-direction snap during mount lock-on with a proportional",
                    "lerp toward the target direction. Body trails the locked target smoothly while",
                    "BLO's camera tracks the target as usual. Only affects locked-on mount turning -",
                    "BLO's free-turn behavior outside lock-on is untouched.",
                    "Tune the smoothness via bloLockOnTurnSmoothness."
                )
                .define("smoothLockOnMountTurn", true);

        BLO_LOCKON_TURN_SMOOTHNESS = builder
                .comment(
                    "Per-tick lerp factor for body yaw during BLO+mount lock-on smoothing.",
                    "Replaces BLO's instant snap with a proportional ease-out lerp toward the target",
                    "direction. Higher = quicker / less trail. Lower = smoother / longer trail.",
                    "Note: lock-on target moves every tick (BLO recomputes vs the locked enemy each",
                    "frame) so steady-state lag = delta * (1 - factor) / factor. With BLO writing up",
                    "to ~30 deg/tick, that's:",
                    "  0.10 = very smooth, ~270 deg lag in steady state (very long trail)",
                    "  0.40 = balanced, ~45 deg lag",
                    "  0.70 = responsive, ~13 deg lag",
                    "  0.85 = quick, ~5 deg lag",
                    "  0.50 = balanced (default - meaningful smoothing, still responsive)",
                    "  0.85 = matches the non-locked-on mountTurnSpeed feel for active combat",
                    "  0.95 = instant-feeling, only smooths abrupt sprint+WASD direction snaps",
                    "  1.0  = no smoothing (BLO's instant snap passes through unchanged)",
                    "Independent from mountTurnSpeed - that's only used for the non-locked-on path."
                )
                .defineInRange("bloLockOnTurnSmoothness", 0.50, 0.01, 1.0);

        builder.pop();

        CLIENT_CONFIG = builder.build();
    }
}

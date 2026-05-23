package com.bettermountsteering;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BetterMountSteeringConfig {

    public static final ModConfigSpec CLIENT_CONFIG;

    public static final ModConfigSpec.DoubleValue MOUNT_TURN_SPEED;
    public static final ModConfigSpec.BooleanValue SMOOTH_LOCKON_MOUNT_TURN;
    public static final ModConfigSpec.DoubleValue BLO_LOCKON_TURN_SMOOTHNESS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Mount Steering Settings").push("mount");

        MOUNT_TURN_SPEED = builder
                .comment(
                    "Per-tick turn factor for the decoupled mount-rotate body lerp.",
                    "Lower = smoother/slower turn. 0.15 = very smooth, 0.25 = balanced, 0.5 = snappy."
                )
                .defineInRange("mountTurnSpeed", 0.25, 0.05, 1.0);

        SMOOTH_LOCKON_MOUNT_TURN = builder
                .comment(
                    "Smooth BLO's mount lock-on body rotation. When true, replaces BLO's",
                    "discrete 8-direction snap on a mount with a per-tick ease-out lerp.",
                    "Set false to fall back to BLO's vanilla snap behavior."
                )
                .define("smoothLockOnMountTurn", true);

        BLO_LOCKON_TURN_SMOOTHNESS = builder
                .comment(
                    "Per-tick lerp factor for body yaw during BLO + mount lock-on.",
                    "0.10 = very smooth, long trail. 0.5 = balanced. 0.85 = responsive.",
                    "1.0 = no smoothing (BLO's snap passes through unchanged)."
                )
                .defineInRange("bloLockOnTurnSmoothness", 0.5, 0.05, 1.0);

        builder.pop();

        CLIENT_CONFIG = builder.build();
    }
}

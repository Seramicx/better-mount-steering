package com.bettermountsteering;

import net.minecraftforge.common.ForgeConfigSpec;

public class BetterMountSteeringConfig {

    public static final ForgeConfigSpec CLIENT_CONFIG;
    public static final ForgeConfigSpec.DoubleValue MOUNT_TURN_SPEED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Mount Steering Settings").push("mount");
        MOUNT_TURN_SPEED = builder
                .comment(
                    "Per-tick turn factor for the decoupled mount-rotate body lerp.",
                    "Lower = smoother/slower turn. 0.15 = very smooth, 0.25 = balanced, 0.5 = snappy."
                )
                .defineInRange("mountTurnSpeed", 0.25, 0.05, 1.0);
        builder.pop();
        CLIENT_CONFIG = builder.build();
    }
}

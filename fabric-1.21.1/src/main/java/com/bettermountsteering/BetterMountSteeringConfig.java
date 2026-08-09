package com.bettermountsteering;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BetterMountSteeringConfig {

    public static final ModConfigSpec CLIENT_CONFIG;

    public enum IdleBehavior { FACE_CAMERA, HOLD_DIRECTION }

    public static final ModConfigSpec.DoubleValue MOUNT_TURN_SPEED;
    public static final ModConfigSpec.BooleanValue AUTO_FACE_ON_COMBAT;
    public static final ModConfigSpec.BooleanValue SLOW_MOUNT_ON_COMBAT;
    public static final ModConfigSpec.EnumValue<IdleBehavior> IDLE_BEHAVIOR;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Mount Steering Settings").push("mount");
        MOUNT_TURN_SPEED = builder
                .comment(
                    "Per-tick turn factor for the decoupled mount-rotate body lerp.",
                    "Lower = smoother/slower turn. 0.15 = very smooth, 0.25 = balanced, 0.5 = snappy."
                )
                .defineInRange("mountTurnSpeed", 0.25, 0.05, 1.0);
        AUTO_FACE_ON_COMBAT = builder
                .comment(
                    "Snap the body to the camera/crosshair direction when you attack, block, aim or",
                    "cast while mounted. Set false for pure omnidirectional steering with no",
                    "auto-facing at all."
                )
                .define("autoFaceOnCombat", true);
        SLOW_MOUNT_ON_COMBAT = builder
                .comment(
                    "Cut mount speed to half while you attack, block, aim or cast. Set false to keep",
                    "full speed through combat."
                )
                .define("slowMountOnCombat", true);
        IDLE_BEHAVIOR = builder
                .comment(
                    "Body behavior when you stop moving on a mount. HOLD_DIRECTION (default): body",
                    "keeps the direction it was moving in until you move again or leave 3rd-person",
                    "back. FACE_CAMERA: body lerps back to the camera direction on stop."
                )
                .defineEnum("idleBehavior", IdleBehavior.HOLD_DIRECTION);
        builder.pop();
        CLIENT_CONFIG = builder.build();
    }
}

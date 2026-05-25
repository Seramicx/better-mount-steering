package com.bettermountsteering.compat;

import net.minecraft.client.player.Input;

public final class ControllableHelper {

    private ControllableHelper() {}

    public static float[] readAnalogDirection(Input input) {
        float forward = input.forwardImpulse;
        float strafe  = input.leftImpulse;

        float magnitude = (float) Math.sqrt(forward * forward + strafe * strafe);
        if (magnitude < 0.15F) return new float[]{0, 0};
        return new float[]{forward, strafe};
    }
}

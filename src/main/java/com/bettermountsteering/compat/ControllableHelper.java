package com.bettermountsteering.compat;

import net.minecraft.client.player.Input;

/**
 * Controllable integration (analog stick reading) for mount-rotate. When
 * Controllable is present, analog stick values flow through vanilla
 * {@link Input#forwardImpulse}/{@code leftImpulse} as fractional floats. When
 * absent, the impulse values are ±1/0 from keyboard and the deadzone
 * gracefully treats those as "above threshold."
 */
public final class ControllableHelper {

    private static final float DEADZONE = 0.15F;

    private ControllableHelper() {}

    /**
     * Returns {forward, strafe} from the Input's impulse values, with deadzone.
     * Below the deadzone, returns {0, 0}.
     */
    public static float[] readAnalogDirection(Input input) {
        float forward = input.forwardImpulse;
        float strafe  = input.leftImpulse;

        float magnitude = (float) Math.sqrt(forward * forward + strafe * strafe);
        if (magnitude < DEADZONE) return new float[]{0, 0};
        return new float[]{forward, strafe};
    }
}

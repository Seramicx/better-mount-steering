package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Field;

/**
 * Force-skips BLO's two post-lock-off transition timers via reflection:
 * <ul>
 *   <li>{@code BLOCameraSetting.transitionTick} (BLO's camera offset/FOV
 *       transition - drives the visible camera-revert animation after lock-off)</li>
 *   <li>{@code EpicFightCameraAPI.blo$unlockDelayTick} (BLO's unlock-delay
 *       counter - keeps the lock-on camera transform engaged for a beat after
 *       lockingOnTarget flips false)</li>
 * </ul>
 *
 * <p>On a mount, the natural transition produces a brief camera flicker as
 * BLO's offset/FOV lerps back to default while we're simultaneously trying to
 * smooth-trail the body. Calling {@link #skipPostLockOff()} on the lock-off
 * edge (only while mounted) tells BLO "transition complete, release now",
 * eliminating the flicker.
 *
 * <p>Reflection-only because BLO is an optional companion (not on
 * compile-time classpath). All failures are logged + swallowed; a missing
 * BLO or renamed field just makes this a no-op rather than crashing the
 * camera.
 *
 * <p>Ported from the OG {@code lockonfix} mod's identical hook.
 */
public final class BLOTransitionSkipHook {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static Field transitionTickField = null;       // BLOCameraSetting.transitionTick (static)
    private static Field unlockDelayTickField = null;      // EpicFightCameraAPI.blo$unlockDelayTick (instance)
    private static Field maxUnlockDelayTickField = null;   // EpicFightCameraAPI.blo$maxUnlockDelayTick (instance)
    private static Object epicFightCameraApiSingleton = null;

    private static boolean resolved = false;
    private static boolean resolvedOk = false;

    private BLOTransitionSkipHook() {}

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (!IntegrationRegistry.isBetterLockOn()) return;

        try {
            Class<?> bloCameraSetting = Class.forName("net.shelmarow.betterlockon.client.control.BLOCameraSetting");
            transitionTickField = bloCameraSetting.getDeclaredField("transitionTick");
            transitionTickField.setAccessible(true);

            Class<?> epicFightCameraApi = Class.forName("yesman.epicfight.api.client.camera.EpicFightCameraAPI");
            epicFightCameraApiSingleton = epicFightCameraApi.getMethod("getInstance").invoke(null);

            // Fields injected into EpicFightCameraAPI by BLO's mixin.
            unlockDelayTickField = epicFightCameraApi.getDeclaredField("blo$unlockDelayTick");
            unlockDelayTickField.setAccessible(true);
            maxUnlockDelayTickField = epicFightCameraApi.getDeclaredField("blo$maxUnlockDelayTick");
            maxUnlockDelayTickField.setAccessible(true);

            resolvedOk = true;
        } catch (Throwable t) {
            LOGGER.warn("BLO transition skip: failed to resolve fields ({}), post-lock-off flicker may persist", t.getMessage());
        }
    }

    /**
     * Sets {@code transitionTick} to its terminal value (30) and
     * {@code blo$unlockDelayTick} to its max - both signal "transition done"
     * to BLO's per-tick logic, releasing the camera transform immediately.
     */
    public static void skipPostLockOff() {
        resolve();
        if (!resolvedOk) return;
        try {
            transitionTickField.setInt(null, 30);
            int max = maxUnlockDelayTickField.getInt(epicFightCameraApiSingleton);
            unlockDelayTickField.setInt(epicFightCameraApiSingleton, max);
        } catch (Throwable t) {
            LOGGER.warn("BLO transition skip: reflection set failed: {}", t.getMessage());
        }
    }
}

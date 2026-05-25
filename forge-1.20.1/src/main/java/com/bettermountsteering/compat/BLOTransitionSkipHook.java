package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Field;

public final class BLOTransitionSkipHook {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static Field transitionTickField = null;
    private static Field unlockDelayTickField = null;
    private static Field maxUnlockDelayTickField = null;
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

            unlockDelayTickField = epicFightCameraApi.getDeclaredField("blo$unlockDelayTick");
            unlockDelayTickField.setAccessible(true);
            maxUnlockDelayTickField = epicFightCameraApi.getDeclaredField("blo$maxUnlockDelayTick");
            maxUnlockDelayTickField.setAccessible(true);

            resolvedOk = true;
        } catch (Throwable t) {
            LOGGER.warn("BLO transition skip: failed to resolve fields ({}), post-lock-off flicker may persist", t.getMessage());
        }
    }

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

package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Reflection-only EpicFight access (EF is an OPTIONAL dep here). Used to
 * detect lock-on state for the BLO+mount smooth-turn feature.
 */
public final class EpicFightHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean resolved = false;
    private static Method getInstanceMethod = null;
    private static Method isLockingOnTargetMethod = null;

    private EpicFightHelper() {}

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (!IntegrationRegistry.isEpicFight()) return;
        try {
            Class<?> apiClass = Class.forName("yesman.epicfight.api.client.camera.EpicFightCameraAPI");
            getInstanceMethod = apiClass.getMethod("getInstance");
            isLockingOnTargetMethod = apiClass.getMethod("isLockingOnTarget");
        } catch (Throwable t) {
            LOGGER.debug("EpicFight reflection unavailable: {}", t.getMessage());
        }
    }

    public static boolean isLockOnTargeting() {
        resolve();
        if (getInstanceMethod == null) return false;
        try {
            Object api = getInstanceMethod.invoke(null);
            if (api == null) return false;
            return (Boolean) isLockingOnTargetMethod.invoke(api);
        } catch (Throwable t) {
            return false;
        }
    }
}

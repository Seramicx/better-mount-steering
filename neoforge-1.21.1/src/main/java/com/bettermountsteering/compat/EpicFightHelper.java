package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class EpicFightHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean resolved = false;
    @Nullable private static Method getInstanceMethod = null;
    @Nullable private static Method isLockingOnTargetMethod = null;
    @Nullable private static Field cameraYRotField = null;
    @Nullable private static Field cameraXRotField = null;

    private EpicFightHelper() {}

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (!IntegrationRegistry.isEpicFight()) return;
        try {
            Class<?> apiClass = Class.forName("yesman.epicfight.api.client.camera.EpicFightCameraAPI");
            getInstanceMethod = apiClass.getMethod("getInstance");
            isLockingOnTargetMethod = apiClass.getMethod("isLockingOnTarget");
            cameraYRotField = apiClass.getDeclaredField("cameraYRot");
            cameraYRotField.setAccessible(true);
            cameraXRotField = apiClass.getDeclaredField("cameraXRot");
            cameraXRotField.setAccessible(true);
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

    public static float getCameraYRot() {
        resolve();
        if (cameraYRotField == null || getInstanceMethod == null) return Float.NaN;
        try {
            Object api = getInstanceMethod.invoke(null);
            if (api == null) return Float.NaN;
            return cameraYRotField.getFloat(api);
        } catch (Throwable t) {
            return Float.NaN;
        }
    }

    public static float getCameraXRot() {
        resolve();
        if (cameraXRotField == null || getInstanceMethod == null) return Float.NaN;
        try {
            Object api = getInstanceMethod.invoke(null);
            if (api == null) return Float.NaN;
            return cameraXRotField.getFloat(api);
        } catch (Throwable t) {
            return Float.NaN;
        }
    }
}

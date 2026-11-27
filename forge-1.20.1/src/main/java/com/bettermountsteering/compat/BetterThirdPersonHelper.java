package com.bettermountsteering.compat;

import com.bettermountsteering.api.MountCameraSource;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class BetterThirdPersonHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static Method getCameraManager;
    private static Method getCustomCamera;
    private static Method getYaw;
    private static Method getPitch;
    private static Method setCameraRotation;
    private static Constructor<?> rotation;
    private static boolean resolved = false;

    // Better Third Person keeps its own free camera, so it stands in for Shoulder Surfing as the camera to steer toward
    public static final MountCameraSource SOURCE = new MountCameraSource() {
        @Override
        public boolean isActive() {
            return camera() != null;
        }

        @Override
        public float yaw() {
            return read(getYaw);
        }

        @Override
        public float xRot() {
            return read(getPitch);
        }

        @Override
        public void onDecoupleEnd(float yaw) {
            Object camera = camera();
            if (camera == null || rotation == null) return;
            try {
                setCameraRotation.invoke(camera, rotation.newInstance(yaw, read(getPitch)));
            } catch (Throwable ignored) {
            }
        }
    };

    private BetterThirdPersonHelper() {}

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> mod = Class.forName("io.socol.betterthirdperson.BetterThirdPerson");
            getCameraManager = mod.getMethod("getCameraManager");
            getCustomCamera = Class.forName("io.socol.betterthirdperson.api.CustomCameraManager").getMethod("getCustomCamera");
            Class<?> camera = Class.forName("io.socol.betterthirdperson.api.CustomCamera");
            getYaw = camera.getMethod("getYaw");
            getPitch = camera.getMethod("getPitch");
            Class<?> rot = Class.forName("io.socol.betterthirdperson.api.util.Rotation");
            setCameraRotation = camera.getMethod("setCameraRotation", rot);
            rotation = rot.getConstructor(float.class, float.class);
        } catch (Throwable t) {
            getCameraManager = null;
            LOGGER.debug("Better Third Person camera unavailable: {}", t.toString());
        }
    }

    @Nullable
    private static Object camera() {
        resolve();
        if (getCameraManager == null) return null;
        try {
            Object manager = getCameraManager.invoke(null);
            return manager == null ? null : getCustomCamera.invoke(manager);
        } catch (Throwable t) {
            return null;
        }
    }

    private static float read(Method method) {
        Object camera = camera();
        if (camera == null) return 0.0F;
        try {
            return (Float) method.invoke(camera);
        } catch (Throwable t) {
            return 0.0F;
        }
    }
}

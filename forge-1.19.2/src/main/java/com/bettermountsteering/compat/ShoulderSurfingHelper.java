package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ShoulderSurfingHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean resolved = false;
    @Nullable private static Method getInstanceMethod = null;
    @Nullable private static Method getCameraMethod = null;
    @Nullable private static Method isShoulderSurfingMethod = null;
    @Nullable private static Method getYRotMethod = null;
    @Nullable private static Method getXRotMethod = null;
    @Nullable private static Method lookAtCrosshairMethod = null;
    @Nullable private static Field  lastMovedYRotField = null;

    private ShoulderSurfingHelper() {}

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (!IntegrationRegistry.isShoulderSurfing()) return;
        try {
            Class<?> ssrClass = Class.forName("com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing");
            getInstanceMethod = ssrClass.getMethod("getInstance");
            Class<?> iface = Class.forName("com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing");
            getCameraMethod = iface.getMethod("getCamera");
            isShoulderSurfingMethod = iface.getMethod("isShoulderSurfing");
            Class<?> camIface = Class.forName("com.github.exopandora.shouldersurfing.api.client.IShoulderSurfingCamera");
            getYRotMethod = camIface.getMethod("getYRot");
            getXRotMethod = camIface.getMethod("getXRot");
        } catch (Throwable t) {
            LOGGER.debug("SSR reflection unavailable: {}", t.getMessage());
        }
        try {
            Class<?> camImpl = Class.forName("com.github.exopandora.shouldersurfing.client.ShoulderSurfingCamera");
            lastMovedYRotField = camImpl.getDeclaredField("lastMovedYRot");
            lastMovedYRotField.setAccessible(true);
        } catch (Throwable t) {
            LOGGER.debug("SSR lastMovedYRot field unavailable: {}", t.getMessage());
        }
    }

    public static boolean isShoulderSurfingActive() {
        resolve();
        if (getInstanceMethod == null) return false;
        try {
            Object inst = getInstanceMethod.invoke(null);
            if (inst == null) return false;
            return (Boolean) isShoulderSurfingMethod.invoke(inst);
        } catch (Throwable t) {
            return false;
        }
    }

    public static float getCameraYaw() {
        resolve();
        if (getInstanceMethod != null) {
            try {
                Object inst = getInstanceMethod.invoke(null);
                if (inst != null) {
                    Object cam = getCameraMethod.invoke(inst);
                    if (cam != null) return (Float) getYRotMethod.invoke(cam);
                }
            } catch (Throwable ignored) {}
        }
        return Minecraft.getInstance().gameRenderer.getMainCamera().getYRot();
    }

    public static float getCameraXRot() {
        resolve();
        if (getInstanceMethod != null) {
            try {
                Object inst = getInstanceMethod.invoke(null);
                if (inst != null) {
                    Object cam = getCameraMethod.invoke(inst);
                    if (cam != null) return (Float) getXRotMethod.invoke(cam);
                }
            } catch (Throwable ignored) {}
        }
        return Minecraft.getInstance().gameRenderer.getMainCamera().getXRot();
    }

    public static void lookAtCrosshairTarget() {
        resolve();
        if (getInstanceMethod == null) return;
        try {
            Object inst = getInstanceMethod.invoke(null);
            if (inst == null) return;
            if (lookAtCrosshairMethod == null) {
                lookAtCrosshairMethod = inst.getClass().getMethod("lookAtCrosshairTarget");
            }
            lookAtCrosshairMethod.invoke(inst);
        } catch (Throwable ignored) {}
    }

    public static void setLastMovedYRot(float value) {
        resolve();
        if (getInstanceMethod == null || lastMovedYRotField == null) return;
        try {
            Object inst = getInstanceMethod.invoke(null);
            if (inst == null) return;
            Object cam = getCameraMethod.invoke(inst);
            if (cam == null) return;
            lastMovedYRotField.setFloat(cam, value);
        } catch (Throwable ignored) {}
    }
}

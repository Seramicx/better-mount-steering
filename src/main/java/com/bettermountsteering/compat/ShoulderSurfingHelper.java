package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Reflection-only SSR access (SSR is an OPTIONAL dep here, unlike Mod 2/4).
 * When SSR is loaded, mount-rotate reads the camera yaw from SSR; otherwise
 * it falls back to {@code player.yRot} which equals the camera in coupled
 * mode.
 */
public final class ShoulderSurfingHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean resolved = false;
    private static Method getInstanceMethod = null;
    private static Method getCameraMethod = null;
    private static Method isShoulderSurfingMethod = null;
    private static Method getYRotMethod = null;
    private static Method getXRotMethod = null;

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
}

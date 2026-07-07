package com.bettermountsteering.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class GunModHelper {

    private GunModHelper() {}

    private static boolean resolved;
    private static Method scorchedGet;
    private static Field scorchedShooting;
    private static Method cgmGet;
    private static Field cgmShooting;

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> c = Class.forName("top.ribs.scguns.client.handler.ShootingHandler");
            scorchedGet = c.getMethod("get");
            scorchedShooting = c.getDeclaredField("shooting");
            scorchedShooting.setAccessible(true);
        } catch (Throwable ignored) {}
        try {
            Class<?> c = Class.forName("com.mrcrayfish.guns.client.handler.ShootingHandler");
            cgmGet = c.getMethod("get");
            cgmShooting = c.getDeclaredField("shooting");
            cgmShooting.setAccessible(true);
        } catch (Throwable ignored) {}
    }

    private static final long FIRE_LATCH_MS = 500L;
    private static long fireSignalMs = 0L;

    // Semi-auto guns release the shoot key the same tick they fire, so the shooting field never latches for a
    // single click. Arm this at fire() so the mount body still turns to the camera on one-shot weapons
    public static void signalFire() {
        fireSignalMs = System.currentTimeMillis();
    }

    public static boolean isGunFiring() {
        if (System.currentTimeMillis() - fireSignalMs < FIRE_LATCH_MS) return true;
        resolve();
        if (scorchedGet != null && scorchedShooting != null) {
            try {
                if (scorchedShooting.getBoolean(scorchedGet.invoke(null))) return true;
            } catch (Throwable ignored) {}
        }
        if (cgmGet != null && cgmShooting != null) {
            try {
                if (cgmShooting.getBoolean(cgmGet.invoke(null))) return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }
}

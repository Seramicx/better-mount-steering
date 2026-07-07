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

    public static boolean isGunFiring() {
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

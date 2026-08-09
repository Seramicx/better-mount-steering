package com.bettermountsteering.compat;

import java.lang.reflect.Field;

public final class SuperbWarfareHelper {

    private SuperbWarfareHelper() {}

    private static boolean resolved;
    private static Field zoomField;
    private static Field holdingFireKeyField;

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> handler = Class.forName("com.atsuishio.superbwarfare.event.ClientEventHandler");
            zoomField = handler.getField("zoom");
            holdingFireKeyField = handler.getField("holdingFireKey");
        } catch (Throwable ignored) {}
    }

    public static boolean isAimingOrFiring() {
        resolve();
        try {
            if (zoomField != null && zoomField.getBoolean(null)) return true;
            if (holdingFireKeyField != null && holdingFireKeyField.getBoolean(null)) return true;
        } catch (Throwable ignored) {}
        return false;
    }
}

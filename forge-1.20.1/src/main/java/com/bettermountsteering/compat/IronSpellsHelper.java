package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public final class IronSpellsHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean loaded;
    private static boolean loadedResolved = false;

    private IronSpellsHelper() {}

    public static boolean isLoaded() {
        if (!loadedResolved) {
            loaded = ModList.get().isLoaded("irons_spellbooks");
            loadedResolved = true;
        }
        return loaded;
    }

    private static Method isCastingMethod = null;
    private static Method castDurationRemainingMethod = null;
    private static boolean cmdResolved = false;

    private static void resolveCMD() {
        if (cmdResolved) return;
        cmdResolved = true;
        if (!isLoaded()) return;
        try {
            Class<?> cmd = Class.forName("io.redspace.ironsspellbooks.player.ClientMagicData");
            try {
                Method m = cmd.getMethod("isCasting");
                if (m.getReturnType() == boolean.class) isCastingMethod = m;
            } catch (NoSuchMethodException ignored) {}
            try {
                Method m = cmd.getMethod("getCastDurationRemaining");
                Class<?> r = m.getReturnType();
                if (r == int.class || r == long.class || r == float.class || r == double.class) {
                    castDurationRemainingMethod = m;
                }
            } catch (NoSuchMethodException ignored) {}
        } catch (Throwable t) {
            LOGGER.debug("Iron's Spells ClientMagicData reflection unavailable: {}", t.getMessage());
        }
    }

    public static boolean isCasting() {
        if (!isLoaded()) return false;
        resolveCMD();
        if (isCastingMethod != null) {
            try {
                if ((boolean) isCastingMethod.invoke(null)) return true;
            } catch (Throwable ignored) {}
        }
        if (castDurationRemainingMethod != null) {
            try {
                Object v = castDurationRemainingMethod.invoke(null);
                if (v instanceof Number && ((Number) v).doubleValue() > 0) return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private static Field activeCastKeymapField = null;
    private static Field quickCastKeymapsField = null;
    private static boolean keymapsResolved = false;

    private static void resolveKeymaps() {
        if (keymapsResolved) return;
        keymapsResolved = true;
        if (!isLoaded()) return;
        try {
            Class<?> km = Class.forName("io.redspace.ironsspellbooks.player.KeyMappings");
            activeCastKeymapField = km.getField("SPELLBOOK_CAST_ACTIVE_KEYMAP");
            quickCastKeymapsField = km.getField("QUICK_CAST_MAPPINGS");
        } catch (Throwable t) {
            LOGGER.debug("Iron's Spells keymap reflection unavailable: {}", t.getMessage());
        }
    }

    public static boolean anyCastKeymapDown() {
        if (!isLoaded()) return false;
        resolveKeymaps();
        try {
            if (activeCastKeymapField != null) {
                Object v = activeCastKeymapField.get(null);
                if (v instanceof KeyMapping km && km.isDown()) return true;
            }
            if (quickCastKeymapsField != null) {
                Object v = quickCastKeymapsField.get(null);
                if (v instanceof List<?> list) {
                    for (Object k : list) {
                        if (k instanceof KeyMapping km && km.isDown()) return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isIronsItem(net.minecraft.world.item.Item item) {
        if (!isLoaded() || item == null) return false;
        return item.getClass().getName().startsWith("io.redspace.ironsspellbooks");
    }

    private static final long CAST_LATCH_MS = 500L;
    private static long castSignalMs = 0L;

    // Instant casts (Firebolt) resolve within the tick and never set isCasting, so latch to hold the auto-turn
    public static void signalCast() {
        castSignalMs = System.currentTimeMillis();
    }

    public static boolean isCastLatchActive() {
        return (System.currentTimeMillis() - castSignalMs) < CAST_LATCH_MS;
    }
}

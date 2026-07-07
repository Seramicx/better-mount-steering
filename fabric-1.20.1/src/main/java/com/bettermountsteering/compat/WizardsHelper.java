package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public final class WizardsHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static Boolean spellEnginePresent;
    private static Method isCastingSpellMethod;
    private static boolean castMethodResolved;

    private static Field hotbarInstanceField;
    private static Field hotbarSlotsField;
    private static Method slotGetKeyBindingMethod;
    private static boolean hotbarResolved;

    private WizardsHelper() {}

    public static boolean isLoaded() {
        if (spellEnginePresent == null) {
            spellEnginePresent = FabricLoader.getInstance().isModLoaded("spell_engine");
        }
        return spellEnginePresent;
    }

    private static synchronized void resolveCastMethod() {
        if (castMethodResolved) return;
        castMethodResolved = true;
        if (!isLoaded()) return;
        try {
            Class<?> iface = Class.forName("net.spell_engine.internals.casting.SpellCasterClient");
            isCastingSpellMethod = iface.getMethod("isCastingSpell");
        } catch (Throwable t) {
            LOGGER.debug("WizardsHelper: isCastingSpell reflection unavailable: {}", t.toString());
        }
    }

    private static synchronized void resolveHotbar() {
        if (hotbarResolved) return;
        hotbarResolved = true;
        if (!isLoaded()) return;
        try {
            Class<?> hotbar = Class.forName("net.spell_engine.client.input.SpellHotbar");
            hotbarInstanceField = hotbar.getField("INSTANCE");
            hotbarSlotsField = hotbar.getField("slots");
            Class<?> slot = Class.forName("net.spell_engine.client.input.SpellHotbar$Slot");
            slotGetKeyBindingMethod = slot.getMethod("getKeyBinding", net.minecraft.client.Options.class);
        } catch (Throwable t) {
            LOGGER.debug("WizardsHelper: SpellHotbar reflection unavailable: {}", t.toString());
        }
    }

    private static boolean isCastKeyDown() {
        resolveHotbar();
        if (hotbarInstanceField == null || hotbarSlotsField == null || slotGetKeyBindingMethod == null) return false;
        try {
            Object instance = hotbarInstanceField.get(null);
            if (instance == null) return false;
            Object slots = hotbarSlotsField.get(instance);
            if (!(slots instanceof List<?> list)) return false;
            net.minecraft.client.Options options = Minecraft.getInstance().options;
            for (Object slot : list) {
                Object km = slotGetKeyBindingMethod.invoke(slot, options);
                if (km instanceof KeyMapping mapping && mapping.isDown()) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isCastingLive() {
        if (!isLoaded()) return false;
        resolveCastMethod();
        if (isCastingSpellMethod == null) return false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        try {
            Object v = isCastingSpellMethod.invoke(player);
            return v instanceof Boolean && (Boolean) v;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isCasting() {
        if (!isLoaded()) return false;
        return isCastingLive() || isCastKeyDown();
    }

    private static final long CAST_LATCH_MS = 500L;
    private static long castSignalMs = 0L;

    // Instant casts (Shadowstep etc.) resolve within the tick and never report isCastingSpell, so latch the turn
    public static void signalCast() {
        castSignalMs = System.currentTimeMillis();
    }

    public static boolean isCastLatchActive() {
        return (System.currentTimeMillis() - castSignalMs) < CAST_LATCH_MS;
    }
}

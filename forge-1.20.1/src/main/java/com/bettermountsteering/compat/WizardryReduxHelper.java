package com.bettermountsteering.compat;

import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class WizardryReduxHelper {

    private static final long CAST_LATCH_MS = 500L;

    private static boolean loaded;
    private static boolean loadedResolved = false;

    private static Class<?> castItemClass;
    private static Class<?> bombItemClass;
    private static boolean classesResolved;

    private static long castSignalMs = 0L;

    private WizardryReduxHelper() {}

    public static boolean isLoaded() {
        if (!loadedResolved) {
            loaded = ModList.get().isLoaded("ebwizardry");
            loadedResolved = true;
        }
        return loaded;
    }

    // Wands and scrolls share ICastItem; bombs are thrown straight out of Item#use from the player rotation
    public static boolean isCastItem(@Nullable Item item) {
        if (item == null || !isLoaded()) return false;
        resolveClasses();
        return (castItemClass != null && castItemClass.isInstance(item))
                || (bombItemClass != null && bombItemClass.isInstance(item));
    }

    // Uncharged instant spells resolve inside the use call and never leave the player using the item, so latch
    // the turn. Charged and continuous spells keep isUsingItem up on their own
    public static void signalCast() {
        castSignalMs = System.currentTimeMillis();
    }

    public static boolean isCastLatchActive() {
        return (System.currentTimeMillis() - castSignalMs) < CAST_LATCH_MS;
    }

    private static void resolveClasses() {
        if (classesResolved) return;
        classesResolved = true;
        try {
            castItemClass = Class.forName("com.binaris.wizardry.api.content.item.ICastItem");
        } catch (ClassNotFoundException ignored) {}
        try {
            bombItemClass = Class.forName("com.binaris.wizardry.content.item.BombItem");
        } catch (ClassNotFoundException ignored) {}
    }
}

package com.bettermountsteering.compat;

import java.lang.reflect.Method;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class MusketModHelper {

    private MusketModHelper() {}

    private static boolean resolved;
    private static Class<?> gunItem;
    private static Method isLoaded;

    private static void resolve() {
        if (resolved) return;
        resolved = true;

        try {
            gunItem = Class.forName("ewewukek.musketmod.GunItem");
            isLoaded = gunItem.getMethod("isLoaded", ItemStack.class);
        } catch (Throwable ignored) {}
    }

    public static boolean isHoldingLoadedGun(LivingEntity entity) {
        resolve();
        if (isLoaded == null || entity == null) return false;
        return isLoadedGun(entity.getItemInHand(InteractionHand.MAIN_HAND))
            || isLoadedGun(entity.getItemInHand(InteractionHand.OFF_HAND));
    }

    private static boolean isLoadedGun(ItemStack stack) {
        if (!gunItem.isInstance(stack.getItem())) return false;
        try {
            return (Boolean) isLoaded.invoke(null, stack);
        } catch (Throwable ignored) {
            return false;
        }
    }
}

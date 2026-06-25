package com.bettermountsteering.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public final class TaczHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean loaded;
    private static boolean loadedResolved = false;

    private static Method iGunGetOrNull = null;
    private static Method gunOperatorFrom = null;
    private static Method isAimMethod = null;
    private static Method shootCooldownMethod = null;
    private static boolean resolved = false;

    private TaczHelper() {}

    public static boolean isLoaded() {
        if (!loadedResolved) {
            loaded = ModList.get().isLoaded("tacz");
            loadedResolved = true;
        }
        return loaded;
    }

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        if (!isLoaded()) return;
        try {
            Class<?> iGun = Class.forName("com.tacz.guns.api.item.IGun");
            iGunGetOrNull = iGun.getMethod("getIGunOrNull", ItemStack.class);

            Class<?> op = Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator");
            gunOperatorFrom = op.getMethod("fromLocalPlayer", LocalPlayer.class);
            isAimMethod = op.getMethod("isAim");
            shootCooldownMethod = op.getMethod("getClientShootCoolDown");
        } catch (Throwable t) {
            LOGGER.debug("TaCZ compat reflection unavailable: {}", t.toString());
        }
    }

    public static boolean isHoldingGun(LocalPlayer player) {
        if (!isLoaded() || player == null) return false;
        resolve();
        if (iGunGetOrNull == null) return false;
        try {
            return iGunGetOrNull.invoke(null, player.getMainHandItem()) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isAimingOrFiring() {
        if (!isLoaded()) return false;
        resolve();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return false;
        if (!isHoldingGun(player)) return false;
        if (mc.options.keyAttack.isDown()) return true;
        if (gunOperatorFrom == null) return false;
        try {
            Object op = gunOperatorFrom.invoke(null, player);
            if (op == null) return false;
            if (isAimMethod != null) {
                Object aim = isAimMethod.invoke(op);
                if (aim instanceof Boolean && (Boolean) aim) return true;
            }
            if (shootCooldownMethod != null) {
                Object cd = shootCooldownMethod.invoke(op);
                if (cd instanceof Number && ((Number) cd).longValue() > 0L) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }
}

package com.bettermountsteering.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Bolt velocity comes from getViewVector, which LivingEntity resolves off yHeadRot, but the rot packet only
// carries yRot, so a mounted shot would fly along whatever head yaw the server had before the snap
@Mixin(CrossbowItem.class)
public abstract class MixinCrossbowMountedHeadYaw {

    @Inject(method = "performShooting", at = @At("HEAD"))
    private static void bettermountsteering$alignHeadToShootYaw(Level level, LivingEntity shooter,
                                                                InteractionHand hand, ItemStack stack,
                                                                float velocity, float inaccuracy,
                                                                CallbackInfo ci) {
        if (level.isClientSide || !(shooter instanceof Player) || !shooter.isPassenger()) return;
        shooter.yHeadRot = shooter.getYRot();
    }
}

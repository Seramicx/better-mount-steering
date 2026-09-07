package com.bettermountsteering.mixin;

import com.bettermountsteering.compat.MusketModHelper;
import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMusketMountFaceCamera {

    @Inject(method = "useItem", at = @At("HEAD"))
    private void bettermountsteering$faceCameraForMusket(Player player, InteractionHand hand,
                                                         CallbackInfoReturnable<?> cir) {
        LocalPlayer local = Minecraft.getInstance().player;
        if (local == null || player != local) return;
        if (!MusketModHelper.isHoldingLoadedGun(local)) return;

        MountSteeringHandler.snapToCameraNow(local);
    }
}

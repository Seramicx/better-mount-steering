package com.bettermountsteering.mixin;

import com.bettermountsteering.handler.MountSteeringHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinCrossbowMountFaceCamera {

    @Inject(method = "useItem", at = @At("HEAD"))
    private void bettermountsteering$faceCameraForCrossbow(Player player, InteractionHand hand,
                                                           CallbackInfoReturnable<?> cir) {
        LocalPlayer local = Minecraft.getInstance().player;
        if (local == null || player != local) return;
        if (!(player.getItemInHand(hand).getItem() instanceof CrossbowItem)) return;

        MountSteeringHandler.snapToCameraNow(local);
    }
}

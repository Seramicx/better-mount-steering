package com.bettermountsteering.mixin;

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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.core.component.DataComponents;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinCrossbowMountFaceCamera {

    @Inject(method = "useItem", at = @At("HEAD"))
    private void bettermountsteering$faceCameraForUse(Player player, InteractionHand hand,
                                                           CallbackInfoReturnable<?> cir) {
        LocalPlayer local = Minecraft.getInstance().player;
        if (local == null || player != local) return;
        // Guns and staffs that fire on right click send a use packet without a rotation, same as a loaded crossbow
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || stack.getItem() instanceof BlockItem || stack.has(DataComponents.FOOD)) return;
        UseAnim anim = stack.getUseAnimation();
        if (anim == UseAnim.EAT || anim == UseAnim.DRINK) return;

        float yaw = MountSteeringHandler.snapToCameraNow(local);
        // useItem sends the client rotation again before the use packet, which would undo the snap
        if (!Float.isNaN(yaw)) local.setYRot(yaw);
    }
}

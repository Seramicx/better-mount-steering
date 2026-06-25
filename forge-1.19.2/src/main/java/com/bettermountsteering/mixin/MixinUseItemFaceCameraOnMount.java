package com.bettermountsteering.mixin;

import com.bettermountsteering.compat.IronSpellsHelper;
import com.bettermountsteering.compat.ShoulderSurfingHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinUseItemFaceCameraOnMount {

    @Inject(method = "useItem", at = @At("HEAD"))
    private void bettermountsteering$faceCameraBeforeCast(Player player, InteractionHand hand,
                                                          CallbackInfoReturnable<?> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player) return;
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON) return;
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof Mob mob) || mob.getControllingPassenger() != player) return;
        if (!IronSpellsHelper.isIronsItem(player.getItemInHand(hand).getItem())) return;

        // Instant casts (Firebolt) fire without setting isUsingItem, so latch here to hold the aim through them.
        IronSpellsHelper.signalCast();
        if (ShoulderSurfingHelper.isShoulderSurfingActive()) {
            player.setYRot(ShoulderSurfingHelper.getCameraYaw());
            player.setXRot(ShoulderSurfingHelper.getCameraXRot());
        }
    }
}

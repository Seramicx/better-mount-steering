package com.bettermountsteering.mixin;

import com.bettermountsteering.compat.WizardryReduxHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Uncharged instant spells and bombs resolve inside the use call, so the mount auto-turn's combat check misses
// them. Arm a short latch here so isCombatActive holds long enough to turn the body
@Mixin(MultiPlayerGameMode.class)
public abstract class MixinWizardryCastTurn {

    @Inject(method = "useItem", at = @At("HEAD"))
    private void bettermountsteering$latchWizardryCast(Player player, InteractionHand hand,
                                                       CallbackInfoReturnable<?> cir) {
        if (player != Minecraft.getInstance().player) return;
        if (!WizardryReduxHelper.isCastItem(player.getItemInHand(hand).getItem())) return;
        WizardryReduxHelper.signalCast();
    }
}

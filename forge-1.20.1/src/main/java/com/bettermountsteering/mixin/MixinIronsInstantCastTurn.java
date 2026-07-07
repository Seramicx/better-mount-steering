package com.bettermountsteering.mixin;

import com.bettermountsteering.compat.IronSpellsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Instant casts (Firebolt right-click) resolve the same tick and never set isCasting, so the mount auto-turn's
// combat check misses them. Arm a short latch here so isCombatActive holds long enough to turn the body
@Mixin(MultiPlayerGameMode.class)
public abstract class MixinIronsInstantCastTurn {

    @Inject(method = "useItem", at = @At("HEAD"))
    private void bettermountsteering$latchInstantCast(Player player, InteractionHand hand,
                                                      CallbackInfoReturnable<?> cir) {
        if (player != Minecraft.getInstance().player) return;
        if (!IronSpellsHelper.isIronsItem(player.getItemInHand(hand).getItem())) return;
        IronSpellsHelper.signalCast();
    }
}

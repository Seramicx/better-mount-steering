package com.bettermountsteering.mixin;

import com.bettermountsteering.compat.IronSpellsHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Right-click Firebolt casts via ServerboundCast, not useItem, and finishes within a tick so isCasting is never
// true for the mount auto-face. Arm the latch when the cast packet is built so isCombatActive holds long enough
@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.network.ServerboundCast", remap = false)
public abstract class MixinIronsCastLatch {

    @Inject(method = "<init>()V", at = @At("TAIL"), require = 0, remap = false)
    private void bettermountsteering$latchCast(CallbackInfo ci) {
        IronSpellsHelper.signalCast();
    }
}

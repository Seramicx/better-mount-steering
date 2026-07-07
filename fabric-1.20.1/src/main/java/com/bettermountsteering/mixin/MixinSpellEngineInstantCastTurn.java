package com.bettermountsteering.mixin;

import com.bettermountsteering.compat.WizardsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Spell Engine instant casts resolve the same tick startSpellCast runs and never report isCastingSpell, so the
// mount auto-turn's combat check misses them. Arm a short latch so isCombatActive holds long enough to turn
@Pseudo
@Mixin(LocalPlayer.class)
public abstract class MixinSpellEngineInstantCastTurn {

    @Inject(method = "startSpellCast", at = @At("HEAD"), require = 0, remap = false)
    private void bettermountsteering$latchInstantCast(CallbackInfoReturnable<Object> cir) {
        if ((Object) this != Minecraft.getInstance().player) return;
        WizardsHelper.signalCast();
    }
}

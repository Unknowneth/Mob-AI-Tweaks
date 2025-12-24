package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.mob.SpellcastingIllagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpellcastingIllagerEntity.class)
public abstract class SpellCastingIllagerEntityAccessor {
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/IllagerEntity;tick()V", shift = At.Shift.AFTER), cancellable = true)
    private void fixParticlesOnClientSide(CallbackInfo ci) {
        if(this instanceof SpecialAttacksInterface s && s.attackType().equals("FIREBALL") && s.getSpecialCooldown() < 0) ci.cancel();
    }
    @Inject(method = "isSpellcasting", at = @At("HEAD"), cancellable = true)
    private void fixItemRenderer(CallbackInfoReturnable<Boolean> cir) {
        SpellcastingIllagerEntity me = (SpellcastingIllagerEntity)(Object)this;
        if(me.getWorld().isClient() && me instanceof SpecialAttacksInterface s && s.attackType().equals("FIREBALL") && s.getSpecialCooldown() < 0) cir.setReturnValue(true);
    }
}

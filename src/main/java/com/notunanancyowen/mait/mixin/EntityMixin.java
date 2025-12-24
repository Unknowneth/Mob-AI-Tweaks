package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract @Nullable Entity getVehicle();
    @Inject(method = "isSprinting", at = @At("TAIL"), cancellable = true)
    private void overrideSprintingForSpecialMobs(CallbackInfoReturnable<Boolean> cir) {
        if(this instanceof SpecialAttacksInterface s) if(s.attackType().equals("GRENADE") && s.getSpecialCooldown() != 0) cir.setReturnValue(false);
        else if(s.attackType().equals("DODGE") && s.getSpecialCooldown() != 0) cir.setReturnValue(true);
    }
}

package com.notunanancyowen.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinBrain.class)
public abstract class PiglinBrainMixin {
    @Inject(method = "isHoldingCrossbow", at = @At("HEAD"), cancellable = true)
    private static void isHoldingCrossbowFix(LivingEntity piglin, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(piglin.getMainHandStack().getItem() instanceof CrossbowItem || piglin.getOffHandStack().getItem() instanceof CrossbowItem);
    }
}

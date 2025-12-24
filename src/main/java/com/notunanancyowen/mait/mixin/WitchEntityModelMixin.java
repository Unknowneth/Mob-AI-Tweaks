package com.notunanancyowen.mait.mixin;

import net.minecraft.client.render.entity.model.WitchEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.WitchEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(WitchEntityModel.class)
public abstract class WitchEntityModelMixin {
    @Inject(method = "setAngles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;setPivot(FFF)V", ordinal = 1), cancellable = true)
    private void doNotRaiseNose(Entity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if(entity instanceof WitchEntity w && !w.isDrinking()) ci.cancel();
    }
}

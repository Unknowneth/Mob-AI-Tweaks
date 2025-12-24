package com.notunanancyowen.mait.mixin;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.entity.mob.PhantomEntity$PhantomMoveControl")
public abstract class PhantomMoveControlMixin {
    @Unique private PhantomEntity phantom;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void assignPhantom(PhantomEntity phantomEntity, MobEntity owner, CallbackInfo ci) {
        phantom = phantomEntity;
    }
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void overrideMovement(CallbackInfo ci) {
        if(phantom.handSwingProgress == 1F) {
            phantom.setYaw((float)(Math.atan2(phantom.getVelocity().getZ(), phantom.getVelocity().getX()) * 180.0 / Math.PI - 90.0));
            phantom.setPitch((float)(Math.atan2(phantom.getVelocity().getY(), phantom.getVelocity().horizontalLength()) * 180.0 / Math.PI));
            ci.cancel();
        }
    }
}

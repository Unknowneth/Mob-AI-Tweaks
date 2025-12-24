package com.notunanancyowen.mait.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AllayEntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.passive.AllayEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AllayEntityModel.class)
public abstract class AllayEntityModelMixin extends SinglePartEntityModel<AllayEntity> {
    @Shadow @Final private ModelPart rightArm;
    @Shadow @Final private ModelPart leftArm;
    @Shadow @Final private ModelPart head;
    @Shadow @Final private ModelPart root;
    @Shadow @Final private ModelPart body;
    @Inject(method = "setAngles(Lnet/minecraft/entity/passive/AllayEntity;FFFFF)V", at = @At("TAIL"))
    private void allayAnimationTweaks(AllayEntity allayEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if(!allayEntity.isAttacking()) return;
        float aimRot = head.pitch - body.pitch - root.pitch - (float)(Math.PI / 2d - Math.sin(handSwingProgress * Math.PI) * Math.PI * 0.4d);
        rightArm.roll = 0;
        rightArm.yaw *= -1.1f;
        rightArm.pitch = aimRot;
        leftArm.roll = 0;
        leftArm.yaw *= -1.1f;
        leftArm.pitch = aimRot;
    }
}

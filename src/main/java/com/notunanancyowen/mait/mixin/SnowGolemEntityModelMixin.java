package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.render.entity.model.SnowGolemEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SnowGolemEntityModel.class)
public abstract class SnowGolemEntityModelMixin<T extends Entity> extends SinglePartEntityModel<T> {
    @Shadow @Final private ModelPart rightArm;
    @Shadow @Final private ModelPart leftArm;
    @Inject(method = "setAngles", at = @At("TAIL"))
    private void addArmAnimations(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        rightArm.roll = -(float)Math.PI / 3F;
        leftArm.roll = (float)Math.PI / 3F;
        if(entity instanceof MobEntity m && m instanceof SpecialAttacksInterface s) if(m.isLeftHanded()) {
            float h = (s.getSpecialCooldown() > 0 ? MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(!MinecraftClient.getInstance().isPaused()) : 0F);
            leftArm.yaw = -(float)Math.PI * 2F * (0.1F * (h + (10 - s.getSpecialCooldown())) - 0.5F) - (float)Math.PI;
            leftArm.roll = -(float)Math.PI / 3F * (1.5F + (float)Math.cos(leftArm.yaw) * 0.5F) + (float)Math.PI;
            float d = 1.0F + 0.2F * (float)Math.abs(Math.sin(leftArm.yaw));
            leftArm.pivotX *= d;
            leftArm.pivotZ *= d;
            if(s.getSpecialCooldown() == 9) rightArm.roll = -h * (float)Math.PI / 3F;
        }
        else {
            float h = (s.getSpecialCooldown() > 0 ? MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(!MinecraftClient.getInstance().isPaused()) : 0F);
            rightArm.yaw = (float)Math.PI * 2F * (0.1F * (h + (10 - s.getSpecialCooldown())) + 0.5F);
            rightArm.roll = -(float)Math.PI / 3F * (1.5F + (float)Math.cos(rightArm.yaw) * 0.5F);
            float d = 1.0F + 0.2F * (float)Math.abs(Math.sin(rightArm.yaw));
            rightArm.pivotX *= d;
            rightArm.pivotZ *= d;
            if(s.getSpecialCooldown() == 9) leftArm.roll = h * (float)Math.PI / 3F;
        }
    }
}

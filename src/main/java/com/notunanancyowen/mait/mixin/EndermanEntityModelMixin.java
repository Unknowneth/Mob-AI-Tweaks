package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EndermanEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndermanEntityModel.class)
public abstract class EndermanEntityModelMixin<T extends EndermanEntity> extends BipedEntityModel<T> {
    @Shadow public boolean carryingBlock;
    EndermanEntityModelMixin(ModelPart root) {
        super(root);
    }
    @Inject(method = "setAngles(Lnet/minecraft/entity/Entity;FFFFF)V", at = @At("TAIL"))
    private void coolSwingAnimations(Entity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("enderman_rework_attack_animation")) return;
        if(entity instanceof EndermanEntity e && e instanceof SpecialAttacksInterface s) if(e.isAttacking()) {
            float specialAttackProgress = (s.getSpecialCooldown() % 20 + MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(!MinecraftClient.getInstance().isPaused())) * 0.1F;
            if(specialAttackProgress > 1F) specialAttackProgress = 1F;
            else specialAttackProgress = (float)Math.sqrt(specialAttackProgress);
            if(handSwingProgress > 0F) e.setYaw(e.getHeadYaw());
            if(!carryingBlock) switch(s.getSpecialCooldown() / 20) {
                case 0:
                    if(e.isLeftHanded()) {
                        leftArm.roll *= 1F - specialAttackProgress;
                        leftArm.pitch *= 1F - specialAttackProgress;
                        leftArm.roll -= (1F - handSwingProgress) * specialAttackProgress;
                        leftArm.pitch -= (float)(Math.sin(handSwingProgress * Math.PI) * 2) * specialAttackProgress;
                    }
                    else {
                        rightArm.roll *= 1F - specialAttackProgress;
                        rightArm.pitch *= 1F - specialAttackProgress;
                        rightArm.roll += (1F - handSwingProgress) * specialAttackProgress;
                        rightArm.pitch -= (float)(Math.sin(handSwingProgress * Math.PI) * 2) * specialAttackProgress;
                    }
                    break;
                case 1:
                    if(e.isLeftHanded()) {
                        leftArm.yaw *= 1F - specialAttackProgress;
                        leftArm.roll *= 1F - specialAttackProgress;
                        leftArm.pitch *= 1F - specialAttackProgress;
                        leftArm.yaw -= handSwingProgress * specialAttackProgress;
                        leftArm.roll += (1F + handSwingProgress) * specialAttackProgress;
                        leftArm.pitch -= (float)(Math.sin(handSwingProgress * Math.PI) * 1.7F + 0.3F) * specialAttackProgress;
                    }
                    else {
                        rightArm.yaw *= 1F - specialAttackProgress;
                        rightArm.roll *= 1F - specialAttackProgress;
                        rightArm.pitch *= 1F - specialAttackProgress;
                        rightArm.yaw += handSwingProgress * specialAttackProgress;
                        rightArm.roll -= (1F + handSwingProgress) * specialAttackProgress;
                        rightArm.pitch -= (float)(Math.sin(handSwingProgress * Math.PI) * 1.7F + 0.3F) * specialAttackProgress;
                    }
                    break;
                case 2:
                    leftArm.roll *= 1F - specialAttackProgress;
                    rightArm.roll *= 1F - specialAttackProgress;
                    leftArm.roll -= 0.2F * specialAttackProgress;
                    rightArm.roll += 0.2F * specialAttackProgress;
                    rightArm.pitch *= specialAttackProgress;
                    leftArm.pitch *= specialAttackProgress;
                    rightArm.pitch += 0.4F * specialAttackProgress;
                    leftArm.pitch += 0.4F * specialAttackProgress;
                    rightArm.yaw *= 1F - specialAttackProgress;
                    leftArm.yaw *= 1F - specialAttackProgress;
                    rightArm.yaw -= 0.4F * specialAttackProgress;
                    leftArm.yaw += 0.4F * specialAttackProgress;
                    if(e.isLeftHanded()) {
                        leftLeg.pitch *= 1F - specialAttackProgress;
                        leftLeg.roll *= 1F - specialAttackProgress;
                        leftLeg.pitch += (0.2F - (float)Math.sin(handSwingProgress * Math.PI) * 2F) * specialAttackProgress;
                        leftLeg.roll -= (0.4F * (1F - handSwingProgress)) * specialAttackProgress;
                    }
                    else {
                        rightLeg.pitch *= 1F - specialAttackProgress;
                        rightLeg.roll *= 1F - specialAttackProgress;
                        rightLeg.pitch += (0.2F - (float)Math.sin(handSwingProgress * Math.PI) * 2F) * specialAttackProgress;
                        rightLeg.roll += (0.4F * (1F - handSwingProgress)) * specialAttackProgress;
                    }
                    body.yaw = 0F;
                    body.pitch *= 1F - specialAttackProgress;
                    body.pitch += (0.1F - handSwingProgress * 0.2F) * specialAttackProgress;
                    break;
            }
            else if(e.isLeftHanded()) {
                leftLeg.pitch *= 1F - specialAttackProgress;
                leftLeg.roll *= 1F - specialAttackProgress;
                leftLeg.pitch += (0.2F - (float)Math.sin(handSwingProgress * Math.PI) * 2F) * specialAttackProgress;
                leftLeg.roll -= (0.4F * (1F - handSwingProgress)) * specialAttackProgress;
            }
            else {
                rightLeg.pitch *= 1F - specialAttackProgress;
                rightLeg.roll *= 1F - specialAttackProgress;
                rightLeg.pitch += (0.2F - (float)Math.sin(handSwingProgress * Math.PI) * 2F) * specialAttackProgress;
                rightLeg.roll += (0.4F * (1F - handSwingProgress)) * specialAttackProgress;
            }
        }
    }
}

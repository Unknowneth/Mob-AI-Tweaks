package com.notunanancyowen.mixin;

import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.CrossbowPosing;
import net.minecraft.client.render.entity.model.SkeletonEntityModel;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.BowItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(value = SkeletonEntityModel.class)
public abstract class SkeletonEntityModelMixin<T extends MobEntity> extends BipedEntityModel<T> {
    @Unique private final static HashMap<Integer, Float> specialAttackTime = new HashMap<>();
    private SkeletonEntityModelMixin(ModelPart modelPart) {
        super(modelPart);
    }
    @Inject(method = "animateModel(Lnet/minecraft/entity/mob/MobEntity;FFF)V", at = @At("HEAD"), cancellable = true)
    private void animateThis(T mobEntity, float f, float g, float h, CallbackInfo ci) {
        rightArmPose = ArmPose.EMPTY;
        leftArmPose = ArmPose.EMPTY;
        if(mobEntity.isAttacking()) if(mobEntity.getStackInHand(Hand.MAIN_HAND).getItem() instanceof BowItem) {
            if(mobEntity.getMainArm().equals(Arm.RIGHT)) rightArmPose = ArmPose.BOW_AND_ARROW;
            else leftArmPose = ArmPose.BOW_AND_ARROW;
        }
        else if(mobEntity.getStackInHand(Hand.OFF_HAND).getItem() instanceof BowItem) {
            if(mobEntity.getMainArm().equals(Arm.RIGHT)) leftArmPose = ArmPose.BOW_AND_ARROW;
            else rightArmPose = ArmPose.BOW_AND_ARROW;
        }
        super.animateModel(mobEntity, f, g, h);
        ci.cancel();
    }
    @Inject(method = "setAngles(Lnet/minecraft/entity/mob/MobEntity;FFFFF)V", at = @At("HEAD"), cancellable = true)
    private void fixAngles(T mobEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        super.setAngles(mobEntity, f, g, h, i, j);
        boolean hasBow = mobEntity.getStackInHand(Hand.MAIN_HAND).getItem() instanceof BowItem || mobEntity.getStackInHand(Hand.OFF_HAND).getItem() instanceof BowItem;
        int myId = mobEntity.getId();
        if(mobEntity.isAttacking()) if(!hasBow) {
            float k = MathHelper.sin(handSwingProgress * (float) Math.PI);
            float l = MathHelper.sin((1.0F - (1.0F - handSwingProgress) * (1.0F - handSwingProgress)) * (float) Math.PI);
            rightArm.roll = 0.0F;
            leftArm.roll = 0.0F;
            rightArm.yaw = -(0.1F - k * 0.6F);
            leftArm.yaw = 0.1F - k * 0.6F;
            rightArm.pitch = (float) (-Math.PI / 2) + k * 1.2F - l * 0.4F;
            leftArm.pitch = (float) (-Math.PI / 2) + k * 1.2F - l * 0.4F;
            CrossbowPosing.swingArms(rightArm, leftArm, h);
        }
        if(!hasBow && mobEntity instanceof SpecialAttacksInterface specialMob && specialMob.getSpecialCooldown() > 0) {
            if(!specialAttackTime.containsKey(myId)) specialAttackTime.put(myId, 0F);
            float specialAttackProgress = specialMob.getSpecialCooldown() * 0.1F;
            float originalValueOfThisThing = specialAttackTime.getOrDefault(myId, 0F);
            if(!MinecraftClient.getInstance().isPaused()) specialAttackTime.replace(myId, originalValueOfThisThing + (specialAttackProgress - originalValueOfThisThing) * 0.1F);
            specialAttackProgress = specialAttackTime.getOrDefault(myId, 0F);
            if(specialAttackProgress > 2f) {
                specialAttackProgress = 1f - (specialAttackProgress - 2f);
                rightArm.pitch += specialAttackProgress * 2f;
                leftArm.pitch += specialAttackProgress * 2f;
                body.pitch += specialAttackProgress * 0.3F;
            }
            else if(specialAttackProgress > 1F) {
                rightArm.pitch--;
                leftArm.pitch--;
                body.pitch -= 0.3F;
                specialAttackProgress--;
                specialAttackProgress *= specialAttackProgress * specialAttackProgress;
                rightArm.pitch += specialAttackProgress * 3F;
                leftArm.pitch += specialAttackProgress * 3F;
                body.pitch += specialAttackProgress * 0.6F;
            }
            else {
                specialAttackProgress *= specialAttackProgress;
                rightArm.pitch -= specialAttackProgress;
                leftArm.pitch -= specialAttackProgress;
                body.pitch -= specialAttackProgress * 0.3F;
            }
        }
        else specialAttackTime.remove(myId);
        ci.cancel();
    }
}

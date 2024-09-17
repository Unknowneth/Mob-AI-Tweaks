package com.notunanancyowen.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.CrossbowPosing;
import net.minecraft.client.render.entity.model.PiglinEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PiglinEntityModel.class)
public abstract class PiglinEntityModelMixin<T extends MobEntity> extends PlayerEntityModel<T> {
    PiglinEntityModelMixin(ModelPart root, boolean thinArms) {
        super(root, thinArms);
    }
    @Inject(method = "setAngles(Lnet/minecraft/entity/mob/MobEntity;FFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/CrossbowPosing;meleeAttack(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/model/ModelPart;ZFF)V"), cancellable = true)
    private void crossbowAnim(T mobEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if(mobEntity instanceof ZombifiedPiglinEntity zombifiedPiglin) if((zombifiedPiglin.getMainHandStack() != null && zombifiedPiglin.getMainHandStack().getItem() instanceof CrossbowItem) || (zombifiedPiglin.getOffHandStack() != null && zombifiedPiglin.getOffHandStack().getItem() instanceof CrossbowItem)) {
            ModelPart zombieMainArm = getArm(zombifiedPiglin.getMainArm());
            ModelPart zombieOtherArm = getArm(zombifiedPiglin.getMainArm().getOpposite());
            boolean notActuallyLeftHanded = zombifiedPiglin.getMainHandStack() != null && !(zombifiedPiglin.getMainHandStack().getItem() instanceof CrossbowItem) && zombifiedPiglin.getOffHandStack() != null && zombifiedPiglin.getOffHandStack().getItem() instanceof CrossbowItem;
            if(zombifiedPiglin.isLeftHanded()) {
                zombieMainArm = zombieOtherArm;
                zombieOtherArm = getArm(zombifiedPiglin.getMainArm());
                notActuallyLeftHanded = !notActuallyLeftHanded;
            }
            if(zombifiedPiglin.isUsingItem()) CrossbowPosing.charge(zombieMainArm, zombieOtherArm, zombifiedPiglin, !notActuallyLeftHanded);
            else CrossbowPosing.hold(zombieMainArm, zombieOtherArm, getHead(), !notActuallyLeftHanded);
            leftPants.copyTransform(leftLeg);
            rightPants.copyTransform(rightLeg);
            leftSleeve.copyTransform(leftArm);
            rightSleeve.copyTransform(rightArm);
            jacket.copyTransform(body);
            hat.copyTransform(head);
            ci.cancel();
        }
    }
}

package com.notunanancyowen.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.CrossbowPosing;
import net.minecraft.client.render.entity.model.ZombieVillagerEntityModel;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieVillagerEntityModel.class)
public abstract class ZombieVillagerEntityModelMixin<T extends ZombieEntity> extends BipedEntityModel<T> {
    ZombieVillagerEntityModelMixin(ModelPart root) {
        super(root);
    }
    @Inject(method = "setAngles(Lnet/minecraft/entity/mob/ZombieEntity;FFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/CrossbowPosing;meleeAttack(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/model/ModelPart;ZFF)V"), cancellable = true)
    private void crossbowAnim(T hostileEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if((hostileEntity.getMainHandStack() != null && hostileEntity.getMainHandStack().getItem() instanceof CrossbowItem) || (hostileEntity.getOffHandStack() != null && hostileEntity.getOffHandStack().getItem() instanceof CrossbowItem)) {
            ModelPart zombieMainArm = getArm(hostileEntity.getMainArm());
            ModelPart zombieOtherArm = getArm(hostileEntity.getMainArm().getOpposite());
            boolean notActuallyLeftHanded = hostileEntity.getMainHandStack() != null && !(hostileEntity.getMainHandStack().getItem() instanceof CrossbowItem) && hostileEntity.getOffHandStack() != null && hostileEntity.getOffHandStack().getItem() instanceof CrossbowItem;
            if(hostileEntity.isLeftHanded()) {
                zombieMainArm = zombieOtherArm;
                zombieOtherArm = getArm(hostileEntity.getMainArm());
                notActuallyLeftHanded = !notActuallyLeftHanded;
            }
            if(hostileEntity.isUsingItem()) CrossbowPosing.charge(zombieMainArm, zombieOtherArm, hostileEntity, !notActuallyLeftHanded);
            else CrossbowPosing.hold(zombieMainArm, zombieOtherArm, getHead(), !notActuallyLeftHanded);
            ci.cancel();
        }
    }
}

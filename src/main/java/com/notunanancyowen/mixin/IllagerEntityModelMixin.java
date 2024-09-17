package com.notunanancyowen.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(IllagerEntityModel.class)
public abstract class IllagerEntityModelMixin<T extends IllagerEntity> {
    @ModifyVariable(method = "setAngles(Lnet/minecraft/entity/mob/IllagerEntity;FFFFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private T getIllagerEntity(T illager, @Share("isNotRightHanded") LocalBooleanRef localBooleanRef) {
        boolean whichHandTho = !illager.isLeftHanded();
        if(illager.getMainHandStack() != null && !(illager.getMainHandStack().getItem() instanceof CrossbowItem) && illager.getOffHandStack() != null && illager.getOffHandStack().getItem() instanceof CrossbowItem) whichHandTho = !whichHandTho;
        localBooleanRef.set(whichHandTho);
        return illager;
    }
    @ModifyArg(method = "setAngles(Lnet/minecraft/entity/mob/IllagerEntity;FFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/CrossbowPosing;charge(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/entity/LivingEntity;Z)V"), index = 3)
    private boolean isActuallyLeftHanded1(boolean rightArmed, @Share("isNotRightHanded") LocalBooleanRef localBooleanRef) {
        return localBooleanRef != null && localBooleanRef.get();
    }
    @ModifyArg(method = "setAngles(Lnet/minecraft/entity/mob/IllagerEntity;FFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/CrossbowPosing;hold(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/model/ModelPart;Z)V"), index = 3)
    private boolean isActuallyLeftHanded2(boolean rightArmed, @Share("isNotRightHanded") LocalBooleanRef localBooleanRef) {
        return localBooleanRef != null && localBooleanRef.get();
    }
}

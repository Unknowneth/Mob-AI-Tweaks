package com.notunanancyowen.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.DrownedEntityModel;
import net.minecraft.client.render.entity.model.ZombieEntityModel;
import net.minecraft.entity.mob.ZombieEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrownedEntityModel.class)
public abstract class DrownedEntityModelMixin<T extends ZombieEntity> extends ZombieEntityModel<T> {
    DrownedEntityModelMixin(ModelPart modelPart) {
        super(modelPart);
    }
    @Inject(method = "setAngles(Lnet/minecraft/entity/mob/ZombieEntity;FFFFF)V", at = @At("TAIL"))
    private void fixHeadRotation(T zombieEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if(leaningPitch > 0F) head.pitch -= leaningPitch;
    }
}

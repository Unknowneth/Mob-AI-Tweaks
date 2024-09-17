package com.notunanancyowen.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BodyControl.class)
public abstract class BodyControlMixin {
    @Shadow @Final private MobEntity entity;
    @ModifyReturnValue(method = "isMoving", at= @At("RETURN"))
    private boolean usingItemCountsAsMoving(boolean original) {
        return original || entity.isUsingItem();
    }
}

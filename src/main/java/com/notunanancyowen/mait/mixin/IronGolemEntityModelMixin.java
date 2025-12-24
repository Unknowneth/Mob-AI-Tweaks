package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.IronGolemEntityModel;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronGolemEntityModel.class)
public abstract class IronGolemEntityModelMixin {
    @Shadow @Final private ModelPart rightArm;
    @Shadow @Final private ModelPart leftArm;
    @Inject(method = "animateModel(Lnet/minecraft/entity/passive/IronGolemEntity;FFF)V", at = @At("HEAD"), cancellable = true)
    private void dashAttack(IronGolemEntity ironGolemEntity, float f, float g, float h, CallbackInfo ci) {
        if(ironGolemEntity instanceof SpecialAttacksInterface s && ((s.getSpecialCooldown() > 60 && s.getSpecialCooldown() < 80) || s.getSpecialCooldown() < 0)) {
            int i = s.getSpecialCooldown();
            if(i == 61) rightArm.pitch = leftArm.pitch = h;
            else if(i == -1F) rightArm.pitch = leftArm.pitch = 1F - h - 2.0F + 1.5F * MathHelper.wrap(i - h, 10.0F);
            else rightArm.pitch = leftArm.pitch = 1F;
            ci.cancel();
        }
    }
}

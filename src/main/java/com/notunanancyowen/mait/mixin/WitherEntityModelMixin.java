package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.dataholders.WitherAttacksInterface;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.WitherEntityModel;
import net.minecraft.entity.boss.WitherEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherEntityModel.class)
public abstract class WitherEntityModelMixin<T extends WitherEntity> {
    @Shadow @Final private ModelPart ribcage;
    @Shadow @Final private ModelPart tail;
    @Inject(method = "setAngles(Lnet/minecraft/entity/boss/WitherEntity;FFFFF)V", at = @At("TAIL"))
    private void newAttackAnimations(T witherEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        float charge = ((WitherAttacksInterface)witherEntity).getSlamTime();
        if (charge > 0) {
            if(charge > 50) charge = 1F - Math.clamp((charge - 50) / 20F, 0F, 1F);
            float lerpAmount = Math.clamp(charge / 20F, 0F, 1F) * (float)(Math.PI * 0.25f);
            ribcage.pitch += lerpAmount;
            tail.setPivot(-2.0F, 6.9F + (float)Math.cos(ribcage.pitch) * 10.0F, -0.5F + (float)Math.sin(ribcage.pitch) * 10.0F);
            tail.pitch += lerpAmount;
        }
        charge = ((WitherAttacksInterface)witherEntity).getChargeTime();
        if (charge > 0) {
            float lerpAmount = Math.clamp(charge / 20F, 0F, 1F) * (float)(Math.PI * 0.4f);
            ribcage.pitch += lerpAmount;
            tail.setPivot(-2.0F, 6.9F + (float)Math.cos(ribcage.pitch) * 10.0F, -0.5F + (float)Math.sin(ribcage.pitch) * 10.0F);
            tail.pitch += lerpAmount;
        }
    }
}

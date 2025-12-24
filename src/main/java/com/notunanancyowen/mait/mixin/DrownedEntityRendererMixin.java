package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.client.render.entity.DrownedEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = DrownedEntityRenderer.class, priority = 1001)
public abstract class DrownedEntityRendererMixin {
    @ModifyConstant(method = "setupTransforms(Lnet/minecraft/entity/mob/DrownedEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V", constant = @Constant(floatValue = -10.0F))
    private float rotate90Degrees(float constant) {
        return MobAITweaks.getModConfigValue("drowned_swimming_animation") ? constant * 8F : constant;
    }
}

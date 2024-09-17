package com.notunanancyowen.mixin;

import net.minecraft.client.render.entity.DrownedEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DrownedEntityRenderer.class)
public abstract class DrownedEntityRendererMixin {
    @ModifyConstant(method = "setupTransforms(Lnet/minecraft/entity/mob/DrownedEntity;Lnet/minecraft/client/util/math/MatrixStack;FFF)V", constant = @Constant(floatValue = -10.0F))
    private float rotate90Degrees(float constant) {
        return constant * 8F;
    }
}

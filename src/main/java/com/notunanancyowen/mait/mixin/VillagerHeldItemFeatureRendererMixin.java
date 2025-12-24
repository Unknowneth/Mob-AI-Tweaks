package com.notunanancyowen.mait.mixin;

import net.minecraft.client.render.entity.feature.VillagerHeldItemFeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(VillagerHeldItemFeatureRenderer.class)
public abstract class VillagerHeldItemFeatureRendererMixin {
    @ModifyArgs(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"), require = 0)
    private void changeRenderStyleForFishingRodsOnly(Args args) {
        if(args.get(0) instanceof VillagerEntity v && v.isAttacking() && args.get(2) instanceof ModelTransformationMode && args.get(4) instanceof MatrixStack matrixStack) {
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
            args.set(2, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND);
        }
    }
}

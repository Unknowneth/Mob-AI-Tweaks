package com.notunanancyowen.mixin;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FireworkRocketEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntityRenderer.class)
public abstract class FireworkRocketEntityRendererMixin extends EntityRenderer<FireworkRocketEntity> {
    @Shadow @Final private ItemRenderer itemRenderer;
    FireworkRocketEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }
    @Inject(method = "render(Lnet/minecraft/entity/projectile/FireworkRocketEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void reworkVisual(FireworkRocketEntity fireworkRocketEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        matrixStack.push();
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((!fireworkRocketEntity.wasShotAtAngle() ? -dispatcher.camera.getYaw() : MathHelper.lerp(g, fireworkRocketEntity.prevYaw, fireworkRocketEntity.getYaw())) - 90.0F));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(g, fireworkRocketEntity.prevPitch, fireworkRocketEntity.getPitch()) - 90.0F));
        matrixStack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(90F));
        itemRenderer.renderItem(fireworkRocketEntity.getStack(), ModelTransformationMode.GROUND, i, OverlayTexture.DEFAULT_UV, matrixStack, vertexConsumerProvider, fireworkRocketEntity.getWorld(), fireworkRocketEntity.getId());
        matrixStack.pop();
        super.render(fireworkRocketEntity, f, g, matrixStack, vertexConsumerProvider, i);
        ci.cancel();
    }
}

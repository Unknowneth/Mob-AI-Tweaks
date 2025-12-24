package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.dataholders.WitherAttacksInterface;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.WitherEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherEntityRenderer.class)
public abstract class WitherEntityRendererMixin {
    @Shadow @Final private static Identifier INVULNERABLE_TEXTURE;
    @Inject(method = "getTexture(Lnet/minecraft/entity/boss/WitherEntity;)Lnet/minecraft/util/Identifier;", at = @At("RETURN"), cancellable = true)
    private void phase2Visuals(WitherEntity witherEntity, CallbackInfoReturnable<Identifier> cir) {
        if(((WitherAttacksInterface)witherEntity).beBlue() || witherEntity.getHealth() <= witherEntity.getMaxHealth() * 0.1666F) cir.setReturnValue(INVULNERABLE_TEXTURE);
    }
    @Inject(method = "scale(Lnet/minecraft/entity/boss/WitherEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("HEAD"))
    private void rotateOnDash(WitherEntity witherEntity, MatrixStack matrixStack, float f, CallbackInfo ci) {
        int chargeTime = ((WitherAttacksInterface)witherEntity).getChargeTime();
        if(chargeTime >= 20 && chargeTime < 25) matrixStack.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees((chargeTime + MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(!MinecraftClient.getInstance().isPaused()) - 20) * 72), 0F, -2.5F, 0F);
    }
}

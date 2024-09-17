package com.notunanancyowen.mixin;

import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import net.minecraft.client.render.entity.GhastEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(GhastEntityRenderer.class)
public abstract class GhastEntityRendererMixin {
    @Inject(method = "scale(Lnet/minecraft/entity/mob/GhastEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("HEAD"), cancellable = true)
    private void inflateBeforeShooting(GhastEntity ghastEntity, MatrixStack matrixStack, float f, CallbackInfo ci) {
        ci.cancel();
        float xzScaler = 4.5F;
        float yScaler = 4.5F;
        float johnInterloper;
        int shootTime = 0;
        if(ghastEntity instanceof SpecialAttacksInterface attack) shootTime += attack.getSpecialCooldown();
        if(shootTime < -25) {
            johnInterloper = MathHelper.clamp(shootTime + 40, 0, 15) / 15F;
            xzScaler *= 1.2F - johnInterloper * 0.2F;
        }
        else if(shootTime > 20) {
            johnInterloper = MathHelper.clamp(shootTime - 20, 0, 10) * 0.1F;
            xzScaler *= 1F + johnInterloper * johnInterloper * 0.2F;
            yScaler *= 1.2F - (float)Math.sqrt(johnInterloper) * 0.2F;
        }
        else if(shootTime > 10) {
            johnInterloper = MathHelper.clamp(shootTime - 10, 0, 10) * 0.1F;
            yScaler *= 1F + johnInterloper * johnInterloper * 0.2F;
        }
        matrixStack.scale(xzScaler, yScaler, xzScaler);
    }
}

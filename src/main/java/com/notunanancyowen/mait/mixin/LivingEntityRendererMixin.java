package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "getOverlay", at = @At("HEAD"), cancellable = true)
    private static void overlayOverride(LivingEntity entity, float whiteOverlayProgress, CallbackInfoReturnable<Integer> cir) {
        if(entity instanceof WitherEntity wither) {
            int i = wither.getInvulnerableTimer();
            if(i > 100) cir.setReturnValue(OverlayTexture.packUv(OverlayTexture.getU(Math.clamp(0.1F * (i - 100), 0F, 1F)), OverlayTexture.getV(false)));
        }
        if(entity instanceof SpecialAttacksInterface attacks) if(attacks.attackType().equals("GRENADE") && attacks.getSpecialCooldown() > 0 && attacks.getSpecialCooldown() < 15) cir.setReturnValue(OverlayTexture.packUv(OverlayTexture.getU((float)Math.sin(Math.PI * (float)attacks.getSpecialCooldown() / 15F)), OverlayTexture.getV(entity.hurtTime > 0 || entity.deathTime > 0)));
        else if(entity instanceof GhastEntity ghast && attacks.getSpecialCooldown() > 10 && attacks.getSpecialCooldown() < 20) cir.setReturnValue(OverlayTexture.packUv(OverlayTexture.getU((float)Math.sin(Math.PI * (float)(attacks.getSpecialCooldown() - 10) / 10F)), OverlayTexture.getV(ghast.hurtTime > 0 || ghast.deathTime > 0)));
        if(cir.getReturnValue() != null) {
            int originalColor = cir.getReturnValue();
            if(entity.isOnFire() && !entity.isFireImmune()) cir.setReturnValue(ColorHelper.Argb.mixColor(ColorHelper.Argb.getArgb(255, 255, 165 ,0), originalColor));
            else if(entity.isFrozen()) cir.setReturnValue(ColorHelper.Argb.mixColor(ColorHelper.Argb.getArgb(255, 173, 216, 230), originalColor));
        }
    }
    @ModifyConstant(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", constant = @Constant(intValue = -1))
    private int becomeOrangeWhenBurningAndBlueWhenFreezing(int constant, LivingEntity entity) {
        return !MobAITweaks.getModConfigValue("burn_and_freeze_visual_effects") ? constant : entity.isOnFire() && !entity.isFireImmune() ? ColorHelper.Argb.mixColor(ColorHelper.Argb.getArgb(255, 255, 165 ,0), constant) : entity.isFrozen() ? ColorHelper.Argb.mixColor(ColorHelper.Argb.getArgb(255, 173, 216, 230), constant) : constant;
    }
    @Inject(method = "setupTransforms", at = @At("TAIL"))
    private void performCoolFlip(LivingEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta, float scale, CallbackInfo ci) {
        if(entity instanceof PathAwareEntity mob && mob.isFallFlying()) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F - mob.getPitch(tickDelta)));
        else if(!entity.hasVehicle() && entity instanceof SpecialAttacksInterface special) if((special.attackType().equals("DODGE") || special.attackType().equals("FLIP")) && Math.abs(special.getSpecialCooldown()) < 10 && special.getSpecialCooldown() != 0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-(special.getSpecialCooldown() - Math.signum(special.getSpecialCooldown()) * tickDelta) * 36F), 0.0F, entity.getHeight() / entity.getScale() / entity.getScaleFactor() / 2.0F, 0.0F);
        else if(special.attackType().equals("HIDE")) if(special.getSpecialCooldown() >= -12 && special.getSpecialCooldown() < 0) {
            matrices.translate(0F, -(1F - (special.getSpecialCooldown() + 12 + tickDelta) / 12F), 0F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F * (1F - (special.getSpecialCooldown() + 12 + tickDelta) / 12F)));
        }
        else if(special.getSpecialCooldown() > 4 && special.getSpecialCooldown() <= 8) {
            matrices.translate(0F, special.getSpecialCooldown() < 8 ? -(special.getSpecialCooldown() - 4 + tickDelta) / 4F : -1F, 0F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(special.getSpecialCooldown() < 8 ? 90F - 180F * (special.getSpecialCooldown() - 4 + tickDelta) / 4F : -90F));
        }
        else if(special.getSpecialCooldown() > 0 && special.getSpecialCooldown() <= 4) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(special.getSpecialCooldown() < 4 ? 90F * (special.getSpecialCooldown() + tickDelta) / 4F : 90F));
    }
}

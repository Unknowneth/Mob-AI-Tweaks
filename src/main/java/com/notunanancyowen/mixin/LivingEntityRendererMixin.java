package com.notunanancyowen.mixin;

import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "getOverlay", at = @At("HEAD"), cancellable = true)
    private static void overlayOverride(LivingEntity entity, float whiteOverlayProgress, CallbackInfoReturnable<Integer> cir) {
        if(entity instanceof WitherEntity wither) {
            int i = wither.getInvulnerableTimer();
            if(i > 100) cir.setReturnValue(OverlayTexture.packUv(OverlayTexture.getU(MathHelper.clamp(0.1F * (i - 100), 0F, 1F)), OverlayTexture.getV(false)));
        }
        if(entity instanceof WitherSkeletonEntity skeleton && skeleton instanceof SpecialAttacksInterface attacks && attacks.getSpecialCooldown() > 0) cir.setReturnValue(OverlayTexture.packUv(OverlayTexture.getU((float)Math.sin(Math.PI * (float)attacks.getSpecialCooldown() / 15F)), OverlayTexture.getV(skeleton.hurtTime > 0 || skeleton.deathTime > 0)));
        if(entity instanceof GhastEntity ghast && ghast instanceof SpecialAttacksInterface attacks && attacks.getSpecialCooldown() > 10 && attacks.getSpecialCooldown() < 20) cir.setReturnValue(OverlayTexture.packUv(OverlayTexture.getU((float)Math.sin(Math.PI * (float)(attacks.getSpecialCooldown() - 10) / 10F)), OverlayTexture.getV(ghast.hurtTime > 0 || ghast.deathTime > 0)));
        if(cir.getReturnValue() != null) {
            int originalColor = cir.getReturnValue();
            if(entity.isOnFire() && !entity.isFireImmune()) cir.setReturnValue(ColorHelper.Argb.mixColor(ColorHelper.Argb.getArgb(255, 255, 165 ,0), originalColor));
            else if(entity.isFrozen()) cir.setReturnValue(ColorHelper.Argb.mixColor(ColorHelper.Argb.getArgb(255, 173, 216, 230), originalColor));
        }
    }
}

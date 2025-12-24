package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.PhantomEyesFeatureRenderer;
import net.minecraft.client.render.entity.model.PhantomEntityModel;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PhantomEyesFeatureRenderer.class)
public abstract class PhantomEyesFeatureRendererMixin<T extends PhantomEntity> {
    @Unique private static final RenderLayer UPSCALED_SKIN = RenderLayer.getEyes(Identifier.of(MobAITweaks.MOD_ID, "textures/phantomare/eyes.png"));
    @Unique private PhantomEntityModel<T> phantom;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void assignPhantom(FeatureRendererContext<T, PhantomEntityModel<T>> featureRendererContext, CallbackInfo ci) {
        phantom = featureRendererContext.getModel();
    }
    @Inject(method = "getEyesTexture", at = @At("HEAD"), cancellable = true)
    private void becomeMoreDetailed(CallbackInfoReturnable<RenderLayer> cir) {
        if(phantom.handSwingProgress == 1F) cir.setReturnValue(UPSCALED_SKIN);
    }
}

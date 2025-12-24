package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.client.render.entity.PhantomEntityRenderer;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PhantomEntityRenderer.class)
public abstract class PhantomEntityRendererMixin {
    @Unique private static final Identifier[] UPSCALED_TEXTURE = new Identifier[] {
            Identifier.of(MobAITweaks.MOD_ID, "textures/phantomare/1.png"),
            Identifier.of(MobAITweaks.MOD_ID, "textures/phantomare/2.png"),
            Identifier.of(MobAITweaks.MOD_ID, "textures/phantomare/3.png")
    };
    @Inject(method = "getTexture(Lnet/minecraft/entity/mob/PhantomEntity;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    private void becomeMoreDetailed(PhantomEntity phantomEntity, CallbackInfoReturnable<Identifier> cir) {
        if(phantomEntity.handSwingProgress == 1F) cir.setReturnValue(UPSCALED_TEXTURE[phantomEntity.age % 3]);
    }
}

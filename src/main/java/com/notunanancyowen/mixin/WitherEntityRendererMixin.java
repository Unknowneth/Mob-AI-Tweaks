package com.notunanancyowen.mixin;

import com.notunanancyowen.dataholders.WitherAttacksInterface;
import net.minecraft.client.render.entity.WitherEntityRenderer;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherEntityRenderer.class)
public abstract class WitherEntityRendererMixin {
    @Shadow @Final private static Identifier INVULNERABLE_TEXTURE;
    @Inject(method = "getTexture(Lnet/minecraft/entity/boss/WitherEntity;)Lnet/minecraft/util/Identifier;", at = @At("RETURN"), cancellable = true)
    private void phase2Visuals(WitherEntity witherEntity, CallbackInfoReturnable<Identifier> cir) {
        if(((WitherAttacksInterface)witherEntity).beBlue() || witherEntity.getHealth() <= witherEntity.getMaxHealth() * 0.1666F) cir.setReturnValue(INVULNERABLE_TEXTURE);
    }
}

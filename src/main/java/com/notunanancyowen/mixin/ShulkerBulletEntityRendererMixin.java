package com.notunanancyowen.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ShulkerBulletEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBulletEntityRenderer.class)
public abstract class ShulkerBulletEntityRendererMixin {
    @Unique private final static Identifier OTHER_TEXTURE = Identifier.of(MobAITweaks.MOD_ID, "textures/seeking_mine.png");
    @Unique private static final RenderLayer OTHER_LAYER = RenderLayer.getEntityTranslucent(OTHER_TEXTURE);
    @Unique private final static Identifier ANOTHER_TEXTURE = Identifier.of(MobAITweaks.MOD_ID, "textures/seeking_mine.png");
    @Unique private static final RenderLayer ANOTHER_LAYER = RenderLayer.getEntityTranslucent(OTHER_TEXTURE);
    @Inject(method = "render(Lnet/minecraft/entity/projectile/ShulkerBulletEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void getShulkerBullet(ShulkerBulletEntity shulkerBulletEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci, @Share("shulkerBulletVariant") LocalIntRef variant) {
        variant.set(((SpecialAttacksInterface)shulkerBulletEntity).getSpecialCooldown());
    }
    @ModifyArg(method = "render(Lnet/minecraft/entity/projectile/ShulkerBulletEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/ShulkerBulletEntityModel;getLayer(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"), index = 0)
    private Identifier changeTexture(Identifier par1, @Share("shulkerBulletVariant") LocalIntRef variant) {
        return variant != null ? variant.get() == 2 ? ANOTHER_TEXTURE : variant.get() == 1 ? OTHER_TEXTURE : par1 : par1;
    }
    @ModifyArg(method = "render(Lnet/minecraft/entity/projectile/ShulkerBulletEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;", ordinal = 1), index = 0)
    private RenderLayer changeOverlayTexture(RenderLayer layer, @Share("shulkerBulletVariant") LocalIntRef variant) {
        return variant != null ? variant.get() == 2 ? ANOTHER_LAYER : variant.get() == 1 ? OTHER_LAYER : layer : layer;
    }
}

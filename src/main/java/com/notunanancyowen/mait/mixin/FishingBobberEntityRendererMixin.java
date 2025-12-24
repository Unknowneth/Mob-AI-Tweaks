package com.notunanancyowen.mait.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FishingBobberEntityRenderer.class, priority = 1001)
public abstract class FishingBobberEntityRendererMixin extends EntityRenderer<FishingBobberEntity> {
    @Shadow @Final private static RenderLayer LAYER;
    @Shadow private static void vertex(VertexConsumer buffer, MatrixStack.Entry matrix, int light, float x, int y, int u, int v) {}
    @Shadow private static void renderFishingLine(float x, float y, float z, VertexConsumer buffer, MatrixStack.Entry matrices, float segmentStart, float segmentEnd) {}
    @Unique private static float percentage(int value, int max) {
        return (float)value / (float)max;
    }
    private FishingBobberEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }
    @Inject(method = "render(Lnet/minecraft/entity/projectile/FishingBobberEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void fromMob(FishingBobberEntity fishingBobberEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if(fishingBobberEntity.getOwner() instanceof PathAwareEntity mob) {
            matrixStack.push();
            matrixStack.push();
            matrixStack.scale(0.5F, 0.5F, 0.5F);
            matrixStack.multiply(dispatcher.getRotation());
            MatrixStack.Entry entry = matrixStack.peek();
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(LAYER);
            vertex(vertexConsumer, entry, i, 0.0F, 0, 0, 1);
            vertex(vertexConsumer, entry, i, 1.0F, 0, 1, 1);
            vertex(vertexConsumer, entry, i, 1.0F, 1, 1, 0);
            vertex(vertexConsumer, entry, i, 0.0F, 1, 0, 0);
            matrixStack.pop();
            Vec3d vec3d = getHandPosNotPlayer(mob, g);
            Vec3d vec3d2 = fishingBobberEntity.getLerpedPos(g).add(0.0, 0.25, 0.0);
            float k = (float)(vec3d.x - vec3d2.x);
            float l = (float)(vec3d.y - vec3d2.y);
            float m = (float)(vec3d.z - vec3d2.z);
            VertexConsumer vertexConsumer2 = vertexConsumerProvider.getBuffer(RenderLayer.getLineStrip());
            MatrixStack.Entry entry2 = matrixStack.peek();
            int n = 16;
            for (int o = 0; o <= n; o++) renderFishingLine(k, l, m, vertexConsumer2, entry2, percentage(o, n), percentage(o + 1, n));
            matrixStack.pop();
            super.render(fishingBobberEntity, f, g, matrixStack, vertexConsumerProvider, i);
        }
    }
    @Unique private Vec3d getHandPosNotPlayer(PathAwareEntity mob, float tickDelta) {
        int i = mob instanceof VillagerEntity v && v.isAttacking() ? 0 : mob.getMainArm() == Arm.RIGHT ? 1 : -1;
        ItemStack itemStack = mob.getMainHandStack();
        if (!itemStack.isOf(Items.FISHING_ROD)) i = -i;
        float g = MathHelper.lerp(tickDelta, mob.prevBodyYaw, mob.bodyYaw) * (float) (Math.PI / 180.0);
        double c = 0;
        if(mob instanceof ZombieEntity z) c = (float)Math.PI / (z.isAttacking() ? 1.5F : 2.25F) - 0.66F + MathHelper.sin(z.getHandSwingProgress(tickDelta) * (float) Math.PI) * 1.2F - MathHelper.sin((1.0F - (1.0F - z.getHandSwingProgress(tickDelta)) * (1.0F - z.getHandSwingProgress(tickDelta))) * (float) Math.PI) * 0.4F;
        double d = MathHelper.sin(g);
        double e = MathHelper.cos(g);
        float h = mob.getScale();
        double j = (double)i * 0.35 * (double)h;
        double k = i == 0 ? 0.65 * (double)h : c != 0 ? (float)-Math.cos(c) : 0.0;
        float l = mob.isInSneakingPose() ? -0.1875F : 0.0F;
        if(i == 0) l += 0.5F;
        else if(c != 0) l += (float)Math.sin(c);
        return mob.getCameraPosVec(tickDelta).add(-e * j - d * k, (double)l - 0.45 * (double)h, -d * j + e * k);
    }
}

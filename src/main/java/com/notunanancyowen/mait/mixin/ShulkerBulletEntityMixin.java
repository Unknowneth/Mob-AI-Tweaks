package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBulletEntity.class)
public abstract class ShulkerBulletEntityMixin extends ProjectileEntity implements SpecialAttacksInterface {
    @Shadow private int stepCount;
    @Unique private final static TrackedData<Integer> VARIANT = DataTracker.registerData(ShulkerBulletEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    ShulkerBulletEntityMixin(EntityType<? extends ShulkerBulletEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void addTextureVariance(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(VARIANT, 0);
    }
    @Inject(method = "changeTargetDirection", at = @At("TAIL"))
    private void homeInFaster(Direction.Axis axis, CallbackInfo ci) {
        if(getSpecialCooldown() == 1 && stepCount > 10) stepCount = 10;
        if(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) stepCount *= 2;
    }
    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void onHitEffects(EntityHitResult entityHitResult, CallbackInfo ci) {
        if(getSpecialCooldown() != 1) return;
        super.onEntityHit(entityHitResult);
        entityHitResult.getEntity().addVelocity(0, -2, 0);
        entityHitResult.getEntity().setAir(entityHitResult.getEntity().getAir() - 1);
        if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1, 0d, 0d, 0d, 0d);
        ci.cancel();
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void extraEffects(CallbackInfo ci) {
        if(getSpecialCooldown() == 2) {
            setVelocity(getVelocity().multiply(1.2d));
            velocityDirty  = true;
        }
        else if(getSpecialCooldown() == 1) if(getOwner() != null && getOwner().isAlive()) {
            setVelocity(getVelocity().multiply(0.7d));
            velocityDirty  = true;
        }
        else discard();
    }
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"), index = 0)
    private ParticleEffect changeParticleType(ParticleEffect parameters) {
        return getSpecialCooldown() == 2 && age % 2 == 0 ? ParticleTypes.DRAGON_BREATH : getSpecialCooldown() == 1 ? isSubmergedInWater() ? ParticleTypes.BUBBLE : ParticleTypes.BUBBLE_POP : parameters;
    }
    @Override public Text getName() {
        return getSpecialCooldown() == 2 ? Text.translatable("mob-ai-tweaks.end_crystal_projectile") : getSpecialCooldown() == 1 ? Text.translatable("mob-ai-tweaks.elder_guardian_projectile") : super.getName();
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        getDataTracker().set(VARIANT, i);
    }
    @SuppressWarnings("all")
    @Override public int getSpecialCooldown() {
        return getDataTracker().get(VARIANT);
    }
}

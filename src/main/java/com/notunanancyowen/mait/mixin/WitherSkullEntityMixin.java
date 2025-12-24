package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WitherSkullEntity.class)
public abstract class WitherSkullEntityMixin extends ExplosiveProjectileEntity {
    @Shadow public abstract boolean isCharged();
    @Shadow protected abstract float getDrag();
    @Unique private int targetId = -1;
    WitherSkullEntityMixin(EntityType<? extends WitherSkullEntity> type, World world) {
        super(type, world);
    }
    @Override public void tick() {
        boolean witherIsReworked = MobAITweaks.getModConfigValue("wither_rework");
        if(age == 1 || targetId == -1) {
            if(age == 1 && getWorld() instanceof ServerWorld server) {
                for(int i = 0; i < 2; i++) server.spawnParticles(isCharged() && witherIsReworked && i > 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE, getPos().getX(), getPos().getY(), getPos().getZ(), 6, 0.2d, 0.2d, 0.2d, 0.1d);
                if(isCharged() && witherIsReworked) server.spawnParticles(ParticleTypes.FLASH, getPos().getX(), getPos().getY(), getPos().getZ(), 1, 0d, 0d, 0d, 0d);
            }
            if(isCharged()) if(getOwner() instanceof PlayerEntity player) {
                List<PathAwareEntity> list = getWorld().getEntitiesByClass(PathAwareEntity.class, player.getBoundingBox().expand(320.0, 320.0, 320.0), h -> {
                    Vec3d vec3d = player.getRotationVector();
                    Vec3d vec3d2 = new Vec3d(h.getX() - player.getX(), h.getEyeY() - player.getEyeY(), h.getZ() - player.getZ());
                    double d = vec3d2.length();
                    vec3d2 = vec3d2.normalize();
                    double e = vec3d.dotProduct(vec3d2);
                    return e > 1.0 - 0.025 / d && player.canSee(h);
                });
                double d = 320d;
                if(!list.isEmpty()) for(PathAwareEntity m : list) if(distanceTo(player) < d) {
                    d = distanceTo(m);
                    targetId = m.getId();
                }
            }
            else if(witherIsReworked && getOwner() instanceof PathAwareEntity shooter && shooter.getTarget() instanceof LivingEntity target) targetId = target.getId();
            if(age == 1) ProjectileUtil.setRotationFromVelocity(this, 1.0F);
        }
        super.tick();
        if(this.getOwner() instanceof HostileEntity shooter && shooter.getTarget() instanceof LivingEntity target && MobAITweaks.isOminous(target) && target.isAlive() && distanceTo(target) > 5) {
            setVelocity(getVelocity().multiply(0.8d));
            addVelocity(target.getEyePos().subtract(getPos()).normalize().multiply(0.1d));
        }
        if(isCharged() && witherIsReworked) {
            if(getWorld().getEntityById(targetId) instanceof LivingEntity target && (target.isBlocking() || target.isOnGround() || distanceTo(target) > 2) && target.isAlive()) {
                setVelocity(getVelocity().multiply(getDrag()));
                addVelocity(target.getEyePos().subtract(getPos()).normalize().multiply(0.17d));
                velocityDirty = true;
            }
            if(age % 3 == 0 && getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getPos().getX(), getPos().getY(), getPos().getZ(), 1, 0.01d, 0.2d, 0.01d, 0.1d);
        }
    }
    @Inject(method = "onEntityHit", at = @At("TAIL"))
    private void healWither(EntityHitResult entityHitResult, CallbackInfo ci) {
        if(getOwner() instanceof PathAwareEntity mob && mob.isAlive()) mob.heal(1F);
        if(getOwner() instanceof PlayerEntity player && player.isAlive()) player.heal(player.getMaxHealth() * 0.2F);
    }
    @Inject(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z"), cancellable = true)
    private void suppressWitherEffect(EntityHitResult entityHitResult, CallbackInfo ci) {
        if(getOwner() instanceof SpecialAttacksInterface s && s.attackType().equals("GRENADE")) ci.cancel();
    }
    @ModifyArg(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFZLnet/minecraft/world/World$ExplosionSourceType;)Lnet/minecraft/world/explosion/Explosion;"), index = 6)
    private World.ExplosionSourceType suppressGriefing(World.ExplosionSourceType explosionSourceType) {
        if(getOwner() instanceof SpecialAttacksInterface s && s.attackType().equals("GRENADE")) return World.ExplosionSourceType.NONE;
        return explosionSourceType;
    }
}

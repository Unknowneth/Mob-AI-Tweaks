package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin extends ProjectileEntity {
    @Unique private static final TrackedData<Boolean> DESPISE = DataTracker.registerData(PersistentProjectileEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique protected int targetId = -1;
    @Unique protected boolean canHome() {
        return true;
    }
    @Shadow public abstract boolean isShotFromCrossbow();
    @Shadow private @Nullable ItemStack weapon;
    @Shadow public abstract void setCritical(boolean critical);
    @Shadow public abstract boolean isCritical();
    @Shadow protected boolean inGround;
    @Shadow public PersistentProjectileEntity.PickupPermission pickupType;
    @Shadow private ItemStack stack;
    PersistentProjectileEntityMixin(EntityType<? extends PersistentProjectileEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
    private void despiseEnchantment(EntityType<? extends PersistentProjectileEntity> type, LivingEntity owner, World world, ItemStack stack, ItemStack shotFrom, CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("enchantments_from_mod")) return;
        boolean hasHoming = stack.getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "despise")));
        if(weapon != null) hasHoming |= weapon.getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "despise")));
        getDataTracker().set(DESPISE, hasHoming);
        if(hasHoming && owner instanceof PlayerEntity player) {
            List<MobEntity> list = getWorld().getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(320.0, 320.0, 320.0), h -> {
                Vec3d vec3d = player.getRotationVector();
                Vec3d vec3d2 = new Vec3d(h.getX() - player.getX(), h.getEyeY() - player.getEyeY(), h.getZ() - player.getZ());
                double d = vec3d2.length();
                vec3d2 = vec3d2.normalize();
                double e = vec3d.dotProduct(vec3d2);
                return e > 1.0 - 0.025 / d && player.canSee(h);
            });
            double d = 320d;
            if(!list.isEmpty()) for(MobEntity m : list) if(distanceTo(player) < d) {
                d = distanceTo(m);
                targetId = m.getId();
            }
        }
        else if(owner instanceof MobEntity mob && mob.getTarget() != null) targetId = mob.getTarget().getId();
    }
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void trackData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(DESPISE, false);
    }
    @WrapMethod(method = "writeCustomDataToNbt")
    private void antiCrashMeasure1(NbtCompound nbt, Operation<Void> original) {
        try {
            original.call(nbt);
        }
        catch (Throwable t) {
            discard();
            MobAITweaks.LOGGER.info(t.getLocalizedMessage());
        }
    }
    @WrapMethod(method = "readCustomDataFromNbt")
    private void antiCrashMeasure2(NbtCompound nbt, Operation<Void> original) {
        try {
            original.call(nbt);
        }
        catch (Throwable t) {
            discard();
            MobAITweaks.LOGGER.info(t.getLocalizedMessage());
        }
    }
    @Inject(method = "setVelocity", at = @At("HEAD"))
    private void shootThisGuy(double x, double y, double z, float power, float uncertainty, CallbackInfo ci) {
        if(isOnFire() && getWorld() instanceof ServerWorld server) for (int i = 0; i < 3; i++) server.spawnParticles(i == 2 ? ParticleTypes.SMOKE : i == 1 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, getX(), getY(), getZ(), 3, 0.1d, 0.1d, 0.1d, 0.1d);
        if(getOwner() instanceof HostileEntity mob) if(weapon != null && (isShotFromCrossbow() || weapon.getItem() instanceof CrossbowItem) && !isCritical() && mob.getTarget() != null) setCritical(true);
        else if(mob.getType().isIn(MobAITweaks.ALWAYS_SHOOTS_CRIT_ARROWS)) setCritical(true);
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void home(CallbackInfo ci) {
        if(getDataTracker().get(DESPISE)) if(canHome() && !inGround && getWorld().getEntityById(targetId) instanceof LivingEntity target) {
            addVelocity(target.getEyePos().subtract(getPos()).normalize().multiply(0.34d));
            setVelocity(getVelocity().multiply(0.86d));
            setGlowing(true);
        }
        else setGlowing(false);
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void spawnFireWhenBurning(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("burning_projectile_visuals") && isOnFire() && getWorld() instanceof ServerWorld server) server.spawnParticles(age % 2 == 0 ?  isCritical() ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE, getX(), getY(), getZ(), 1, 0.1d, 0.1d, 0.1d, 0.05d);
    }
    @Inject(method = "getDragInWater", at = @At("HEAD"), cancellable = true)
    private void noWaterInertiaWhenFeatherweight(CallbackInfoReturnable<Float> cir) {
        if(stack.contains(MobAITweaks.IGNORES_WATER)) cir.setReturnValue(0.99F);
    }
    @Inject(method = "fall", at = @At("HEAD"))
    private void resetIgnoresWaterTagWhenFalling(CallbackInfo ci) {
        if(inGround) stack.remove(MobAITweaks.IGNORES_WATER);
    }
    @Override public boolean hasNoGravity() {
        return super.hasNoGravity() || (getDataTracker().get(DESPISE) && canHome() && !inGround && getWorld().getEntityById(targetId) instanceof LivingEntity);
    }
}

package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.goals.HostileMobRandomlySitDownGoal;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSkeletonEntity.class)
public abstract class AbstractSkeletonEntityMixin extends HostileEntity implements RangedAttackMob {
    @Shadow public abstract void updateAttackType();
    @Shadow protected abstract PersistentProjectileEntity createArrowProjectile(ItemStack arrow, float damageModifier);
    @Unique private static final TrackedData<Boolean> BABY = DataTracker.registerData(AbstractSkeletonEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private boolean cantStrafe() {
        if(!MobAITweaks.getModConfigValue("skeleton_sniper_AI")) return false;
        if(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) return getControllingVehicle() == null && age / 100 % 2 == 0; //switch attack types every 5 seconds
        return (!getWorld().isSkyVisible(getBlockPos()) || getWorld().isDay() || isBaby()) && getControllingVehicle() == null;
    }
    AbstractSkeletonEntityMixin(EntityType<? extends AbstractSkeletonEntity> entityType, World world) {
        super(entityType, world);
    }
    @Override protected void tickHandSwing() {
        super.tickHandSwing();
        if(isUsingItem() && !handSwinging) handSwingProgress = 1f;
    }
    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("IsBaby", isBaby());
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readNBTData(NbtCompound nbt, CallbackInfo ci) {
        setBaby(nbt.getBoolean("IsBaby"));
    }
    @Inject(method = "initialize", at = @At("TAIL"))
    private void onSpawned(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, NbtCompound entityNbt, CallbackInfoReturnable<EntityData> cir) {
        if(MobAITweaks.getModConfigValue("skeleton_babies") && world.getRandom().nextInt(10) == 5) setBaby(true);
    }
    @Override protected void initDataTracker() {
        super.initDataTracker();
        getDataTracker().startTracking(BABY, false);
    }
    @Override public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if(data.equals(BABY)) calculateDimensions();
    }
    @Override public void setBaby(boolean baby) {
        this.getDataTracker().set(BABY, baby);
    }
    @Override public boolean isBaby() {
        return this.getDataTracker().get(BABY) && MobAITweaks.getModConfigValue("skeleton_babies");
    }
    @Inject(method = "tickRiding", at = @At("HEAD"), cancellable = true)
    private void disableAIWhenSitting(CallbackInfo ci) {
        if(getVehicle() instanceof AreaEffectCloudEntity aoe && aoe.getCommandTags().contains(MobAITweaks.MOD_ID + ":" + getName().getString() + "'s seat")) ci.cancel();
    }
    @Inject(method = "initGoals", at = @At("HEAD"))
    private void addNewAttacks(CallbackInfo ci) {
        goalSelector.add(4, new MeleeAttackGoal(this, 1.2, false) {
            @Override public boolean canStart() {
                return !(getMainHandStack().getItem() instanceof BowItem) && !(getOffHandStack().getItem() instanceof BowItem) && getTarget() != null;
            }
            @Override public boolean shouldContinue() {
                return getTarget() != null && mob.getNavigation().isFollowingPath();
            }
            @Override public boolean canStop() {
                return getMainHandStack().getItem() instanceof BowItem || getOffHandStack().getItem() instanceof BowItem || getTarget() == null;
            }
            @Override public void start() {
                setAttacking(true);
                super.start();
            }
            @Override public void stop() {
                setAttacking(false);
                setSprinting(false);
                super.stop();
            }
            @Override public void tick() {
                super.tick();
                setSprinting(mob.getNavigation().isFollowingPath());
            }
        });
        goalSelector.add(4, new BowAttackGoal<>((AbstractSkeletonEntity)(HostileEntity)this, 1.0, 40, 15f) {
            @Override public boolean canStart() {
                return super.canStart() && !cantStrafe();
            }
            @Override public boolean shouldContinue() {
                return super.shouldContinue() || (isUsingItem() && getTarget() != null);
            }
            @Override public boolean canStop() {
                return super.canStop() || cantStrafe();
            }
            @Override public void start() {
                int i = 20;
                if (getWorld().getDifficulty().getId() == 2) i = 30;
                else if (getWorld().getDifficulty().getId() < 2) i = 40;
                setAttackInterval(i);
                super.start();
            }
            @Override public void stop() {
                boolean wasUsingItem = isUsingItem() && cantStrafe();
                super.stop();
                if(wasUsingItem) setCurrentHand(getMainHandStack().getItem() instanceof BowItem ? Hand.MAIN_HAND : Hand.OFF_HAND);
                if(cantStrafe() && getTarget() != null) setAttacking(true);
            }
        });
        goalSelector.add(4, new ProjectileAttackGoal(this, 1.25, 40, 60, 15f) {
            @Override public boolean canStart() {
                return cantStrafe() && (getMainHandStack().getItem() instanceof BowItem || getOffHandStack().getItem() instanceof BowItem) && super.canStart();
            }
            @Override public boolean shouldContinue() {
                return super.shouldContinue();
            }
            @Override public boolean canStop() {
                return !cantStrafe() || (!(getMainHandStack().getItem() instanceof BowItem) && !(getOffHandStack().getItem() instanceof BowItem)) || super.canStop();
            }
            @Override public void start() {
                if(cantStrafe() && (handSwinging || handSwingProgress > 0F) && !isUsingItem()) setCurrentHand(getMainHandStack().getItem() instanceof BowItem ? Hand.MAIN_HAND : Hand.OFF_HAND);
                super.start();
            }
            @Override public void stop() {
                super.stop();
                setAttacking(false);
                if(cantStrafe()) stopUsingItem();
            }
        });
        if(MobAITweaks.getModConfigValue("hostile_mobs_can_sit")) goalSelector.add(10, new HostileMobRandomlySitDownGoal(this));
    }
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void shootAtFix(LivingEntity target, float pullProgress, CallbackInfo ci) {
        ItemStack bow = getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, (getMainHandStack().getItem() instanceof BowItem bowItem ? bowItem : getOffHandStack().getItem() instanceof BowItem bowItem2 ? bowItem2 : ItemStack.EMPTY.getItem())));
        PersistentProjectileEntity persistentProjectileEntity = createArrowProjectile(getProjectileType(bow), pullProgress);
        if(!bow.isEmpty()) bow.damage(1, this, AbstractSkeletonEntityMixin::updateAttackType);
        double x = target.getX() - getX();
        double y = target.getBodyY(1F / 3F) - persistentProjectileEntity.getY();
        double z = target.getZ() - getZ();
        double v = Math.sqrt(x * x + z * z);
        persistentProjectileEntity.setPosition(persistentProjectileEntity.getPos().subtract(0d, getScaleFactor() / 4F, 0d).add(getRotationVector()));
        persistentProjectileEntity.setVelocity(x, y + v * 0.2F, z, 1.6F, (float)(14 - getWorld().getDifficulty().getId() * 4));
        if(cantStrafe() && !isBaby()) persistentProjectileEntity.setCritical(true);
        playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        getWorld().spawnEntity(persistentProjectileEntity);
        ci.cancel();
    }
    @Inject(method = "updateAttackType", at = @At("HEAD"), cancellable = true)
    private void fixWeaponTypeCheck(CallbackInfo ci) {
        ci.cancel();
    }
    @Override public boolean canUseRangedWeapon(RangedWeaponItem weapon) {
        return weapon instanceof BowItem;
    }
}

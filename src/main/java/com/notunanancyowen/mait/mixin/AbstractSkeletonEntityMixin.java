package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import com.notunanancyowen.mait.goals.HostileMobRandomlySitDownGoal;
import com.notunanancyowen.mait.goals.RangedGunAttackGoal;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.provider.EnchantmentProviders;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = AbstractSkeletonEntity.class, priority = 1001)
public abstract class AbstractSkeletonEntityMixin extends HostileEntity implements RangedAttackMob, SpecialAttacksInterface {
    @Shadow protected abstract int getHardAttackInterval();
    @Shadow protected abstract int getRegularAttackInterval();
    @Shadow protected abstract PersistentProjectileEntity createArrowProjectile(ItemStack arrow, float damageModifier, @Nullable ItemStack shotFrom);
    @Unique private static final TrackedData<Boolean> BABY = DataTracker.registerData(AbstractSkeletonEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private static final TrackedData<Integer> SPECIAL = DataTracker.registerData(AbstractSkeletonEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private ItemStack lastHeldWeapon = ItemStack.EMPTY;
    @Unique private boolean cantStrafe() {
        if(!MobAITweaks.getModConfigValue("skeleton_sniper_AI") && !isBaby()) return false;
        if(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) return getControllingVehicle() == null && age / 100 % 2 == 0; //switch attack types every 5 seconds
        return (isBaby() || !getWorld().isSkyVisible(getBlockPos()) || getWorld().isDay()) && getControllingVehicle() == null;
    }
    AbstractSkeletonEntityMixin(EntityType<? extends AbstractSkeletonEntity> entityType, World world) {
        super(entityType, world);
    }
    @Override protected void tickHandSwing() {
        super.tickHandSwing();
        if(isUsingItem() && !handSwinging) handSwingProgress = 1f;
        if(!MobAITweaks.getModConfigValue("skeletons_swing_off_hand_on_draw") && handSwingProgress == 1f) lastHandSwingProgress = 1f;
    }
    @SuppressWarnings("all")
    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if(FabricLoader.getInstance().isModLoaded("frycmobvariants")) try {
            if(AbstractSkeletonEntity.class.getDeclaredField("canConvert").get(this) instanceof Boolean b) nbt.putBoolean("MobVariantsCanConvert", b);
        }
        catch(Throwable ignored) {
        }
        nbt.putBoolean("IsBaby", isBaby());
        nbt.putInt("SpecialMove", getDataTracker().get(SPECIAL));
        if(!lastHeldWeapon.isEmpty()) nbt.put("BackupWeapon", lastHeldWeapon.encode(getRegistryManager()));
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readNBTData(NbtCompound nbt, CallbackInfo ci) {
        setBaby(nbt.getBoolean("IsBaby"));
        getDataTracker().set(SPECIAL, nbt.getInt("SpecialMove"));
        lastHeldWeapon = ItemStack.fromNbtOrEmpty(getRegistryManager(), nbt.getCompound("BackupWeapon"));
    }
    @Inject(method = "initialize", at = @At("TAIL"))
    private void onSpawned(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
        if(spawnReason == SpawnReason.NATURAL && MobAITweaks.getModConfigValue("hostile_mobs_spawn_with_effects_chance", 10) > 0 && world.getDifficulty().getId() == 3  && getRandom().nextFloat() < (MobAITweaks.getModConfigValue("hostile_mobs_spawn_with_effects_chance", 10) * 0.01F) * difficulty.getClampedLocalDifficulty()) switch(getRandom().nextInt(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) ? 5 : 4)) {
            case 0 -> addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1));
            case 4 -> addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, -1));
            case 1 -> addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, -1));
            default -> addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, -1));
        }
        if(MobAITweaks.getModConfigValue("skeleton_babies") && world.getRandom().nextInt(10) == 5) setBaby(true);
        if(MobAITweaks.getModConfigValue("skeleton_switch_to_melee_range", 0) > 0) lastHeldWeapon = MobAITweaks.getSecondaryWeapon(getType());
    }
    @Override protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(BABY, false);
        builder.add(SPECIAL, 0);
        if(FabricLoader.getInstance().isModLoaded("frycmobvariants")) try {
            builder.add(new TrackedData<>(BABY.id() - 1, TrackedDataHandlerRegistry.BOOLEAN), false);
        }
        catch(Throwable ignore) {
        }
        super.initDataTracker(builder);
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
    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/HostileEntity;tickMovement()V"), cancellable = true)
    private void disableAIWhenSitting(CallbackInfo ci) {
        if(getVehicle() instanceof AreaEffectCloudEntity aoe && aoe.getOwner() != null && aoe.getOwner().getId() == getId() && aoe.getCommandTags().contains(MobAITweaks.MOD_ID + ":" + getName().getString() + "'s seat")) {
            if(aoe.age > 60) {
                setPitch(60F);
                if(age % 10 == 0) heal(1F);
            }
            else if(aoe.age > 40) setPitch((aoe.age - 40) * 3);
            if(aoe.age == aoe.getDuration() + aoe.getWaitTime() - 1) {
                stopRiding();
                refreshPositionAndAngles(getX(), aoe.getBlockY() + 1, getZ(), getYaw(), 0F);
            }
            else if(hurtTime > 0 || (getTarget() != null && !getTarget().isSneaking() && (getTarget().getVelocity().getX() != 0F || getTarget().getVelocity().getZ() != 0F)) || getAttacker() != null || isDead() || getWorld().getBlockState(getBlockPos().down()).isAir() || !getWorld().getBlockState(getBlockPos().up()).isAir()) {
                if(getAttacker() != null && canTarget(getAttacker())) setTarget(getAttacker());
                stopRiding();
                refreshPositionAndAngles(getX(), aoe.getBlockY() + 1, getZ(), getYaw(), 0F);
                aoe.discard();
            }
            ci.cancel();
        }
    }
    @Inject(method = "initGoals", at = @At("HEAD"))
    private void addNewAttacks(CallbackInfo ci) {
        goalSelector.add(4, new MeleeAttackGoal(this, MobAITweaks.getModConfigValue("skeleton_melee_mode_speed_boost", 10) * 0.01 + 1.0, false) {
            private boolean notHoldingGun() {
                return !getMainHandStack().getItem().getClass().getSimpleName().contains("Gun") && !getMainHandStack().getItem().getClass().getSuperclass().getSimpleName().contains("Gun") && !getOffHandStack().getItem().getClass().getSimpleName().contains("Gun") && !getOffHandStack().getItem().getClass().getSuperclass().getSimpleName().contains("Gun");
            }
            @Override public boolean canStart() {
                return !(getMainHandStack().getItem() instanceof BowItem) && !(getOffHandStack().getItem() instanceof BowItem) && notHoldingGun() && getTarget() != null;
            }
            @Override public boolean shouldContinue() {
                return super.shouldContinue() && (forwardSpeed >= 0f || notHoldingGun()) && (MobAITweaks.getModConfigValue("skeleton_switch_to_melee_range", 0) == 0 || canStart());
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
        goalSelector.add(4, new BowAttackGoal<>((AbstractSkeletonEntity)(HostileEntity)this, 1.0, getRegularAttackInterval(), 15f) {
            @Override public boolean canStart() {
                return super.canStart() && !cantStrafe();
            }
            @Override public boolean shouldContinue() {
                return super.shouldContinue() || (isUsingItem() && getTarget() != null);
            }
            @Override public void start() {
                int i = getHardAttackInterval();
                if (getWorld().getDifficulty().getId() == 2) i = (getRegularAttackInterval() + i) / 2;
                else if (getWorld().getDifficulty().getId() < 2) i = getRegularAttackInterval();
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
        goalSelector.add(4, new ProjectileAttackGoal(this, 1.25, getHardAttackInterval() + 20, getRegularAttackInterval() + 20, 15f) {
            @Override public boolean canStart() {
                return cantStrafe() && (getMainHandStack().getItem() instanceof BowItem || getOffHandStack().getItem() instanceof BowItem) && super.canStart();
            }
            @Override public void tick() {
                if(isUsingItem()) turnHead(bodyYaw, headYaw);
                super.tick();
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
        if(MobAITweaks.getModConfigValue("skeleton_switch_to_melee_range", 0) > 0) goalSelector.add(4, new Goal() {
            private boolean wasUsingRangedWeapon = false;
            @Override public boolean canStart() {
                return !lastHeldWeapon.isEmpty() && getTarget() != null;
            }
            @Override public void start() {
                wasUsingRangedWeapon = getMainHandStack().getItem() instanceof BowItem;
            }
            @Override public void tick() {
                double swapDistance = MobAITweaks.getModConfigValue("skeleton_switch_to_melee_range", 0);
                double targetDistance = distanceTo(getTarget());
                if((targetDistance > swapDistance && !(getMainHandStack().getItem() instanceof BowItem)) || (getMainHandStack().getItem() instanceof BowItem && swapDistance > targetDistance)) swapWeapons();
            }
            @Override public void stop() {
                if(wasUsingRangedWeapon != getMainHandStack().getItem() instanceof BowItem) swapWeapons();
            }
            private void swapWeapons() {
                ItemStack actualLastHeldItem = getMainHandStack();
                equipStack(EquipmentSlot.MAINHAND, lastHeldWeapon);
                lastHeldWeapon = actualLastHeldItem;
            }
        });
        if(MobAITweaks.getModConfigValue("ranged_mobs_use_guns")) goalSelector.add(4, new RangedGunAttackGoal(this));
        if(FabricLoader.getInstance().isModLoaded("musketmod")) goalSelector.add(0, new Goal() {
            @Override public boolean canStart() {
                return getTarget() != null && (getMainHandStack().getItem().getClass().getSimpleName().contains("GunItem") || getOffHandStack().getItem().getClass().getSimpleName().contains("GunItem") || getMainHandStack().getItem().getClass().getSuperclass().getSimpleName().contains("GunItem") || getOffHandStack().getItem().getClass().getSuperclass().getSimpleName().contains("GunItem"));
            }
            @Override public void tick() {
                if(!isUsingItem() && !handSwinging && getActiveHand() != null && !getWorld().getOtherEntities(AbstractSkeletonEntityMixin.this, getBoundingBox(), e -> e.getClass().getSimpleName().contains("Bullet")).isEmpty()) swingHand(getActiveHand());
                if(getTarget() == null) return;
                getLookControl().lookAt(getTarget(), 60F, 60F);
                lookAtEntity(getTarget(), 60F, 60F);
                float skeletonSpeedMultiplier = MobAITweaks.getModConfigValue("skeleton_strafe_speed_buff") ? getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) ? 2F : getWorld().getDifficulty().getId() > 1 ? 1.5F : 1F : 1F;
                getMoveControl().strafeTo(Math.signum(forwardSpeed) * skeletonSpeedMultiplier, Math.signum(sidewaysSpeed) * skeletonSpeedMultiplier);
            }
        });
        if(MobAITweaks.getModConfigValue("hostile_mobs_can_sit")) goalSelector.add(10, new HostileMobRandomlySitDownGoal(this));
    }
    @Inject(method = "initEquipment", at = @At("TAIL"))
    private void giveWoodenSword(Random random, LocalDifficulty localDifficulty, CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("skeleton_switch_to_melee_range", 0) > 0 && !lastHeldWeapon.isEmpty() && getWorld() instanceof ServerWorld server && random.nextFloat() < 0.5F * localDifficulty.getClampedLocalDifficulty()) EnchantmentHelper.applyEnchantmentProvider(lastHeldWeapon, server.getRegistryManager(), EnchantmentProviders.MOB_SPAWN_EQUIPMENT, localDifficulty, random);
    }
    @Inject(method = "shootAt", at = @At("HEAD"), cancellable = true)
    private void shootAtFix(LivingEntity target, float pullProgress, CallbackInfo ci) {
        try {
            ItemStack bow = getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, (getMainHandStack().getItem() instanceof BowItem bowItem ? bowItem : getOffHandStack().getItem() instanceof BowItem bowItem ? bowItem : !lastHeldWeapon.isEmpty() && lastHeldWeapon.getItem() instanceof BowItem bowItem ? bowItem : null)));
            ItemStack arrow = getProjectileType(bow);
            PersistentProjectileEntity persistentProjectileEntity = createArrowProjectile(arrow, pullProgress, bow);
            if(!bow.isEmpty()) bow.damage(1, this, getPreferredEquipmentSlot(bow));
            if(!arrow.isEmpty()) arrow.decrement(1);
            double x = target.getX() - getX();
            double y = target.getBodyY(1F / 3F) - persistentProjectileEntity.getY();
            double z = target.getZ() - getZ();
            double v = Math.sqrt(x * x + z * z);
            persistentProjectileEntity.setPosition(persistentProjectileEntity.getPos().subtract(0d, getScale() * getScaleFactor() / 4F, 0d).add(getRotationVector()));
            persistentProjectileEntity.setVelocity(x, y + v * 0.2F, z, 1.6F, (float)(14 - getWorld().getDifficulty().getId() * 4));
            if(attackType().equals("FLIP")) persistentProjectileEntity.setCritical(!(getControllingVehicle() != null ? getControllingVehicle().isOnGround() : isOnGround()) && fallDistance < 1f && hurtTime == 0);
            if(cantStrafe() && !isBaby()) persistentProjectileEntity.setCritical(true);
            playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
            getWorld().spawnEntity(persistentProjectileEntity);
        }
        catch (Throwable t) {
            MobAITweaks.LOGGER.info(t.getLocalizedMessage());
        }
        ci.cancel();
    }
    @Inject(method = "updateAttackType", at = @At("HEAD"), cancellable = true)
    private void fixWeaponTypeCheck(CallbackInfo ci) {
        if(age == 0 && getType().isIn(MobAITweaks.SKELETONS_THAT_USE_VANILLA_ATTACK_CHECKS)) addNewAttacks(ci);
        ci.cancel();
    }
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void dodgeAbility(CallbackInfo ci) {
        if(attackType().equals("DODGE") || attackType().equals("FLIP")) {
            int specialCd = getSpecialCooldown();
            if(specialCd != 0) fallDistance = 0F;
            if(specialCd > 0) setSpecialCooldown(specialCd - 1);
            else if(specialCd < 0) setSpecialCooldown(specialCd + 1);
        }
    }
    @Override public boolean canUseRangedWeapon(RangedWeaponItem weapon) {
        return weapon instanceof BowItem;
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        getDataTracker().set(SPECIAL, i);
    }
    @SuppressWarnings("all")
    @Override public int getSpecialCooldown() {
        return getDataTracker().get(SPECIAL);
    }
    @SuppressWarnings("all")
    @Override public String attackType() {
        return getType() == EntityType.BOGGED ? "DODGE" : getType() == EntityType.WITHER_SKELETON ? "GRENADE" : getType() == EntityType.STRAY ? "FLIP" : "NONE";
    }
}

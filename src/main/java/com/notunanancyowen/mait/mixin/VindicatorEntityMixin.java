package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VindicatorEntity.class)
public abstract class VindicatorEntityMixin extends IllagerEntity implements CrossbowUser {
    @Unique private static final TrackedData<Boolean> MELEE_STRAFING = DataTracker.registerData(VindicatorEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
    VindicatorEntityMixin(EntityType<? extends IllagerEntity> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "getState", at = @At("HEAD"), cancellable = true)
    private void newArmAnimations(CallbackInfoReturnable<State> cir) {
        if(MobAITweaks.getModConfigValue("vindicator_rework") && isAttacking()) if(getMainHandStack().getItem() instanceof BowItem || (getActiveItem() != null && getActiveItem().getItem() instanceof BowItem) || getOffHandStack().getItem() instanceof BowItem) cir.setReturnValue(State.BOW_AND_ARROW);
        else if(getActiveItem() != null && getActiveItem().getItem() instanceof CrossbowItem) cir.setReturnValue(State.CROSSBOW_CHARGE);
        else if(getMainHandStack().getItem() instanceof CrossbowItem || getOffHandStack().getItem() instanceof CrossbowItem) cir.setReturnValue(State.CROSSBOW_HOLD);
        else if(getDataTracker().get(MELEE_STRAFING) && !handSwinging) cir.setReturnValue(State.CROSSBOW_CHARGE);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void replaceMeleeAttack(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("vindicator_rework") && goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof MeleeAttackGoal)) {
            goalSelector.add(4, new MeleeAttackGoal(this,1.0 + MobAITweaks.getModConfigValue("vindicator_rework_melee_movement_speed_boost", 0) * 0.01, false) {
                //ts was so ahh to code
                private boolean strafingLeft;
                private int strafeTimer;
                private int strafeCounter;
                private int sprintTimer;
                @Override public boolean canStart() {
                    return super.canStart() && !((getMainHandStack().getItem() instanceof RangedWeaponItem r1 && canUseRangedWeapon(r1)) || (getOffHandStack().getItem() instanceof RangedWeaponItem r2 && canUseRangedWeapon(r2)));
                }
                @Override public void tick() {
                    boolean meleeStrafing = getDataTracker().get(MELEE_STRAFING);
                    int hitsNeeded = MobAITweaks.getModConfigValue("vindicator_hits_needed_to_back_off", 3);
                    if(getTarget() instanceof LivingEntity l) if(((l.getLastAttacker() instanceof VindicatorEntity v && v.getId() == getId() && l.hurtTime == 1) || (getLastAttacker() instanceof LivingEntity h && h.getId() == l.getId() && hurtTime == 1) || strafeCounter >= hitsNeeded) && !meleeStrafing) if(++strafeCounter >= hitsNeeded && isOnGround()) {
                        strafeCounter = 0;
                        getDataTracker().set(MELEE_STRAFING, true);
                        strafingLeft = getRandom().nextBoolean();
                        strafeTimer = toGoalTicks(MobAITweaks.getModConfigValue("vindicator_back_off_max_duration", 120));
                        getNavigation().stop();
                        if(isOnGround()) setVelocity(getRotationVector().multiply(-1, 0, -1).normalize().multiply(1.0 + MobAITweaks.getModConfigValue("vindicator_rework_melee_movement_speed_boost", 0) * 0.01));
                        sprintTimer = toGoalTicks(MobAITweaks.getModConfigValue("vindicator_sprint_on_back_off_duration", 20));
                        setSprinting(sprintTimer > 0);
                    }
                    else {
                        setSprinting(false);
                        sprintTimer = 0;
                    }
                    else if(meleeStrafing) if(--strafeTimer <= 0) {
                        getDataTracker().set(MELEE_STRAFING, false);
                        setSprinting(true);
                        sprintTimer = toGoalTicks(MobAITweaks.getModConfigValue("vindicator_sprint_after_back_off_duration", 40));
                        setSprinting(sprintTimer > 0);
                    }
                    else {
                        getNavigation().stop();
                        getMoveControl().strafeTo(distanceTo(l) > 4.0 ? 1F : -1F, strafingLeft ? -1F : 1F);
                        getLookControl().lookAt(getTarget(), 30F, 30F);
                        lookAtEntity(getTarget(), 30F, 30F);
                    }
                    else {
                        strafeTimer = 0;
                        super.tick();
                    }
                    else {
                        strafeTimer = 0;
                        getDataTracker().set(MELEE_STRAFING, false);
                        super.tick();
                    }
                    if(sprintTimer > 0) if(--sprintTimer == 0) setSprinting(false);
                }
                @Override public void start() {
                    super.start();
                    setSprinting(false);
                    sprintTimer = 0;
                    strafeCounter = 0;
                }
                @Override public void stop() {
                    super.stop();
                    setSprinting(false);
                    sprintTimer = 0;
                    getDataTracker().set(MELEE_STRAFING, false);
                }
            });
            goalSelector.add(4, new BowAttackGoal<>(this, 1.0 + MobAITweaks.getModConfigValue("vindicator_rework_melee_movement_speed_boost", 0) * 0.01, 20, 12) {
                @Override public void start() {
                    getDataTracker().set(MELEE_STRAFING, false);
                    setAttacking(true);
                    setSprinting(false);
                    super.start();
                }
                @Override public void stop() {
                    setAttacking(false);
                    setSprinting(false);
                    super.stop();
                }
            });
            goalSelector.add(4, new CrossbowAttackGoal<>(this, 1.0 + MobAITweaks.getModConfigValue("vindicator_rework_melee_movement_speed_boost", 0) * 0.01, 12) {
                @Override public void start() {
                    getDataTracker().set(MELEE_STRAFING, false);
                    setAttacking(true);
                    setSprinting(false);
                    super.start();
                }
                @Override public void stop() {
                    setAttacking(false);
                    setSprinting(false);
                    super.stop();
                }
            });
        }
    }
    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(MELEE_STRAFING, false);
    }
    @Override public void shootAt(LivingEntity target, float pullProgress) {
        try {
            if (getMainHandStack() != null && !getMainHandStack().isEmpty() && getMainHandStack().getItem() instanceof BowItem) {
                ArrowEntity arrow = new ArrowEntity(getWorld(), this, getOffHandStack() == null || getOffHandStack().isEmpty() ? new ItemStack(Items.ARROW, 1) : getOffHandStack(), getMainHandStack() != null && !getMainHandStack().isEmpty() ? getMainHandStack() : new ItemStack(Items.BOW, 1));
                arrow.setVelocity(target.getEyePos().subtract(getEyePos()).normalize().multiply(pullProgress));
                getWorld().spawnEntity(arrow);
                getMainHandStack().damage(1, this, getPreferredEquipmentSlot(getMainHandStack()));
            }
            else {
                shoot(this, 1.6f);
                if (getActiveHand() != null) swingHand(getActiveHand());
            }
        }
        catch (IllegalStateException | NullPointerException e) {
            MobAITweaks.LOGGER.info("Silently caught \"" + e.getMessage() + "\" error at VindicatorEntityMixin.shootAt(LivingEntity, float), ignore this");
        }
    }
    @Override public void setCharging(boolean charging) {
        if(charging) setCurrentHand(getMainHandStack() != null && getMainHandStack().getItem() instanceof CrossbowItem ? Hand.MAIN_HAND : Hand.OFF_HAND);
        else clearActiveItem();
    }
    @Override public void postShoot() {
        getDataTracker().set(MELEE_STRAFING, false);
    }
    @Override public boolean canUseRangedWeapon(RangedWeaponItem weapon) {
        return weapon instanceof BowItem || weapon instanceof CrossbowItem;
    }
}

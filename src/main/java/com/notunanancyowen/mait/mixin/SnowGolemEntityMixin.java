package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import com.notunanancyowen.mait.goals.SnowGolemAttackGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowGolemEntity.class)
public abstract class SnowGolemEntityMixin extends GolemEntity implements SpecialAttacksInterface {
    @Unique private static final TrackedData<Integer> THROW_TIME = DataTracker.registerData(SnowGolemEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    @Shadow public abstract boolean hasPumpkin();
    @Shadow public abstract void setHasPumpkin(boolean hasPumpkin);
    protected SnowGolemEntityMixin(EntityType<? extends GolemEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void replaceAI(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("snow_golem_rework")) if(goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof ProjectileAttackGoal)) goalSelector.add(1, new SnowGolemAttackGoal((SnowGolemEntity)(GolemEntity)this));
    }
    @Inject(method = "initDataTracker", at = @At("HEAD"))
    private void trackAttackTime(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(THROW_TIME, 0);
    }
    @ModifyReturnValue(method = "createSnowGolemAttributes", at = @At("TAIL"))
    private static DefaultAttributeContainer.Builder addDefense(DefaultAttributeContainer.Builder original) {
        if(!MobAITweaks.getModConfigValue("snow_golem_rework")) return original;
        return original.add(EntityAttributes.GENERIC_ARMOR, MobAITweaks.getModConfigValue("snow_golem_armor_if_it_has_pumpkin", 10));
    }
    @ModifyConstant(method = "shootAt", constant = @Constant(floatValue = 12F))
    private float becomeMoreAccurateWhenNoMask(float constant) {
        return hasPumpkin() || !MobAITweaks.getModConfigValue("snow_golem_rework") ? constant : MobAITweaks.getModConfigValue("snow_golem_accuracy_when_no_pumpkin", 2);
    }
    @Inject(method = "setHasPumpkin", at = @At("TAIL"))
    private void removeDefenseWhenNoPumpkin(boolean hasPumpkin, CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("snow_golem_rework")) return;
        var defense = getAttributes().getCustomInstance(EntityAttributes.GENERIC_ARMOR);
        if(defense != null) defense.setBaseValue(hasPumpkin ? MobAITweaks.getModConfigValue("snow_golem_armor_if_it_has_pumpkin", 10) : 0d);
    }
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void returnPumpkinHead(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if(MobAITweaks.getModConfigValue("snow_golem_rework") && MobAITweaks.getModConfigValue("snow_golem_pumpkin_can_be_returned") && player.getStackInHand(hand).isOf(Items.CARVED_PUMPKIN) && !hasPumpkin()) {
            emitGameEvent(GameEvent.SHEAR, player);
            if (!getWorld().isClient) player.getStackInHand(hand).decrementUnlessCreative(1, player);
            setHasPumpkin(true);
            cir.setReturnValue(ActionResult.PASS);
        }
    }
    @Inject(method = "shootAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z", shift = At.Shift.AFTER))
    private void forceSnowballToRide(LivingEntity target, float pullProgress, CallbackInfo ci, @Local(index = 3) SnowballEntity snowball) {
        if(!MobAITweaks.getModConfigValue("snow_golem_attack_rework")) return;
        snowball.startRiding(this, true);
        setLeftHanded(!isLeftHanded());
    }
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void tickDownAnimation(CallbackInfo ci) {
        if(getSpecialCooldown() > 0) if(getPassengerList() == null || getPassengerList().isEmpty() || getPassengerList().stream().noneMatch(e -> e instanceof SnowballEntity)) setSpecialCooldown(getSpecialCooldown() - 1);
    }
    @Override protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        if(passenger instanceof SnowballEntity s) {
            setSpecialCooldown(10 - passenger.age);
            float r = (float)Math.PI * 2F * ((passenger.age + 1) * 0.1F + 0.5F);
            int c = isLeftHanded() ? -90 : 90;
            Vec3d d = getRotationVector(0F, getBodyYaw() + c).lerp(getRotationVector(0F, getHeadYaw() + c), 0.25);
            Vec3d e = getRotationVector(0F, getBodyYaw()).lerp(getRotationVector(0F, getHeadYaw()), 0.25);
            double f = Math.cos(r);
            double g = Math.sin(r);
            Vec3d v = new Vec3d(getX(), getBodyY(0.5) + passenger.getHeight() * 0.25 + f * 0.86602540378443864676372317075294 * 0.8, getZ()).add(d.multiply(0.5 - 0.05 * Math.abs(g)).add(d.multiply(0.13397459621556135323627682924706 * 0.8 * Math.abs(f)))).add(e.multiply(g * 0.86602540378443864676372317075294 * 0.8));
            super.updatePassengerPosition(passenger, positionUpdater);
            positionUpdater.accept(passenger, v.getX(), v.getY(), v.getZ());
            if(passenger.age < 6) return;
            passenger.stopRiding();
            passenger.refreshPositionAndAngles(v, 0F, 0F);
            if(MobAITweaks.getModConfigValue("snow_golem_rework") && !hasPumpkin() && getTarget() != null) {
                v = getTarget().getEyePos().subtract(v.subtract(0, distanceTo(getTarget()) * s.getFinalGravity(), 0)).normalize();
                s.setVelocity(v.getX(), v.getY(), v.getZ(), 1F, becomeMoreAccurateWhenNoMask(12F));
            }
            else s.setVelocity(this, getPitch() - 10, getHeadYaw(), 0F, 1F, becomeMoreAccurateWhenNoMask(12F));
        }
        else super.updatePassengerPosition(passenger, positionUpdater);
    }
    @Override public void setTarget(@Nullable LivingEntity target) {
        if(target == null) super.setTarget(null);
        else if(!target.getType().isIn(MobAITweaks.GOLEMS_NEVER_TARGET_AT_ALL_COSTS)) super.setTarget(target);
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        getDataTracker().set(THROW_TIME, i);
    }
    @SuppressWarnings("all")
    @Override public int getSpecialCooldown() {
        return getDataTracker().get(THROW_TIME);
    }
    @SuppressWarnings("all")
    @Override public String attackType() {
        return "BARRAGE";
    }
}

package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import com.notunanancyowen.mait.goals.EvokerCastFireballGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SpellcastingIllagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EvokerEntity.class)
public abstract class EvokerEntityMixin extends SpellcastingIllagerEntity implements SpecialAttacksInterface {
    @Unique private static final TrackedData<Integer> FIREBALL_COOLDOWN = DataTracker.registerData(EvokerEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    EvokerEntityMixin(EntityType<? extends EvokerEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addNewAttacks(CallbackInfo ci) {
        if(goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof FleeEntityGoal)) {
            goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 8.0f, 0.4f, 0.8f) {
                @Override public boolean canStart() {
                    return getSpecialCooldown() >= 0 && super.canStart();
                }
            });
            goalSelector.add(2, new FleeEntityGoal<>(this, MobEntity.class, 10.0f, 0.6f, 1.0f, l -> l instanceof MobEntity m && m.getTarget() instanceof EvokerEntity e && e.getId() == getId()) {
                @Override public boolean canStart() {
                    return getSpecialCooldown() >= 0 && super.canStart();
                }
            });
        }
        if(MobAITweaks.getModConfigValue("evokers_cast_fireball")) goalSelector.add(1, new EvokerCastFireballGoal(this));
    }
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void trackData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(FIREBALL_COOLDOWN, MobAITweaks.getModConfigValue("evokers_cast_fireball_cooldown", 20));
    }
    @Override public State getState() {
        if(getSpecialCooldown() < 0) return State.BOW_AND_ARROW;
        return super.getState();
    }
    @Inject(method = "mobTick", at = @At("TAIL"))
    private void fireballCooldownTime(CallbackInfo ci) {
        if(!isSpellcasting() && getSpecialCooldown() > 0) setSpecialCooldown(getSpecialCooldown() - 1);
    }
    @Override protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        super.updatePassengerPosition(passenger, positionUpdater);
        if(passenger instanceof SmallFireballEntity fireball) {
            int i = getPassengerList().indexOf(passenger);
            double d = age / 45d * Math.PI + Math.PI * 2d * i / getPassengerList().size();
            if(isLeftHanded()) d *= -1;
            boolean riding = hasVehicle() && getVehicle() != null;
            double b = riding ? getVehicle().getBoundingBox().getLengthX() + getVehicle().getBoundingBox().getLengthZ() : getBoundingBox().getLengthX() + getBoundingBox().getLengthZ();
            double e = Math.min(passenger.age, 20) * 0.05 * b;
            double px = riding ? getVehicle().getX() : getX();
            double py = riding ? getVehicle().getBodyY(0.5) : getBodyY(0.5);
            double pz = riding ? getVehicle().getZ() : getZ();
            positionUpdater.accept(passenger, px + Math.cos(d) * e, py, pz + Math.sin(d) * e);
            if(getTarget() != null && getTarget().getBoundingBox().intersects(passenger.getBoundingBox()) && getTarget().damage(passenger.getDamageSources().fireball(fireball, this), 5)) getTarget().setFireTicks(60);
        }
    }
    @Override protected void updatePostDeath() {
        super.updatePostDeath();
        getPassengerList().forEach(e -> {
            if(e instanceof SmallFireballEntity) e.discard();
        });
    }
    @Override public void onRemoved() {
        super.onRemoved();
        getPassengerList().forEach(e -> {
            if(e instanceof SmallFireballEntity) e.discard();
        });
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        getDataTracker().set(FIREBALL_COOLDOWN, i);
    }
    @SuppressWarnings("all")
    @Override public int getSpecialCooldown() {
        return getDataTracker().get(FIREBALL_COOLDOWN);
    }
    @SuppressWarnings("all")
    @Override public String attackType() {
        return "FIREBALL";
    }
}

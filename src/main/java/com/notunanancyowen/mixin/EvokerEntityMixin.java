package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import com.notunanancyowen.goals.EvokerCastFireballGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.SpellcastingIllagerEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
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
        if(!MobAITweaks.getModConfigValue("evokers_cast_fireball")) return;
        if(goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof FleeEntityGoal)) {
            goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 8.0f, 0.4f, 0.8f) {
                @Override public boolean canStart() {
                    return super.canStart() && getSpecialCooldown() >= 0;
                }
                @Override public boolean shouldContinue() {
                    return super.shouldContinue() && getSpecialCooldown() >= 0;
                }
                @Override public boolean canStop() {
                    return super.canStop() || getSpecialCooldown() < 0;
                }
            });
            goalSelector.add(2, new FleeEntityGoal<>(this, IronGolemEntity.class, 10.0f, 0.6f, 1.0f) {
                @Override public boolean canStart() {
                    return super.canStart() && getSpecialCooldown() >= 0;
                }
                @Override public boolean shouldContinue() {
                    return super.shouldContinue() && getSpecialCooldown() >= 0;
                }
                @Override public boolean canStop() {
                    return super.canStop() || getSpecialCooldown() < 0;
                }
            });
        }
        goalSelector.add(1, new EvokerCastFireballGoal(this));
    }
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void trackData(CallbackInfo ci) {
        getDataTracker().startTracking(FIREBALL_COOLDOWN, 20);
    }
    @Override public State getState() {
        if(getSpecialCooldown() < 0) return State.BOW_AND_ARROW;
        return super.getState();
    }
    @Inject(method = "mobTick", at = @At("TAIL"))
    private void fireballCooldownTime(CallbackInfo ci) {
        if(!isSpellcasting() && getSpecialCooldown() > 0) setSpecialCooldown(getSpecialCooldown() - 1);
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        getDataTracker().set(FIREBALL_COOLDOWN, i);
    }
    @SuppressWarnings("all")
    @Override public int getSpecialCooldown() {
        return getDataTracker().get(FIREBALL_COOLDOWN);
    }
}

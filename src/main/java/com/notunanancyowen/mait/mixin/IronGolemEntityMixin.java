package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(IronGolemEntity.class)
public abstract class IronGolemEntityMixin extends GolemEntity implements SpecialAttacksInterface {
    protected IronGolemEntityMixin(EntityType<? extends GolemEntity> type, World world) {
        super(type, world);
    }
    @Unique private static final EntityAttributeModifier ANGERED_SPEED_BOOST = new EntityAttributeModifier(Identifier.of(MobAITweaks.MOD_ID, "iron_golem_rage"), 0.25d, EntityAttributeModifier.Operation.ADD_VALUE);
    @Unique private static final TrackedData<Integer> DESPERATION = DataTracker.registerData(IronGolemEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void implementSpecialAttack(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(DESPERATION, 0);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void dontTargetSnowGolems(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("iron_golem_rework")) goalSelector.add(0, new Goal() {
            @Override public boolean canStart() {
                if(isSprinting()) setSprinting(false);
                if(getTarget() == null) {
                    setSpecialCooldown(0);
                    return false;
                }
                int desperateTimer = getSpecialCooldown();
                if(desperateTimer > 80) setSpecialCooldown(0);
                else setSpecialCooldown(++desperateTimer);
                return ++desperateTimer > 60;
            }
            @Override public void start() {
                setSprinting(true);
                var a = getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if(a != null) a.addTemporaryModifier(ANGERED_SPEED_BOOST);
                playSound(SoundEvents.ENTITY_IRON_GOLEM_ATTACK, 4.0F, 0.4F);
                getNavigation().stop();
            }
            @Override public void stop() {
                setSprinting(false);
                var a = getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if(a != null) a.removeModifier(ANGERED_SPEED_BOOST);
                setSpecialCooldown(0);
            }
            @Override public void tick() {
                if(getTarget() != null) lookAtEntity(getTarget(), 10F, 10F);
            }
        });
        if(MobAITweaks.getModConfigValue("disable_golem_infighting") && targetSelector.getGoals().removeIf(g -> g.getGoal() instanceof RevengeGoal)) targetSelector.add(2, new RevengeGoal(this, GolemEntity.class));
    }
    @Inject(method = "tryAttack", at = @At("TAIL"))
    private void resetDesperation(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if(MobAITweaks.getModConfigValue("iron_golem_rework")) if(cir.getReturnValue() && getSpecialCooldown() > 60 && getSpecialCooldown() < 80) {
            target.addVelocity(getVelocity());
            if(getWorld() instanceof ServerWorld server) for(int i = 0; i < 8; i++) server.spawnParticles(ParticleTypes.CRIT, target.getParticleX(0.5), target.getRandomBodyY(), target.getParticleZ(0.5), 2, 0.01, 0.01, 0.01, 0.1);
            playSound(SoundEvents.BLOCK_ANVIL_PLACE, 1.0F, 1.0F);
            setSpecialCooldown(-1);
        }
        else setSpecialCooldown(0);
    }
    @ModifyArgs(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/GolemEntity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private void takeIncreasedDamageFromPickaxes(Args args) {
        if(args.get(0) instanceof DamageSource d && d.getAttacker() != null && d.getAttacker().getWeaponStack() != null && d.getAttacker().getWeaponStack().isIn(ItemTags.PICKAXES) && args.get(1) instanceof Float f) args.set(1, (MobAITweaks.getModConfigValue("iron_golem_pickaxe_damage_boost", 100) * 0.01F + 1F) * f);
    }
    @Override public void setTarget(@Nullable LivingEntity target) {
        if(target == null) super.setTarget(null);
        else if(!target.getType().isIn(MobAITweaks.GOLEMS_NEVER_TARGET_AT_ALL_COSTS)) super.setTarget(target);
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        getDataTracker().set(DESPERATION, i);
    }
    @SuppressWarnings("all")
    @Override public int getSpecialCooldown() {
        return getDataTracker().get(DESPERATION);
    }
    @SuppressWarnings("all")
    @Override public String attackType() {
        return "DASH";
    }
}

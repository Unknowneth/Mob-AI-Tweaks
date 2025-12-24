package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndermanEntity.class)
public abstract class EndermanEntityMixin extends HostileEntity implements SpecialAttacksInterface, Angerable {
    EndermanEntityMixin(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }
    @Shadow protected abstract boolean teleportTo(double x, double y, double z);
    @Unique private static final TrackedData<Integer> DESPERATION = DataTracker.registerData(EndermanEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void implementSpecialAttack(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(DESPERATION, 0);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void runAwayFromDragonDuringDamagePhase(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("ender_dragon_rework")) goalSelector.add(0, new FleeEntityGoal<>(this, EnderDragonEntity.class, 128F, 1.0F, 1.25F, e -> e instanceof EnderDragonEntity dragon && dragon.getPhaseManager().getCurrent() != null && dragon.getPhaseManager().getCurrent().isSittingOrHovering()) {
            @Override public void start() {
                setTarget(null);
                setAngerTime(0);
                super.start();
            }
        });
        if(MobAITweaks.getModConfigValue("enderman_rework") && goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof MeleeAttackGoal)) goalSelector.add(2, new MeleeAttackGoal(this, 2F / 3F, false) {
            private int specialAttack = 0;
            @Override public void tick() {
                if(getTarget() instanceof EndermiteEntity) super.tick();
                if(getTarget() != null && ++specialAttack > 60) {
                    getLookControl().lookAt(getTarget(), 90F, 30F);
                    if(specialAttack % 10 == 0) if(specialAttack % 20 == 0) {
                        if(getRotationVector().dotProduct(getTarget().getPos().subtract(getPos())) > 0.5) attack(getTarget());
                        if(!handSwinging) swingHand(Hand.MAIN_HAND);
                        playSound(SoundEvents.ENTITY_ENDER_PEARL_THROW, 2.0F, 0.1F);
                        setAttacking(specialAttack != 0);
                    }
                    else if(teleportTo(getTarget().getX() - getTarget().getVelocity().getX() * 3, getTarget().getY(), getTarget().getZ() - getTarget().getVelocity().getZ() * 3)) {
                        setYaw(bodyYaw = headYaw);
                        if(specialAttack > 110) specialAttack = 0;
                        else setAttacking(true);
                        if(isAttacking()) setSpecialCooldown(10 * (specialAttack / 10 - 7));
                    }
                    if(handSwingTicks == 5) setAttacking(false);
                    getNavigation().stop();
                }
                else super.tick();
                if(isAttacking()) setSpecialCooldown(getSpecialCooldown() + 1);
                else setSpecialCooldown(0);
                setSprinting(specialAttack < 60);
            }
            @Override public boolean canStart() {
                return super.canStart() || specialAttack > 60;
            }
            @Override public void start() {
                specialAttack = 0;
                setSprinting(true);
                super.start();
                setAttacking(false);
            }
            @Override public void stop() {
                specialAttack = 0;
                setSprinting(false);
                super.stop();
                setSpecialCooldown(0);
            }
            @Override protected boolean canAttack(LivingEntity target) {
                return specialAttack > 60 ? specialAttack % 20 == 0 : super.canAttack(target);
            }
        });
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
        return "TELEPORT";
    }
}

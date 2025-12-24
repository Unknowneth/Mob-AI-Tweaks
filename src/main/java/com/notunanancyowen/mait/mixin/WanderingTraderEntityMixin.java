package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.HoldInHandsGoal;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WanderingTraderEntity.class)
public abstract class WanderingTraderEntityMixin extends MerchantEntity {
    public WanderingTraderEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "initGoals", at = @At("HEAD"))
    private void attemptToBlindTargets(CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("wandering_traders_blind_attackers")) return;
        ItemStack potionOfBlindness = new ItemStack(Items.SPLASH_POTION);
        potionOfBlindness.apply(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(MobAITweaks.BLINDNESS), (PotionContentsComponent potionContent) -> potionContent.with(MobAITweaks.BLINDNESS));
        this.goalSelector.add(0, new HoldInHandsGoal<>(this, potionOfBlindness, SoundEvents.ENTITY_WANDERING_TRADER_NO, wanderingTrader -> wanderingTrader.getAttacker() != null && canTarget(wanderingTrader.getAttacker()) && age - getLastAttackedTime() < 40) {
            @Override public void start() {
                super.start();
                stopUsingItem();
                setTarget(getAttacker());
            }
            @Override public void stop() {
                if(getTarget() != null) {
                    PotionEntity blinding = new PotionEntity(getWorld(), getX(), getEyeY(), getZ());
                    blinding.setOwner(WanderingTraderEntityMixin.this);
                    blinding.setItem(getMainHandStack());
                    blinding.setPosition(getX(), getBodyY(0.55), getZ());
                    double d = getTarget().getX() + getTarget().getVelocity().getX() - getX();
                    double e = getTarget().getEyeY() - 1.1F - getTarget().getY();
                    double f = getTarget().getZ() + getTarget().getVelocity().getZ() - getZ();
                    double g = Math.sqrt(d * d + f * f);
                    blinding.setVelocity(d, e + (g * 0.2) * Math.clamp((canSee(getTarget()) ? distanceTo(getTarget()) : Double.MAX_VALUE) / 8, 0, 1), f, 0.75F, 0F);
                    getWorld().spawnEntity(blinding);
                    setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    setTarget(null);
                }
                super.stop();
            }
            @Override public boolean shouldContinue() {
                return getAttacker() != null && canTarget(getAttacker()) && age - getLastAttackedTime() < 40;
            }
            @Override public boolean shouldRunEveryTick() {
                return shouldContinue();
            }
            @Override public void tick() {
                if(getTarget() == null || !canTarget(getAttacker())) return;
                getLookControl().lookAt(getTarget(), 60F, 60F);
                double targetDistance = canSee(getTarget()) ? distanceTo(getTarget()) : Double.MAX_VALUE;
                if(targetDistance > 8) {
                    getNavigation().startMovingTo(getTarget(),0.5d);
                    return;
                }
                else if(targetDistance < 4) {
                    lookAtEntity(getTarget(), 60f, 60f);
                    getMoveControl().strafeTo(-0.5F, 0F);
                }
                getNavigation().stop();
            }
        });
    }
}

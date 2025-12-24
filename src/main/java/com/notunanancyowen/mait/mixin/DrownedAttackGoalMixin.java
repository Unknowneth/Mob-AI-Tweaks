package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.entity.mob.DrownedEntity$DrownedAttackGoal")
public abstract class DrownedAttackGoalMixin extends ZombieAttackGoal {
    @Shadow @Final private DrownedEntity drowned;
    DrownedAttackGoalMixin(DrownedEntity drowned, double speed, boolean pauseWhenMobIdle) {
        super(drowned, speed, pauseWhenMobIdle);
    }
    @Override public void tick() {
        if(MobAITweaks.getModConfigValue("drowned_rework") && (drowned.getMainHandStack().isOf(Items.FISHING_ROD) || drowned.getOffHandStack().isOf(Items.FISHING_ROD)) && drowned.getTarget() != null) {
            double distance = drowned.squaredDistanceTo(drowned.getTarget());
            if(!drowned.getTarget().isSubmergedInWater() && drowned.canSee(drowned.getTarget()) && distance > 36d && distance < 169d && drowned.age % 60 == 0 && !mob.handSwinging) {
                FishingBobberEntity bob = new FishingBobberEntity(EntityType.FISHING_BOBBER, drowned.getWorld());
                bob.setPosition(drowned.getEyePos().add(drowned.getRotationVector()));
                bob.setVelocity(drowned.getTarget().getEyePos().add(drowned.getTarget().getVelocity()).subtract(drowned.getEyePos()).normalize().multiply(2d));
                bob.setOwner(mob);
                drowned.getWorld().spawnEntity(bob);
                drowned.swingHand(Hand.MAIN_HAND);
                drowned.setCurrentHand(Hand.MAIN_HAND);
            }
            else super.tick();
        }
        else super.tick();
    }
    @Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
    private void stopWhenTridentSomewhere1(CallbackInfoReturnable<Boolean> cir) {
        if(MobAITweaks.getModConfigValue("drowned_rework") && drowned instanceof SpecialAttacksInterface special && drowned.getWorld().getEntityById(special.getSpecialCooldown()) instanceof TridentEntity) cir.setReturnValue(false);
    }
    @Inject(method = "shouldContinue", at = @At("HEAD"), cancellable = true)
    private void stopWhenTridentSomewhere2(CallbackInfoReturnable<Boolean> cir) {
        if(MobAITweaks.getModConfigValue("drowned_rework") && drowned instanceof SpecialAttacksInterface special && drowned.getWorld().getEntityById(special.getSpecialCooldown()) instanceof TridentEntity) cir.setReturnValue(false);
    }
}

package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.entity.mob.DrownedEntity$TridentAttackGoal")
public abstract class DrownedTridentAttackGoalMixin extends ProjectileAttackGoal {
    @Unique private int giveUpTimer = 0;
    @Shadow @Final private DrownedEntity drowned;
    DrownedTridentAttackGoalMixin(RangedAttackMob mob, double mobSpeed, int minIntervalTicks, int maxIntervalTicks, float maxShootRange) {
        super(mob, mobSpeed, minIntervalTicks, maxIntervalTicks, maxShootRange);
    }
    @Override public void tick() {
        boolean crazyMode = drowned.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP);
        if(!MobAITweaks.getModConfigValue("drowned_rework") || FabricLoader.getInstance().isModLoaded("fintastic")) super.tick();
        else if(drowned instanceof SpecialAttacksInterface special && drowned.getWorld().getEntityById(special.getSpecialCooldown()) instanceof TridentEntity trident) {
            drowned.getNavigation().startMovingTo(trident, crazyMode ? 1.5d : 1.25d);
            if(drowned.getTarget() != null && drowned.getTarget().getBoundingBox().intersects(drowned.getBoundingBox()) && drowned.tryAttack(drowned.getTarget()) && !drowned.handSwinging) drowned.swingHand(Hand.MAIN_HAND);
            if(drowned.getBoundingBox().intersects(trident.getBoundingBox())) {
                drowned.equipStack(EquipmentSlot.MAINHAND, trident.getWeaponStack());
                trident.discard();
                special.setSpecialCooldown(-1);
                giveUpTimer = 0;
            }
            else if(drowned.getNavigation().isIdle()) if(++giveUpTimer > 200) {
                giveUpTimer = 0;
                if(drowned.getTarget() != null) {
                    trident.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
                    trident.setOwner(drowned.getTarget());
                    if(trident.getWeaponStack() != null) trident.getWeaponStack().setDamage(drowned.getRandom().nextInt(trident.getWeaponStack().getMaxDamage() / 4 * 3));
                }
                else trident.discard();
            }
            else {
                if(drowned.isOnGround() && drowned.squaredDistanceTo(trident.getX(), drowned.getY(), trident.getZ()) < Math.pow(drowned.getBoundingBox().getLengthX() + drowned.getBoundingBox().getLengthZ(), 2) && drowned.canSee(trident)) {
                    double yDist = trident.getY() - drowned.getEyeY();
                    double maxDistForJump = 0.9d * drowned.getScale() * drowned.getScaleFactor() + drowned.getJumpBoostVelocityModifier();
                    if(yDist > 0.2d && yDist < maxDistForJump) drowned.getJumpControl().setActive();
                }
                drowned.getMoveControl().strafeTo(1F, 0F);
                drowned.lookAtEntity(trident, 60F, 60F);
                drowned.getLookControl().lookAt(trident, 60F, 60F);
            }
            if(drowned.isSwimming() && drowned.canSee(trident)) {
                Vec3d swimToLook = trident.getPos().subtract(drowned.getPos()).normalize().multiply(drowned.getAttributeValue(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY)).multiply(crazyMode ? 0.08 : 0.04);
                drowned.addVelocity(swimToLook);
            }
            else if(drowned.isOnGround()) drowned.setSprinting(crazyMode);
        }
        else if(drowned.getTarget() != null) if(!drowned.isUsingItem()) {
            drowned.setCurrentHand(Hand.MAIN_HAND);
            drowned.lookAtEntity(drowned.getTarget(), 180F, 60F);
            drowned.getMoveControl().strafeTo(1F, 0F);
            drowned.getNavigation().stop();
            drowned.setSprinting(false);
        }
        else {
            super.tick();
            drowned.lookAtEntity(drowned.getTarget(), 60F, 60F);
            drowned.getMoveControl().strafeTo(crazyMode ? 1F : 0.5F, 0F);
        }
    }
    @Override public boolean shouldContinue() {
        if(MobAITweaks.getModConfigValue("drowned_rework") && !FabricLoader.getInstance().isModLoaded("fintastic")) return drowned.getTarget() != null && super.shouldContinue();
        return super.shouldContinue();
    }
    @Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
    private void canStillUse(CallbackInfoReturnable<Boolean> cir) {
        if(MobAITweaks.getModConfigValue("drowned_rework") && !FabricLoader.getInstance().isModLoaded("fintastic") && drowned instanceof SpecialAttacksInterface special && drowned.getWorld().getEntityById(special.getSpecialCooldown()) instanceof TridentEntity) cir.setReturnValue(true);
    }
    @Inject(method = "start", at = @At("TAIL"))
    private void resetSprinting(CallbackInfo ci) {
        drowned.setSprinting(false);
    }
    @Inject(method = "stop", at = @At("TAIL"))
    private void stopSprinting(CallbackInfo ci) {
        drowned.setSprinting(false);
    }
}

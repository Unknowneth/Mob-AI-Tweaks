package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.PiglinActivity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinEntity.class)
public abstract class PiglinEntityMixin extends AbstractPiglinEntity implements CrossbowUser, InventoryOwner, SpecialAttacksInterface {
    @Unique private static final TrackedData<Integer> DODGE_TIME = DataTracker.registerData(PiglinEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    PiglinEntityMixin(EntityType<? extends AbstractPiglinEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void trackData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(DODGE_TIME, 0);
    }
    @Inject(method = "postShoot", at = @At("TAIL"))
    private void onShoot(CallbackInfo ci) {
        if(getActiveHand() != null) swingHand(getActiveHand());
    }
    @Override public boolean canUseRangedWeapon(RangedWeaponItem weapon) {
        return weapon instanceof CrossbowItem;
    }
    @Inject(method = "getActivity", at = @At("TAIL"), cancellable = true)
    private void allowUsageOfModdedCrossbows(CallbackInfoReturnable<PiglinActivity> cir) {
        if(isAttacking() && (getMainHandStack().getItem() instanceof CrossbowItem || getOffHandStack().getItem() instanceof CrossbowItem)) cir.setReturnValue(PiglinActivity.CROSSBOW_HOLD);
    }
    @Override public void tickMovement() {
        super.tickMovement();
        if(getSpecialCooldown() == 0 && isOnGround() && isUsingItem() && getTargetInBrain() != null && distanceTo(getTargetInBrain()) < 2F) {
            setVelocity(getRotationVector().multiply(-1d, 0d, -1d).normalize().multiply(getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 2F).add(0d, 0.2d, 0d));
            setSpecialCooldown(10);
        }
        if(getSpecialCooldown() > 0) setSpecialCooldown(getSpecialCooldown() - 1);
        else if(getSpecialCooldown() < 0) setSpecialCooldown(getSpecialCooldown() + 1);
    }
    @Override public boolean isSprinting() {
        return super.isSprinting() || getSpecialCooldown() != 0;
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        getDataTracker().set(DODGE_TIME, i);
    }
    @SuppressWarnings("all")
    @Override public int getSpecialCooldown() {
        return getDataTracker().get(DODGE_TIME);
    }
    @SuppressWarnings("all")
    @Override public String attackType() {
        return "DODGE";
    }
}

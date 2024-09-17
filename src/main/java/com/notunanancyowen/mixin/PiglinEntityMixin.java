package com.notunanancyowen.mixin;

import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.PiglinActivity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinEntity.class)
public abstract class PiglinEntityMixin extends AbstractPiglinEntity implements CrossbowUser, InventoryOwner {
    PiglinEntityMixin(EntityType<? extends AbstractPiglinEntity> type, World world) {
        super(type, world);
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
}

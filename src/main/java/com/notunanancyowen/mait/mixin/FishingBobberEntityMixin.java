package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin extends ProjectileEntity {
    @Shadow private @Nullable Entity hookedEntity;
    @Shadow protected abstract void checkForCollision();
    @Shadow protected abstract void updateHookedEntityId(@Nullable Entity entity);
    FishingBobberEntityMixin(EntityType<? extends ProjectileEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "onSpawnPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileEntity;onSpawnPacket(Lnet/minecraft/network/packet/s2c/play/EntitySpawnS2CPacket;)V", shift = At.Shift.AFTER), cancellable = true)
    private void allowMobsToUseThis(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        if(getOwner() instanceof PathAwareEntity mob && (mob.getMainHandStack().isOf(Items.FISHING_ROD) || mob.getOffHandStack().isOf(Items.FISHING_ROD))) ci.cancel();
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileEntity;tick()V", shift = At.Shift.AFTER), cancellable = true)
    private void replaceFunctionality(CallbackInfo ci) {
        if(getOwner() instanceof LivingEntity mob) {
            boolean rightHand = mob.getMainHandStack().isOf(Items.FISHING_ROD);
            if(rightHand || mob.getOffHandStack().isOf(Items.FISHING_ROD)) {
                if((hookedEntity != null || getVelocity().lengthSquared() < 2.5000003E-7F || horizontalCollision || verticalCollision) && (rightHand ? mob.getMainHandStack() : mob.getOffHandStack()).getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "grappling_bobber")))) {
                    if(hookedEntity != null && !hookedEntity.isAlive()) {
                        discard();
                        ci.cancel();
                        return;
                    }
                    mob.addVelocity(getPos().subtract(getY() > mob.getEyeY() ? mob.getEyePos() : mob.getPos()).normalize().multiply(Math.min(distanceTo(mob) / 48d, 0.16d)));
                    mob.setVelocity(mob.getVelocity().multiply(0.98d));
                    mob.fallDistance = 0F;
                    if(horizontalCollision || verticalCollision) setVelocity(Vec3d.ZERO);
                    if(hookedEntity == null && getOwner() instanceof PlayerEntity) ci.cancel();
                }
                if(getOwner() instanceof PlayerEntity) return;
                if(mob.isDead()) {
                    discard();
                    mob.stopUsingItem();
                    return;
                }
                mob.setCurrentHand(mob.getActiveHand());
                float waterLevel = 0.0F;
                BlockPos blockPos = this.getBlockPos();
                FluidState fluidState = this.getWorld().getFluidState(blockPos);
                if(fluidState.isIn(FluidTags.WATER)) waterLevel = fluidState.getHeight(getWorld(), blockPos);
                if(hookedEntity != null) {
                    setVelocity(Vec3d.ZERO);
                    if(!hookedEntity.isRemoved() && hookedEntity.getWorld().getRegistryKey() == getWorld().getRegistryKey()) setPosition(hookedEntity.getX(), hookedEntity.getBodyY(0.8), hookedEntity.getZ());
                    else updateHookedEntityId(null);
                }
                else if(waterLevel > 0F) {
                    Vec3d oldVelocity = getVelocity();
                    double d = getY() + oldVelocity.y - (double)blockPos.getY() - (double)waterLevel;
                    if(Math.abs(d) < 0.01) d += Math.signum(d) * 0.1;
                    setVelocity(oldVelocity.getX() * 0.9, oldVelocity.getY() - d * (double)getRandom().nextFloat() * 0.2, oldVelocity.getZ() * 0.9);
                }
                else checkForCollision();
                if(waterLevel == 0F) setVelocity(getVelocity().add(0.0, -0.03, 0.0));
                move(MovementType.SELF, getVelocity());
                updateRotation();
                setVelocity(getVelocity().multiply(0.92));
                refreshPosition();
                if(age > 55) if(age > 58) {
                    if(hookedEntity != null) {
                        hookedEntity.addVelocity(mob.getEyePos().subtract(hookedEntity.getPos()).multiply(0.25d));
                        getWorld().sendEntityStatus(this, EntityStatuses.PULL_HOOKED_ENTITY);
                    }
                    discard();
                    mob.stopUsingItem();
                }
                else if(!mob.handSwinging) mob.swingHand(Hand.MAIN_HAND);
                ci.cancel();
            }
        }
    }
    @Inject(method = "use", at = @At("TAIL"), cancellable = true)
    private void damageOnGrapple(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        if(usedItem.getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "grappling_bobber")))) cir.setReturnValue(2);
    }
}

package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombifiedPiglinEntity.class)
public abstract class ZombifiedPiglinEntityMixin extends ZombieEntity implements CrossbowUser {
    ZombifiedPiglinEntityMixin(EntityType<? extends ZombieEntity> type, World world) {
        super(type, world);
    }
    @Unique private boolean hasCrossbow() {
        return (getMainHandStack() != null && getMainHandStack().getItem() instanceof CrossbowItem) || (getOffHandStack() != null && getOffHandStack().getItem() instanceof CrossbowItem);
    }
    @Inject(method = "initCustomGoals", at = @At("TAIL"))
    private void addNewAttacks(CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("zombie_pigmen_can_use_crossbows")) return;
        if(goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof ZombieAttackGoal)) {
            goalSelector.add(2, new ZombieAttackGoal(this, 1.0d, false) {
                @Override public boolean canStart() {
                    return super.canStart() && !hasCrossbow();
                }
            });
            goalSelector.add(2, new CrossbowAttackGoal<>(this, 1.0d, 8F) {
                private int crossbowCooldownTime = 0;
                @Override public void stop() {
                    setAttacking(false);
                    setTarget(null);
                    stopUsingItem();
                }
                @Override public void tick() {
                    Hand realHand = getMainHandStack() != null && getMainHandStack().getItem() instanceof CrossbowItem ? Hand.MAIN_HAND : Hand.OFF_HAND;
                    if(getTarget() != null) {
                        if(CrossbowItem.isCharged(getStackInHand(realHand))) {
                            if(squaredDistanceTo(getTarget().getPos()) > 64F) getNavigation().startMovingTo(getTarget(), 1.0d);
                            else if(++crossbowCooldownTime > 20 && getStackInHand(realHand).getItem() instanceof CrossbowItem) {
                                CrossbowItem.shootAll(getWorld(), ZombifiedPiglinEntityMixin.this, realHand, getStackInHand(realHand), 0.9F, (float)(14 - getWorld().getDifficulty().getId() * 4));
                                CrossbowItem.setCharged(getStackInHand(realHand), false);
                            }
                            if(!CrossbowItem.isCharged(getStackInHand(realHand))) swingHand(realHand);
                            if(!getNavigation().isFollowingPath()) getMoveControl().strafeTo(-0.5F, 0F);
                        }
                        else if(crossbowCooldownTime > 0) {
                            crossbowCooldownTime--;
                            getMoveControl().strafeTo(0.5F, 0F);
                        }
                        else if(!isUsingItem()) setCurrentHand(realHand);
                        else if(getItemUseTimeLeft() == 0) stopUsingItem();
                        getLookControl().lookAt(getTarget(), 60F, 60F);
                        lookAtEntity(getTarget(), 60F, 60F);
                        if(getControllingVehicle() != null) getControllingVehicle().setYaw(getYaw());
                    }
                }
            });
        }
        if(targetSelector.getGoals().removeIf(target -> target.getGoal() instanceof RevengeGoal)) targetSelector.add(1, new RevengeGoal(this, ZombifiedPiglinEntity.class).setGroupRevenge());
    }
    @Override public void shoot(LivingEntity target, ItemStack crossbow, ProjectileEntity projectile, float multiShotSpray) {
        shoot(this, target, projectile, multiShotSpray, 1.6F);
    }
    @Inject(method = "initEquipment", at = @At("TAIL"))
    private void spawnWithCrossbow(Random random, LocalDifficulty localDifficulty, CallbackInfo ci) {
        if(random.nextInt(10) == 5 && localDifficulty.getLocalDifficulty() > 2.5F) equipStack(EquipmentSlot.MAINHAND, MobAITweaks.getRandomCrossbow(random).getDefaultStack());
    }
}

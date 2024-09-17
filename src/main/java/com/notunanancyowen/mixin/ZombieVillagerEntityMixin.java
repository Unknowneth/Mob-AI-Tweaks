package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ZombieVillagerEntity.class)
public abstract class ZombieVillagerEntityMixin extends ZombieEntity implements CrossbowUser {
    @Shadow public abstract VillagerData getVillagerData();

    public ZombieVillagerEntityMixin(EntityType<? extends ZombieEntity> type, World world) {
        super(type, world);
    }
    @Unique private boolean hasCrossbow() {
        return (getMainHandStack() != null && getMainHandStack().getItem() instanceof CrossbowItem) || (getOffHandStack() != null && getOffHandStack().getItem() instanceof CrossbowItem);
    }
    @Override protected void initCustomGoals() {
        super.initCustomGoals();
        if(MobAITweaks.getModConfigValue("zombie_villager_special_attacks") && goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof ZombieAttackGoal)) {
            goalSelector.add(2, new ZombieAttackGoal(this, 1.0d, false) {
                @Override public boolean canStart() {
                    return super.canStart() && !hasCrossbow();
                }
                @Override public boolean shouldContinue() {
                    return getVillagerData().getProfession() == VillagerProfession.CLERIC ? (getMainHandStack() != null && getMainHandStack().getItem() instanceof SplashPotionItem) || (getOffHandStack() != null && getOffHandStack().getItem() instanceof SplashPotionItem) : super.shouldContinue();
                }
                @Override public void tick() {
                    if(getVillagerData().getProfession() == VillagerProfession.CLERIC && getTarget() != null) {
                        if(canSee(getTarget()) && squaredDistanceTo(getTarget()) < 144F && age % 60 == 0) {
                            PotionEntity healing = new PotionEntity(mob.getWorld(), mob.getX(), mob.getEyeY(), mob.getZ());
                            healing.setOwner(mob);
                            if(getMainHandStack() != null || getOffHandStack().getItem() != null) healing.setItem(getMainHandStack().getItem() instanceof SplashPotionItem ? getMainHandStack() : getOffHandStack());
                            healing.setVelocity(getTarget().getEyePos().subtract(mob.getEyePos()).normalize());
                            mob.getWorld().spawnEntity(healing);
                            swingHand(Hand.MAIN_HAND);
                        }
                        if(squaredDistanceTo(getTarget()) > 64F) mob.getNavigation().startMovingTo(getTarget(), 1.0d);
                        else mob.getNavigation().stop();
                        getLookControl().lookAt(getTarget(), 60F, 60F);
                        lookAtEntity(getTarget(), 60F, 60F);
                        if((getTarget() instanceof HostileEntity fellowHostile && fellowHostile.isUndead() && fellowHostile.getHealth() >= fellowHostile.getMaxHealth()) || (getAttacker() instanceof HostileEntity)) setTarget(null);
                    }
                    else super.tick();
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
                                CrossbowItem.shootAll(getWorld(), ZombieVillagerEntityMixin.this, realHand, getStackInHand(realHand), 0.9F, (float)(14 - getWorld().getDifficulty().getId() * 4));
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
            targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, true, fellowHostiles -> !equals(fellowHostiles) && fellowHostiles.isUndead() && fellowHostiles.getHealth() < fellowHostiles.getMaxHealth() && getVillagerData().getProfession() == VillagerProfession.CLERIC));
        }
        if(MobAITweaks.getModConfigValue("illagers_and_zombie_villagers_fight")) targetSelector.add(3, new ActiveTargetGoal<>(this, IllagerEntity.class, true));
    }
    @Override public void shoot(LivingEntity target, ItemStack crossbow, ProjectileEntity projectile, float multiShotSpray) {
        shoot(this, target, projectile, multiShotSpray, 1.6F);
    }
    @Override protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        super.initEquipment(random, localDifficulty);
        if(MobAITweaks.getModConfigValue("zombie_villager_special_attacks")) if(getVillagerData().getProfession() == VillagerProfession.CLERIC) {
            ItemStack potion = new ItemStack(Items.SPLASH_POTION, 1);
            PotionUtil.setPotion(potion, Potions.HARMING);
            equipStack(getMainHandStack() != null && !getMainHandStack().isEmpty() ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND, potion);
        }
        else if(getVillagerData().getProfession() == VillagerProfession.FLETCHER) equipStack(EquipmentSlot.MAINHAND, MobAITweaks.getRandomCrossbow(random).getDefaultStack());
    }
}

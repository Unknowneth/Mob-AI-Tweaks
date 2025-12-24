package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.*;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Arrays;

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
                private int getSplashPotionCooldownTime() {
                    int[] cooldowns = new int[3];
                    Arrays.fill(cooldowns, 60);
                    return MobAITweaks.getRangedAttackCooldown(ZombieVillagerEntityMixin.this, cooldowns);
                }
                @Override public boolean canStart() {
                    return super.canStart() && !hasCrossbow();
                }
                @Override public void tick() {
                    if(getVillagerData().getProfession() == VillagerProfession.FISHERMAN && (getMainHandStack().isOf(Items.FISHING_ROD) || getOffHandStack().isOf(Items.FISHING_ROD)) && getTarget() != null) {
                        double distance = squaredDistanceTo(getTarget());
                        if(canSee(getTarget()) && distance > 36d && distance < 169d && age % 60 == 0 && !mob.handSwinging) {
                            FishingBobberEntity bob = new FishingBobberEntity(EntityType.FISHING_BOBBER, getWorld());
                            bob.setPosition(getEyePos().add(getRotationVector()));
                            bob.setVelocity(getTarget().getEyePos().add(getTarget().getVelocity()).subtract(getEyePos()).normalize().multiply(2d));
                            bob.setOwner(mob);
                            getWorld().spawnEntity(bob);
                            swingHand(Hand.MAIN_HAND);
                            setCurrentHand(Hand.MAIN_HAND);
                        }
                        else super.tick();
                    }
                    else if(getVillagerData().getProfession() == VillagerProfession.CLERIC && ((getMainHandStack() != null && getMainHandStack().getItem() instanceof SplashPotionItem) || (getOffHandStack() != null && getOffHandStack().getItem() instanceof SplashPotionItem)) && getTarget() != null) {
                        if(canSee(getTarget()) && squaredDistanceTo(getTarget()) < 144d && age % getSplashPotionCooldownTime() == 0) {
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
                        if((getTarget() instanceof HostileEntity fellowHostile && fellowHostile.hasInvertedHealingAndHarm() && fellowHostile.getHealth() >= fellowHostile.getMaxHealth()) || (getAttacker() instanceof HostileEntity)) setTarget(null);
                    }
                    else super.tick();
                }
            });
            goalSelector.add(2, new CrossbowAttackGoal<>(this, 1.0d, 8F) {
                private int crossbowCooldownTime = 0;
                private int getCrossbowCooldownTime() {
                    int[] cooldowns = new int[3];
                    Arrays.fill(cooldowns, 40);
                    return MobAITweaks.getRangedAttackCooldown(ZombieVillagerEntityMixin.this, cooldowns) / 2;
                }
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
                            else if(++crossbowCooldownTime > getCrossbowCooldownTime() && getStackInHand(realHand).getItem() instanceof CrossbowItem crossbowItem) crossbowItem.shootAll(getWorld(), ZombieVillagerEntityMixin.this, realHand, getStackInHand(realHand), 0.9F, (float)(14 - getWorld().getDifficulty().getId() * 4), getTarget());
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
            targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, true, fellowHostiles -> !equals(fellowHostiles) && fellowHostiles.hasInvertedHealingAndHarm() && fellowHostiles.getHealth() < fellowHostiles.getMaxHealth() && getVillagerData().getProfession() == VillagerProfession.CLERIC));
        }
        if(MobAITweaks.getModConfigValue("illagers_and_zombie_villagers_fight") && !MobAITweaks.getModConfigValue("illagers_and_normal_zombies_fight")) targetSelector.add(!MobAITweaks.getModConfigValue("illagers_and_zombies_prioritize_player") ? 2 : 3, new ActiveTargetGoal<>(this, IllagerEntity.class, true));
    }
    @Override protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        super.initEquipment(random, localDifficulty);
        if(MobAITweaks.getModConfigValue("zombie_villager_special_attacks")) if(getVillagerData().getProfession() == VillagerProfession.CLERIC) {
            ItemStack potion = Items.SPLASH_POTION.getDefaultStack();
            potion.apply(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.HARMING), (PotionContentsComponent potionContent) -> potionContent.with(Potions.HARMING));
            equipStack(getMainHandStack() != null && !getMainHandStack().isEmpty() ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND, potion);
        }
        else if(getVillagerData().getProfession() == VillagerProfession.FLETCHER) {
            equipStack(EquipmentSlot.MAINHAND, MobAITweaks.getRandomCrossbow(random).getDefaultStack());
            if(getWorld().getDifficulty().getId() > 2 && localDifficulty.isAtLeastHard()) equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SPECTRAL_ARROW, (int)localDifficulty.getLocalDifficulty() + 1));
        }
        else if(getVillagerData().getProfession() == VillagerProfession.FISHERMAN) equipStack(getMainHandStack().isEmpty() ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND, Items.FISHING_ROD.getDefaultStack());
        else if(getVillagerData().getProfession() == VillagerProfession.CARTOGRAPHER) equipStack(getMainHandStack().isEmpty() ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND, Items.SPYGLASS.getDefaultStack());
        else if(getVillagerData().getProfession() == VillagerProfession.BUTCHER && !getMainHandStack().isIn(ItemTags.AXES)) equipStack(EquipmentSlot.MAINHAND, Items.IRON_AXE.getDefaultStack());
    }
}

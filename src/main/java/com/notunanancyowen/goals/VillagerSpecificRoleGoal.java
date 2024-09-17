package com.notunanancyowen.goals;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillagerProfession;

import java.util.EnumSet;

public class VillagerSpecificRoleGoal extends Goal {
    private final VillagerEntity mob;
    private final int searchRadius;
    private LivingEntity target;
    private int activityTicker = 0;

    public VillagerSpecificRoleGoal(MerchantEntity mob, int searchRadius) {
        this.mob = (VillagerEntity) mob;
        this.searchRadius = searchRadius;
    }

    @Override
    public boolean canStart() {
        return !mob.isSleeping();
    }

    @Override
    public boolean shouldContinue() {
        return !mob.isSleeping();
    }

    @Override
    public boolean canStop() {
        return mob.isSleeping();
    }

    @Override
    public void tick() {
        Box inflatedHitbox = mob.getBoundingBox();
        inflatedHitbox = inflatedHitbox.withMinX(inflatedHitbox.minX - searchRadius).withMinY(inflatedHitbox.minY - searchRadius).withMinZ(inflatedHitbox.minZ - searchRadius).withMaxX(inflatedHitbox.maxX + searchRadius).withMaxY(inflatedHitbox.maxY + searchRadius).withMaxZ(inflatedHitbox.maxZ + searchRadius);
        if (++activityTicker > 100) activityTicker = 0;
        VillagerProfession prof = mob.getVillagerData().getProfession();
        if (prof == VillagerProfession.ARMORER || prof == VillagerProfession.TOOLSMITH || prof == VillagerProfession.WEAPONSMITH) {
            if (target instanceof LivingEntity) {
                ItemStack iron = new ItemStack(Items.IRON_INGOT, 1);
                setControls(EnumSet.of(Control.MOVE, Control.LOOK));
                mob.setStackInHand(Hand.MAIN_HAND, iron);
                mob.getLookControl().lookAt(target, 60f, 60f);
                mob.lookAtEntity(target, 60f, 60f);
                if (mob.distanceTo(target) > 3) mob.getNavigation().startMovingTo(target, 1d);
                else if (target.getHealth() >= target.getMaxHealth()) {
                    mob.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    target = null;
                    getControls().clear();
                } else if (activityTicker % 20 == 0) target.heal(6f);
            } else if (activityTicker == 0)
                for (var ironGolem : mob.getWorld().getEntitiesByType(EntityType.IRON_GOLEM, inflatedHitbox, ironGolems -> ironGolems.distanceTo(mob) < searchRadius && ironGolems.getHealth() < ironGolems.getMaxHealth() && ironGolems.getTarget() == null)) {
                    target = ironGolem;
                    break;
                }
        } else if (prof == VillagerProfession.CLERIC) {
            if (target instanceof LivingEntity) {
                ItemStack fakeItem = new ItemStack(Items.SPLASH_POTION, 1);
                PotionUtil.setPotion(fakeItem, Potions.HEALING);
                if (target.getHealth() >= target.getMaxHealth()) {
                    getControls().clear();
                    target = null;
                    mob.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    return;
                }
                setControls(EnumSet.of(Control.MOVE, Control.LOOK));
                mob.setStackInHand(Hand.MAIN_HAND, fakeItem);
                mob.getLookControl().lookAt(target, 60f, 60f);
                mob.lookAtEntity(target, 60f, 60f);
                if (activityTicker % 30 == 0) {
                    PotionEntity healing = new PotionEntity(mob.getWorld(), mob.getX(), mob.getEyeY(), mob.getZ());
                    healing.setOwner(mob);
                    healing.setItem(fakeItem);
                    healing.setVelocity(target.getEyePos().subtract(mob.getEyePos()).normalize());
                    mob.getWorld().spawnEntity(healing);
                }
                if (mob.distanceTo(target) > 10) mob.getNavigation().startMovingTo(target, 1d);
            } else if (activityTicker == 0)
                for (var friend : mob.getWorld().getOtherEntities(mob, inflatedHitbox, friends -> (friends.getClass().getPackageName().contains("guardvillagers") || friends instanceof IronGolemEntity || friends instanceof MerchantEntity) && !friends.equals(mob) && mob.canSee(friends) && friends instanceof LivingEntity livingFriend && livingFriend.getHealth() < livingFriend.getMaxHealth() && friends.distanceTo(mob) < searchRadius)) {
                    if (friend instanceof LivingEntity livingFriend) target = livingFriend;
                    break;
                }
        } else if (prof == VillagerProfession.FARMER) {
            if (target instanceof AnimalEntity pet) {
                if (mob.distanceTo(pet) < 2) {
                    if (pet.getBreedingAge() < 0) {
                        pet.setBreedingAge(pet.getBreedingAge() + 3);
                        setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
                    } else {
                        pet.growUp(0);
                        getControls().clear();
                    }
                    mob.setStackInHand(Hand.MAIN_HAND, new ItemStack(pet.getType() == EntityType.CHICKEN ? Items.WHEAT_SEEDS : Items.WHEAT, 1));
                    if (mob.getRandom().nextBoolean() && activityTicker % 60 == 0 && activityTicker > 0) {
                        target = null;
                        getControls().clear();
                    }
                } else mob.getNavigation().startMovingTo(pet, 0.5d);
                mob.getLookControl().lookAt(pet, 60f, 60f);
                mob.lookAtEntity(pet, 60f, 60f);
            } else if (mob.getRandom().nextInt(5) == 2 && activityTicker == 0)
                for (var pet : mob.getWorld().getEntitiesByClass(AnimalEntity.class, inflatedHitbox, pets -> pets.isBaby() && mob.canSee(pets) && pets.distanceTo(mob) < 16)) {
                    target = pet;
                    break;
                }
        } else if (prof == VillagerProfession.SHEPHERD) {
            if (target instanceof SheepEntity sheep) {
                setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
                if (mob.distanceTo(sheep) < 1) {
                    if (sheep.isShearable() && activityTicker % 20 == 0 && !sheep.isSheared()) {
                        sheep.sheared(mob.getSoundCategory());
                        target = null;
                        getControls().clear();
                    }
                    mob.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.SHEARS, 1));
                } else mob.getNavigation().startMovingTo(sheep, 0.5d);
                mob.getLookControl().lookAt(sheep, 60f, 60f);
                mob.lookAtEntity(sheep, 60f, 60f);
            } else if (activityTicker == 0)
                for (var pet : mob.getWorld().getEntitiesByClass(SheepEntity.class, inflatedHitbox, sheep -> !sheep.isBaby() && !sheep.isSheared() && sheep.isShearable() && mob.canSee(sheep) && mob.distanceTo(sheep) < 16)) {
                    target = pet;
                    break;
                }

        }
    }
}

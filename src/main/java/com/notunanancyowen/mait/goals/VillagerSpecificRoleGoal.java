package com.notunanancyowen.mait.goals;

import com.google.common.util.concurrent.AtomicDouble;
import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;

import java.util.EnumSet;

public class VillagerSpecificRoleGoal extends Goal {
    private final VillagerEntity mob;
    private final int searchRadius;
    private int activityTicker = 0;
    public VillagerSpecificRoleGoal(VillagerEntity mob, int searchRadius) {
        this.mob = mob;
        this.searchRadius = searchRadius;
        setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.TARGET));
    }
    @Override public boolean canStart() {
        if(mob.isBaby()) return false;
        long time = mob.getWorld().getTimeOfDay() % 24000L;
        return !mob.isSleeping() && ((time >= 2000 && time <= 9000) || (time >= 11000 && time <= 12000) || mob.getVillagerData().getProfession() == VillagerProfession.NITWIT || shouldRunEveryTick());
    }
    @Override public boolean shouldRunEveryTick() {
        return mob.getTarget() != null || mob.isAttacking();
    }
    @Override public void stop() {
        mob.clearPositionTarget();
        mob.setAttacking(false);
        mob.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
    }
    @Override public void tick() {
        LivingEntity target = mob.getTarget();
        if(target != null && target.isDead()) {
            mob.setTarget(null);
            return;
        }
        Box inflatedHitbox = mob.getBoundingBox();
        inflatedHitbox = inflatedHitbox.withMinX(inflatedHitbox.minX - searchRadius).withMinY(inflatedHitbox.minY - searchRadius).withMinZ(inflatedHitbox.minZ - searchRadius).withMaxX(inflatedHitbox.maxX + searchRadius).withMaxY(inflatedHitbox.maxY + searchRadius).withMaxZ(inflatedHitbox.maxZ + searchRadius);
        if(++activityTicker > 100) activityTicker = 0;
        VillagerProfession prof = mob.getVillagerData().getProfession();
        if (prof == VillagerProfession.ARMORER || prof == VillagerProfession.TOOLSMITH || prof == VillagerProfession.WEAPONSMITH) {
            if (target instanceof LivingEntity) {
                ItemStack iron = new ItemStack(Items.IRON_INGOT, 1);
                mob.setStackInHand(Hand.MAIN_HAND, iron);
                mob.getLookControl().lookAt(target, 60f, 60f);
                mob.lookAtEntity(target, 60f, 60f);
                if (mob.distanceTo(target) > 3) mob.getNavigation().startMovingTo(target,0.8d);
                else if (target.getHealth() >= target.getMaxHealth()) {
                    mob.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    mob.setTarget(null);
                }
                else if (activityTicker % 20 == 0) {
                    if(target.getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.HAPPY_VILLAGER, target.getParticleX(0.5), target.getRandomBodyY(), target.getParticleZ(0.5), 6, 0.1, 0.1, 0.1, 0);
                    target.getWorld().playSoundFromEntity(target, SoundEvents.BLOCK_ANVIL_USE, target.getSoundCategory(), 1f, target.getSoundPitch());
                    target.heal(6f);
                }
            }
            else if(activityTicker == 0) for (var ironGolem : mob.getWorld().getEntitiesByType(EntityType.IRON_GOLEM, inflatedHitbox, ironGolems -> ironGolems.distanceTo(mob) < searchRadius && ironGolems.getHealth() < ironGolems.getMaxHealth() && mob.canSee(ironGolems) && ironGolems.getTarget() == null)) {
                mob.setTarget(ironGolem);
                break;
            }
        }
        else if (prof == VillagerProfession.CLERIC) {
            if (target instanceof LivingEntity) {
                ItemStack fakeItem = new ItemStack(Items.SPLASH_POTION, 1);
                fakeItem.apply(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.HEALING), (PotionContentsComponent potionContent) -> potionContent.with(Potions.HEALING));
                if (target.getHealth() >= target.getMaxHealth()) {
                    mob.setTarget(null);
                    if(mob.getMainHandStack().equals(fakeItem)) mob.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    return;
                }
                mob.getLookControl().lookAt(target, 60f, 60f);
                double targetDistance = mob.canSee(target) ? mob.distanceTo(target) : Double.MAX_VALUE;
                if (mob.canSee(target) && activityTicker % 15 == 0) if(activityTicker % 30 == 0 && !mob.getMainHandStack().isEmpty() && targetDistance < 12) {
                    PotionEntity healing = new PotionEntity(mob.getWorld(), mob.getX(), mob.getEyeY(), mob.getZ());
                    healing.setOwner(mob);
                    healing.setItem(mob.getMainHandStack());
                    healing.setPosition(mob.getX(), mob.getBodyY(0.55), mob.getZ());
                    double d = target.getX() + target.getVelocity().getX() - mob.getX();
                    double e = target.getEyeY() - 1.1F - mob.getY();
                    double f = target.getZ() + target.getVelocity().getZ() - mob.getZ();
                    double g = Math.sqrt(d * d + f * f);
                    healing.setVelocity(d, e + (g * 0.2) * Math.clamp(targetDistance / 8, 0, 1), f, 0.75F, 0F);
                    mob.getWorld().spawnEntity(healing);
                    mob.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    if(target.isPlayer()) mob.setTarget(null);
                }
                else if(mob.getMainHandStack().isEmpty()) {
                    activityTicker += 15;
                    mob.setStackInHand(Hand.MAIN_HAND, fakeItem);
                }
                if(targetDistance < 8) {
                    if(target instanceof VillagerEntity fellow) {
                        fellow.lookAtEntity(mob, 60F, 60F);
                        fellow.getLookControl().lookAt(mob, 60F, 60F);
                        fellow.getNavigation().stop();
                        if(fellow.getWorld() instanceof ServerWorld server) fellow.getBrain().stopAllTasks(server, fellow);
                    }
                    if(targetDistance < 4) {
                        mob.lookAtEntity(target, 60f, 60f);
                        mob.getMoveControl().strafeTo(-0.5F, 0F);
                    }
                    mob.getNavigation().stop();
                }
                else mob.getNavigation().startMovingTo(target,0.75d);
            }
            else if (activityTicker == 0) for (var friend : mob.getWorld().getOtherEntities(mob, inflatedHitbox, friends -> friends.getType().isIn(MobAITweaks.HEALABLE_BY_CLERIC) && !friends.equals(mob) && mob.canSee(friends) && friends instanceof LivingEntity livingFriend && livingFriend.getHealth() < livingFriend.getMaxHealth() && friends.distanceTo(mob) < searchRadius)) {
                if(friend instanceof LivingEntity livingFriend) mob.setTarget(livingFriend);
                break;
            }
        }
        else if(prof == VillagerProfession.FARMER) {
            if (mob.getAttacker() != null) {
                stop();
                mob.setTarget(null);
            }
            else if (target instanceof AnimalEntity pet) {
                if (mob.distanceTo(pet) < 1.5) {
                    pet.getNavigation().stop();
                    pet.getLookControl().lookAt(mob);
                    int i = (int)(pet.getBreedingAge() / 20F * 0.1F);
                    pet.growUp(-i, true);
                    if(pet.getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pet.getParticleX(0.5), pet.getRandomBodyY(), pet.getParticleZ(0.5), i, 0.1, 0.1, 0.1, 0);
                    mob.setStackInHand(Hand.MAIN_HAND, new ItemStack(pet.getType() == EntityType.CHICKEN ? Items.WHEAT_SEEDS : Items.WHEAT, 1));
                    if(mob.getRandom().nextBoolean() && activityTicker % 60 == 0 && activityTicker > 0) mob.setTarget(null);
                    if(!pet.isBaby()) mob.setTarget(null);
                }
                else mob.getNavigation().startMovingTo(pet,0.5d);
                mob.getLookControl().lookAt(pet, 60f, 60f);
                mob.lookAtEntity(pet, 60f, 60f);
            }
            else if (activityTicker == 0) for (var pet : mob.getWorld().getEntitiesByClass(AnimalEntity.class, inflatedHitbox, pets -> pets.isBaby() && mob.canSee(pets) && pets.distanceTo(mob) < 16)) {
                mob.setTarget(pet);
                break;
            }
        }
        else if(prof == VillagerProfession.FISHERMAN) {
            if(mob.getAttacker() != null) {
                stop();
                return;
            }
            if(activityTicker == 0) {
                BlockPos.Mutable b = new BlockPos.Mutable();
                AtomicDouble atomicDouble = new AtomicDouble(16 * 16);
                BlockPos.stream(mob.getBoundingBox().expand(16)).forEach(bp -> {
                    if(mob.getWorld().getBlockState(bp).isOf(Blocks.WATER) && mob.getBlockPos().getSquaredDistance(bp) < atomicDouble.get()) {
                        b.set(bp);
                        atomicDouble.set(mob.getBlockPos().getSquaredDistance(b));
                    }
                });
                if(mob.isAttacking()) {
                    if(mob.getRandom().nextInt(10) == 0) {
                        if(mob.hasPositionTarget()) mob.clearPositionTarget();
                        mob.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                        mob.setAttacking(false);
                    }
                    else {
                        mob.setStackInHand(Hand.MAIN_HAND, Items.FISHING_ROD.getDefaultStack());
                        mob.setAttacking(true);
                        mob.setPositionTarget(b, 1);
                        FishingBobberEntity bob = new FishingBobberEntity(EntityType.FISHING_BOBBER, mob.getWorld()) {
                            @Override public void tick() {
                                if(getOwner() instanceof VillagerEntity v && v.getVillagerData().getProfession() == VillagerProfession.FISHERMAN) {
                                    if(!v.getMainHandStack().isOf(Items.FISHING_ROD)) {
                                        discard();
                                        return;
                                    }
                                    double sqrDist = squaredDistanceTo(v);
                                    if(sqrDist > 4) {
                                        v.getLookControl().lookAt(this, 60F, 60F);
                                        v.lookAtEntity(this, 60F, 60F);
                                    }
                                    if(v.isTouchingWater()) {
                                        v.getMoveControl().strafeTo(-0.5F, 0F);
                                        v.getJumpControl().setActive();
                                    }
                                    else if(isTouchingWater() && sqrDist < 9) v.getMoveControl().strafeTo(-0.5F, 0F);
                                    else if(sqrDist > 64) v.getMoveControl().strafeTo(0.5F, 0F);
                                }
                                super.tick();
                            }
                        };
                        bob.setPos(mob.getX(), mob.getBodyY(0.55), mob.getZ());
                        bob.setVelocity(b.toCenterPos().subtract(mob.getPos()).normalize().multiply(0.5));
                        bob.setOwner(mob);
                        mob.getWorld().spawnEntity(bob);
                        mob.setCurrentHand(Hand.MAIN_HAND);
                    }
                }
                else {
                    mob.setPositionTarget(b, 1);
                    mob.setStackInHand(Hand.MAIN_HAND, Items.FISHING_ROD.getDefaultStack());
                    mob.setAttacking(true);
                }
            }
            if(mob.hasPositionTarget()) {
                BlockPos bp = mob.getPositionTarget();
                if(mob.getBlockPos().getSquaredDistance(bp) > 9) mob.getNavigation().startMovingAlong(mob.getNavigation().findPathTo(mob.getPositionTarget(), 1), 0.5);
                else mob.getNavigation().stop();
                mob.getLookControl().lookAt(bp.getX() + 0.5, bp.getY() + 1.5, bp.getZ() + 0.5, 60F, 60F);
            }
        }
        else if(prof == VillagerProfession.SHEPHERD) {
            if(mob.getAttacker() != null) {
                stop();
                mob.setTarget(null);
            }
            else if(target instanceof SheepEntity sheep) {
                if (mob.distanceTo(sheep) < 1.5) {
                    if(sheep.isShearable() && activityTicker % 20 == 0 && !sheep.isSheared()) {
                        sheep.sheared(mob.getSoundCategory());
                        mob.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                        mob.setTarget(null);
                    }
                    else mob.setStackInHand(Hand.MAIN_HAND, Items.SHEARS.getDefaultStack());
                }
                else mob.getNavigation().startMovingTo(sheep,0.5d);
                mob.getLookControl().lookAt(sheep, 60f, 60f);
                mob.lookAtEntity(sheep, 60f, 60f);
            }
            else if (activityTicker == 0) for (var pet : mob.getWorld().getEntitiesByClass(SheepEntity.class, inflatedHitbox, sheep -> !sheep.isBaby() && !sheep.isSheared() && sheep.isShearable() && mob.canSee(sheep) && mob.distanceTo(sheep) < 16)) {
                mob.setTarget(pet);
                break;
            }
        }
        else if(prof == VillagerProfession.NITWIT) {
            if(target != null) {
                if(activityTicker > 0 && activityTicker % 10 == 0) if(activityTicker % 20 == 0) {
                    SnowballEntity snowball = new SnowballEntity(mob.getWorld(), mob) {
                        @Override protected void onEntityHit(EntityHitResult entityHitResult) {
                            super.onEntityHit(entityHitResult);
                            if(entityHitResult == null || entityHitResult.getEntity() == null) return;
                            entityHitResult.getEntity().damage(getDamageSources().thrown(this, mob), 1.0F);
                            if(entityHitResult.getEntity() instanceof LivingEntity l) if(getStack().isOf(Items.SAND)) l.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60 * getWorld().getDifficulty().getId()), getOwner());
                            else if(getStack().isOf(Items.POISONOUS_POTATO)) l.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60 * getWorld().getDifficulty().getId()), getOwner());
                        }
                    };
                    snowball.setItem(mob.getMainHandStack());
                    snowball.setPosition(mob.getX(), mob.getBodyY(0.55), mob.getZ());
                    if(snowball.getStack().isOf(Items.SAND)) mob.playSoundIfNotSilent(SoundEvents.BLOCK_SAND_PLACE);
                    snowball.setVelocity(mob, mob.getPitch() - 10F, mob.getHeadYaw(), 0F, 1F, 6F - mob.getWorld().getDifficulty().getId());
                    mob.getWorld().spawnEntity(snowball);
                    mob.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    if(mob.getRandom().nextInt(100) <= activityTicker || !mob.canTarget(target) || mob.distanceTo(target) < 4) mob.setTarget(null);
                }
                else if(mob.getRandom().nextBoolean()) mob.setStackInHand(Hand.MAIN_HAND, Items.EGG.getDefaultStack());
                else if(mob.getVillagerData().getType() == VillagerType.DESERT) mob.setStackInHand(Hand.MAIN_HAND, Items.SAND.getDefaultStack());
                else if(mob.getVillagerData().getType() == VillagerType.SNOW) mob.setStackInHand(Hand.MAIN_HAND, Items.SNOWBALL.getDefaultStack());
                else mob.setStackInHand(Hand.MAIN_HAND, Items.POISONOUS_POTATO.getDefaultStack());
                mob.getLookControl().lookAt(target, 60f, 60f);
                mob.lookAtEntity(target, 60f, 60f);
            }
            if(mob.getAttacker() != null && mob.canTarget(mob.getAttacker()) && mob.getAttacker().distanceTo(mob) > 8) mob.setTarget(mob.getAttacker());
        }
        if(shouldRunEveryTick() && mob.getWorld() instanceof ServerWorld server) mob.getBrain().stopAllTasks(server, mob);
    }
}

package com.notunanancyowen.mait.goals;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

public class SkeletonSpecificGoal extends Goal {
    private final AbstractSkeletonEntity mob;
    private final String attackType;
    private final int interval;
    private int cooldown = 0;
    private boolean ominous = false;
    private boolean crazyMobs = false;
    public SkeletonSpecificGoal(AbstractSkeletonEntity mob, int interval) {
        this.mob = mob;
        this.interval = interval;
        if(mob instanceof SpecialAttacksInterface specialMob) attackType = specialMob.attackType();
        else attackType = "NONE";
    }
    @Override public boolean canStart() {
        return mob.getTarget() != null;
    }
    @Override public boolean shouldContinue() {
        return canStart();
    }
    @Override public boolean canStop() {
        return !canStart();
    }
    @Override public boolean shouldRunEveryTick() {
        return canStart() || (mob instanceof SpecialAttacksInterface special && special.getSpecialCooldown() > 0);
    }
    @Override public void start() {
        crazyMobs = mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP);
        cooldown = mob.isBaby() ? interval * 2 : interval;
        if(mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) cooldown /= 3;
        if(mob instanceof SpecialAttacksInterface special) special.setSpecialCooldown(0);
        ominous = MobAITweaks.isOminous(mob.getTarget());
    }
    @Override public void stop() {
        if(mob.getControllingVehicle() instanceof MobEntity ride) {
            ride.setSidewaysSpeed(0F);
            ride.setSprinting(false);
            if(ride instanceof AbstractHorseEntity horse) horse.setAngry(false);
        }
        cooldown = 0;
        crazyMobs = ominous = false;
        if(mob instanceof SpecialAttacksInterface special) special.setSpecialCooldown(0);
    }
    @SuppressWarnings("all")
    @Override public void tick() {
        if(cooldown > 0) {
            if(mob.getControllingVehicle() instanceof MobEntity ride) if(ride.isOnGround() && cooldown > interval / 6 && cooldown < interval / 3 && ride.getType() == EntityType.SKELETON_HORSE && ride.hurtTime <= 0 && mob.getTarget() instanceof LivingEntity target) {
                ride.setYaw((float)Math.toDegrees(Math.atan2(target.getZ() - mob.getZ(), target.getX() - mob.getX())) - 90F);
                ride.setPitch(0f);
                ride.setVelocity(ride.getRotationVector().multiply(ride.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 2d));
                ride.setSidewaysSpeed(0F);
                if(ride instanceof AbstractHorseEntity horse && horse.isAngry()) horse.setAngry(false);
                if(ride instanceof SkeletonHorseEntity) ride.heal(2F);
                if(ride.getBoundingBox().intersects(target.getBoundingBox())) if(target.damage(mob.getDamageSources().mobAttack(ride), (float)mob.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * 2F)) {
                    cooldown = interval / 6;
                    target.addVelocity(ride.getVelocity().multiply((1d - target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)) * 3d));
                    ride.setVelocity(ride.getVelocity().negate());
                    ride.addVelocity(0d, -ride.getVelocity().getY() + 0.2d, 0d);
                }
                if(!ride.isSprinting()) ride.setSprinting(true);
            }
            else {
                if(cooldown == interval / 2 && ride instanceof AbstractHorseEntity horse) horse.setAngry(true);
                if(ride instanceof SkeletonHorseEntity horse) {
                    if(!ride.isSprinting() && !ride.getNavigation().isFollowingPath() && mob.getTarget() instanceof LivingEntity target && !horse.isAngry()) {
                        boolean strafeOtherWay = (ride.age + mob.getId() - ride.getId()) / 120 % 2 == 0;
                        if(mob.isLeftHanded()) strafeOtherWay = !strafeOtherWay;
                        if(mob.getId() % 3 == 0) strafeOtherWay = !strafeOtherWay;
                        if(ride.getId() % 4 == 0) strafeOtherWay = !strafeOtherWay;
                        ride.setYaw((float)Math.toDegrees(Math.atan2(target.getZ() - mob.getZ(), target.getX() - mob.getX())) - ((strafeOtherWay ? 180F : 0F)));
                        Vec3d realMovement = ride.getRotationVector().multiply(ride.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
                        ride.setVelocity(realMovement.getX(), ride.getVelocity().getY(), realMovement.getZ());
                    }
                    ride.heal(1F);
                }
                if(ride.isSprinting()) ride.setSprinting(false);
            }
            cooldown--;
        }
        else if(mob.getTarget() instanceof LivingEntity target) if(attackType.equals("DODGE") && mob.canSee(target) && (target.distanceTo(mob) <= 6 || target.distanceTo(mob) >= 15 || mob.getControllingVehicle() instanceof SkeletonHorseEntity)) {
            boolean decayeder = EntityType.getId(mob.getType()).equals(Identifier.of("terra_entity", "decayeder"));
            if(!decayeder) {
                AreaEffectCloudEntity aoe = new AreaEffectCloudEntity(mob.getWorld(), mob.getX(), mob.getY(), mob.getZ());
                aoe.setOwner(mob);
                aoe.setDuration(40);
                aoe.setParticleType(ParticleTypes.CRIMSON_SPORE);
                aoe.setRadius(0.4f);
                aoe.setPosition(mob.getPos());
                aoe.setRadiusGrowth(0.1f);
                aoe.setRadiusOnUse(0.1f);
                aoe.setPotionContents(new PotionContentsComponent(Potions.POISON));
                mob.getWorld().spawnEntity(aoe);
            }
            if(ominous || decayeder) for(int i = 0; i < (crazyMobs ? 4 : 2); i++) {
                SnowballEntity mushrooms = new SnowballEntity(mob.getWorld(), mob) {
                    @Override protected void onEntityHit(EntityHitResult entityHitResult) {
                        if(entityHitResult != null && entityHitResult.getEntity() instanceof LivingEntity hitEntity) {
                            if(getOwner() != null) hitEntity.damage(getOwner().getDamageSources().thrown(this, getOwner()), getWorld().getDifficulty().getId() + 1);
                            if(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) hitEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 0), this);
                            AreaEffectCloudEntity aoe = new AreaEffectCloudEntity(mob.getWorld(), mob.getX(), mob.getY(), mob.getZ());
                            aoe.setOwner(mob);
                            aoe.setDuration(20);
                            aoe.setWaitTime(10);
                            aoe.setParticleType(ParticleTypes.WARPED_SPORE);
                            aoe.setRadius(0.4f);
                            aoe.setPosition(getPos());
                            aoe.setRadiusGrowth(0.1f);
                            aoe.setRadiusOnUse(0.1f);
                            aoe.setPotionContents(new PotionContentsComponent(Potions.HARMING));
                            getWorld().spawnEntity(aoe);
                        }
                        super.onEntityHit(entityHitResult);
                    }
                    @Override protected void onBlockHit(BlockHitResult blockHitResult) {
                        super.onBlockHit(blockHitResult);
                        AreaEffectCloudEntity aoe = new AreaEffectCloudEntity(mob.getWorld(), mob.getX(), mob.getY(), mob.getZ());
                        aoe.setOwner(mob);
                        aoe.setDuration(20);
                        aoe.setWaitTime(10);
                        aoe.setParticleType(ParticleTypes.WARPED_SPORE);
                        aoe.setRadius(0.4f);
                        aoe.setPosition(getPos());
                        aoe.setRadiusGrowth(0.1f);
                        aoe.setRadiusOnUse(0.1f);
                        aoe.setPotionContents(new PotionContentsComponent(Potions.HARMING));
                        getWorld().spawnEntity(aoe);
                    }
                    @Override protected double getGravity() {
                        return 0.05;
                    }
                };
                mushrooms.setItem((FabricLoader.getInstance().isModLoaded("confluence") && decayeder ? Registries.ITEM.get(Identifier.of("confluence", "vile_mushroom")) : i % 2 == 0 ? Items.RED_MUSHROOM : Items.BROWN_MUSHROOM).getDefaultStack());
                mushrooms.setVelocity(0.5 * (mob.getRandom().nextDouble() - 0.5), mob.getRandom().nextDouble() * 0.2 + 0.1, (mob.getRandom().nextDouble() - 0.5) * 0.5);
                mushrooms.setPosition(mob.getEyePos().add(mushrooms.getVelocity()));
                mob.getWorld().spawnEntity(mushrooms);
            }
            Vec3d whichWayToDash = mob.getRotationVector(0f, mob.getYaw());
            if(target.distanceTo(mob) < 15) whichWayToDash = whichWayToDash.negate();
            if(mob.getControllingVehicle() instanceof LivingEntity ride && ride.getType() != EntityType.SKELETON_HORSE) ride.addVelocity(whichWayToDash.getX(), ride.getAttributeValue(EntityAttributes.GENERIC_JUMP_STRENGTH) * 0.3d, whichWayToDash.getZ());
            else mob.addVelocity(whichWayToDash.getX(), 0.2d, whichWayToDash.getZ());
            if(mob.getVehicle() == null && mob instanceof SpecialAttacksInterface special) special.setSpecialCooldown(target.distanceTo(mob) > 6 ? -10 : 10);
            cooldown = mob.isBaby() ? interval * 2 : interval;
            if(crazyMobs) cooldown /= 3;
        }
        else if(attackType.equals("FLIP") && mob instanceof SpecialAttacksInterface special) {
            if(!mob.isUsingItem() && special.getSpecialCooldown() != 0) {
                if(mob.getTarget() != null) {
                    mob.getLookControl().lookAt(mob.getTarget(), 60f, 60f);
                    mob.lookAtEntity(mob.getTarget(), 60f, 60f);
                }
                mob.swingHand(mob.getMainHandStack().isEmpty() ? Hand.MAIN_HAND : Hand.OFF_HAND);
                if(mob.handSwingTicks == -1) {
                    SnowballEntity snowball = new SnowballEntity(mob.getWorld(), mob) {
                        @Override protected void onEntityHit(EntityHitResult entityHitResult) {
                            if(entityHitResult != null && entityHitResult.getEntity() instanceof LivingEntity hitEntity) {
                                if(getOwner() != null) hitEntity.damage(getOwner().getDamageSources().thrown(this, getOwner()), getWorld().getDifficulty().getId() + 1);
                                if(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) hitEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 2), this);
                            }
                            if(ominous && getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 10, 0.1d, 0.1d, 0.1d, 0.5d);
                            super.onEntityHit(entityHitResult);
                        }
                        @Override protected void onBlockHit(BlockHitResult blockHitResult) {
                            super.onBlockHit(blockHitResult);
                            if(ominous && getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 10, 0.1d, 0.1d, 0.1d, 0.5d);
                        }
                        @Override public void tick() {
                            if(getWorld() instanceof ServerWorld server) for(int i = 0; i < 3; i++) {
                                Vec3d pos = getPos().subtract(getVelocity().multiply((float)i / 3F));
                                server.spawnParticles(ParticleTypes.SNOWFLAKE, pos.getX(), pos.getY(), pos.getZ(), 1, 0d, 0d, 0d, 0d);
                            }
                            if(ominous) {
                                double speed = getVelocity().length();
                                if(speed < (crazyMobs ? 0.5 : 0.35) && getOwner() instanceof MobEntity owner && owner.getTarget() != null) setVelocity(owner.getTarget().getEyePos().subtract(getPos()).normalize().multiply(speed * 1.2));
                                else setVelocity(getVelocity().multiply(1.2));
                                velocityDirty = true;
                            }
                            super.tick();
                        }
                    };
                    snowball.setVelocity(crazyMobs && mob.getTarget() != null ? mob.getTarget().getEyePos().subtract(mob.getEyePos()).normalize() : mob.getRotationVector().subtract(mob.getVelocity().multiply(0.2d)));
                    snowball.setPosition(mob.getEyePos());
                    if(ominous) {
                        snowball.setNoGravity(true);
                        snowball.setVelocity(snowball.getVelocity().multiply(0.05));
                    }
                    mob.getWorld().spawnEntity(snowball);
                    mob.getWorld().playSound(mob, mob.getBlockPos(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.HOSTILE, 0.5f, mob.getWorld().getRandom().nextFloat() * 0.4f + 0.8f);
                }
                if(Math.abs(special.getSpecialCooldown()) <= 1) {
                    cooldown = mob.isBaby() ? interval * 2 : interval;
                    if(crazyMobs) cooldown /= 3;
                }
            }
            else if(special.getSpecialCooldown() == 0 && ((mob.isUsingItem() && mob.getItemUseTime() >= 10 && target.distanceTo(mob) > 6) || (!(mob.getMainHandStack().getItem() instanceof BowItem) && !(mob.getOffHandStack().getItem() instanceof BowItem)))) if(mob.getControllingVehicle() instanceof LivingEntity ride) {
                ride.jump();
                if(ride instanceof AbstractHorseEntity horse) horse.setAngry(true);
                if(ride.getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.SNOWFLAKE, ride.getX(), ride.getY(), ride.getZ(), 15, 0.1d, 0d, 0.1d, 0.5d);
                special.setSpecialCooldown(19);
                Vec3d extraMovement = mob.isUsingItem() ? ride.getVelocity().multiply(crazyMobs ? 9d : 3d) : ride.getRotationVector().multiply(ride.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 2.0d).subtract(ride.getVelocity());
                ride.addVelocity(extraMovement.getX(), ride.getVelocity().getY() * 0.1d, extraMovement.getZ());
                ride.getWorld().playSound(ride, ride.getBlockPos(), SoundEvents.BLOCK_SNOW_FALL, SoundCategory.HOSTILE, 0.5f, ride.getWorld().getRandom().nextFloat() * 0.4f + 0.8f);
            }
            else {
                mob.jump();
                if(mob.getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.SNOWFLAKE, mob.getX(), mob.getY(), mob.getZ(), 15, 0.1d, 0d, 0.1d, 0.5d);
                special.setSpecialCooldown(mob.forwardSpeed > 0F ? -19 : 19);
                Vec3d extraMovement = mob.isUsingItem() ? mob.getVelocity().multiply(crazyMobs ? -9d : -3d) : mob.getRotationVector().multiply(mob.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 2.0d).add(mob.getVelocity());
                mob.addVelocity(-extraMovement.getX(), mob.getVelocity().getY() * 0.5d, -extraMovement.getZ());
                mob.getWorld().playSound(mob, mob.getBlockPos(), SoundEvents.BLOCK_SNOW_FALL, SoundCategory.HOSTILE, 0.5f, mob.getWorld().getRandom().nextFloat() * 0.4f + 0.8f);
            }
            if(special.getSpecialCooldown() != 0 && mob.getWorld() instanceof ServerWorld server) for(int i = 0; i < 3; i++) {
                Vec3d pos = mob.getPos().subtract(mob.getVelocity().multiply((float)i / 3F));
                server.spawnParticles(ParticleTypes.SNOWFLAKE, pos.getX(), pos.getY(), pos.getZ(), 1, 0.1d, 0.1d, 0.1d, 0.1d);
            }
        }
        else if(attackType.equals("GRENADE") && mob.canSee(target) && mob instanceof SpecialAttacksInterface special) {
            int specialCd = special.getSpecialCooldown();
            if(specialCd < 30) {
                mob.getNavigation().stop();
                mob.getLookControl().lookAt(target, 60f, 60f);
                mob.lookAtEntity(target, 60f, 60f);
                if(specialCd == 20) {
                    WitherSkullEntity skull = new WitherSkullEntity(mob.getWorld(), mob, mob.getEyePos());
                    skull.setVelocity(mob.getRotationVector().multiply(0.1d));
                    skull.setPosition(mob.getEyePos().add(mob.getRotationVector()));
                    mob.getWorld().spawnEntity(skull);
                    mob.getWorld().playSound(mob, mob.getBlockPos(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.5f, mob.getWorld().getRandom().nextFloat() * 0.4f + 0.8f);
                }
                if(mob.getControllingVehicle() instanceof AbstractHorseEntity horse) horse.setAngry(specialCd > 0 && specialCd < 20);
                if(specialCd == 1) mob.getWorld().playSound(mob, mob.getBlockPos(), SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 0.5f, mob.getWorld().getRandom().nextFloat() * 0.4f + 0.8f);
                special.setSpecialCooldown(++specialCd);
            }
            else {
                cooldown = mob.isBaby() ? interval * 2 : interval;
                if(crazyMobs) cooldown /= 3;
                special.setSpecialCooldown(0);
            }
            mob.getMoveControl().strafeTo(-0.3f, 0f);
        }
        else if(attackType.equals("NONE")) cooldown = mob.isBaby() ? interval * 2 : interval;
    }
}

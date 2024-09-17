package com.notunanancyowen.goals;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.BowItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

public class SkeletonSpecificGoal extends Goal {
    private final AbstractSkeletonEntity mob;
    private final int interval;
    private int cooldown = 0;
    public SkeletonSpecificGoal(AbstractSkeletonEntity mob, int interval) {
        this.mob = mob;
        this.interval = interval;
    }
    @Override public boolean canStart() {
        return mob.getTarget() != null;
    }
    @Override public boolean shouldContinue() {
        return mob.getTarget() != null;
    }
    @Override public boolean canStop() {
        return mob.getTarget() == null;
    }
    @Override public boolean shouldRunEveryTick() {
        return mob.getTarget() != null;
    }
    @Override public void start() {
        cooldown = mob.isBaby() ? interval * 2 : interval;
    }
    @Override public void stop() {
        if(mob.getControllingVehicle() instanceof MobEntity ride) ride.setSidewaysSpeed(0F);
        cooldown = 0;
        if(mob instanceof SpecialAttacksInterface special) special.setSpecialCooldown(0);
    }
    @SuppressWarnings("all")
    @Override public void tick() {
        if(cooldown > 0) {
            LivingEntity target = mob.getTarget();
            if(mob.getControllingVehicle() instanceof MobEntity ride) if(ride.isOnGround() && cooldown > interval / 6 && cooldown < interval / 3 && ride.getType() == EntityType.SKELETON_HORSE && ride.hurtTime <= 0 && target != null) {
                ride.setBodyYaw(ride.prevBodyYaw);
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
                    if(!ride.isSprinting() && !ride.getNavigation().isFollowingPath() && target != null && !horse.isAngry()) {
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
        else if(mob.getType() == EntityType.STRAY && ((mob.isUsingItem() && mob.getItemUseTime() >= 10 && mob.getTarget().distanceTo(mob) > 6) || (!(mob.getMainHandStack().getItem() instanceof BowItem) && !(mob.getOffHandStack().getItem() instanceof BowItem)))) {
            AreaEffectCloudEntity aoe = new AreaEffectCloudEntity(mob.getWorld(), mob.getPos().getX(), mob.getPos().getY(), mob.getPos().getZ()) {
                @Override public void tick() {
                    boolean originallyWaiting = true;
                    if(getOwner() != null) {
                        if(!getOwner().isUsingItem()) {
                            if(mob.getTarget() != null) {
                                mob.getLookControl().lookAt(mob.getTarget(), 60f, 60f);
                                mob.lookAtEntity(mob.getTarget(), 60f, 60f);
                            }
                            if(!getOwner().handSwinging) {
                                SnowballEntity snowball = new SnowballEntity(getOwner().getWorld(), getOwner()) {
                                    @Override protected void onEntityHit(EntityHitResult entityHitResult) {
                                        if(entityHitResult != null && entityHitResult.getEntity() instanceof LivingEntity hitEntity) {
                                            hitEntity.damage(getOwner().getDamageSources().thrown(this, getOwner()), 2);
                                            if(mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) hitEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 2), mob);
                                        }
                                        super.onEntityHit(entityHitResult);
                                    }
                                };
                                snowball.setVelocity(mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) && mob.getTarget() != null ? mob.getTarget().getEyePos().subtract(mob.getEyePos()).normalize() : getOwner().getRotationVector().subtract(getOwner().getVelocity()));
                                snowball.setPosition(getOwner().getEyePos());
                                getOwner().getWorld().spawnEntity(snowball);
                                getOwner().swingHand(getOwner().getMainHandStack().isEmpty() ? Hand.MAIN_HAND : Hand.OFF_HAND);
                            }
                            mob.setAttacking(false);
                        }
                        if(getOwner().getControllingVehicle() == null && !getOwner().isOnGround()) {
                            setPosition(getOwner().getPos());
                            if(getWorld() instanceof ServerWorld server) server.spawnParticles(getParticleType(), getOwner().getX(), getOwner().getY(), getOwner().getZ(), 3, 0.1d, 0d, 0.1d, 0.1d);
                        }
                        else if(getOwner().getControllingVehicle() != null && !getOwner().getControllingVehicle().isOnGround()) {
                            setPosition(getOwner().getControllingVehicle().getPos());
                            if(getWorld() instanceof ServerWorld server) server.spawnParticles(getParticleType(), getOwner().getControllingVehicle().getX(), getOwner().getControllingVehicle().getY(), getOwner().getControllingVehicle().getZ(), 5, 0.2d, 0d, 0.2d, 0.1d);
                        }
                        else originallyWaiting = false;
                    }
                    super.tick();
                    setWaiting(originallyWaiting);
                    if(!isWaiting()) {
                        mob.setAttacking(mob.getTarget() != null);
                        if(getOwner() != null && getOwner().getControllingVehicle() instanceof AbstractHorseEntity horse) horse.setAngry(false);
                    }
                    if(getOwner() != null) if(getOwner().getControllingVehicle() == null && !getOwner().isOnGround()) setPosition(getOwner().getPos());
                    else if(getOwner().getControllingVehicle() != null && !getOwner().getControllingVehicle().isOnGround()) setPosition(getOwner().getControllingVehicle().getPos());
                }
            };
            aoe.setOwner(mob);
            aoe.setDuration(5);
            aoe.setParticleType(ParticleTypes.SNOWFLAKE);
            aoe.setRadius(0.1f);
            aoe.setPosition(mob.getPos());
            aoe.setRadiusGrowth(0.6f);
            aoe.setPotion(Potions.MUNDANE);
            mob.getWorld().spawnEntity(aoe);
            var l = LivingEntity.class;
            if(mob.getControllingVehicle() instanceof LivingEntity ride) {
                try {
                    var k = l.getDeclaredMethod("jump");
                    k.setAccessible(true);
                    k.invoke(ride);
                }
                catch (Throwable ignore) {
                    try {
                        var k = l.getDeclaredMethod("jumpFromGround");
                        k.setAccessible(true);
                        k.invoke(ride);
                    }
                    catch (Throwable ignoreAsWell) {
                    }
                }
                if(ride instanceof AbstractHorseEntity horse) horse.setAngry(true);
                Vec3d extraMovement = mob.isUsingItem() ? ride.getVelocity().multiply(mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) ? 9d : 3d) : ride.getRotationVector().multiply(ride.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 2.0d).subtract(ride.getVelocity());
                ride.addVelocity(extraMovement.getX(), ride.getVelocity().getY() * 0.1d, extraMovement.getZ() );
            }
            else {
                try {
                    var k = l.getDeclaredMethod("jump");
                    k.setAccessible(true);
                    k.invoke((LivingEntity)mob);
                }
                catch (Throwable ignore) {
                    try {
                        var k = l.getDeclaredMethod("jumpFromGround");
                        k.setAccessible(true);
                        k.invoke((LivingEntity)mob);
                    }
                    catch (Throwable ignoreAsWell) {
                    }
                }
                Vec3d extraMovement = mob.isUsingItem() ? mob.getVelocity().multiply(mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) ? -9d : -3d) : mob.getRotationVector().multiply(mob.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 2.0d).add(mob.getVelocity());
                mob.addVelocity(-extraMovement.getX(), mob.getVelocity().getY() * 0.5d, -extraMovement.getZ());
            }
            cooldown = mob.isBaby() ? interval * 2 : interval;
        }
        else if(mob.getType() == EntityType.WITHER_SKELETON && mob.canSee(mob.getTarget()) && mob instanceof SpecialAttacksInterface special) {
            int specialCd = special.getSpecialCooldown();
            if(specialCd < 30) {
                mob.getNavigation().stop();
                mob.getLookControl().lookAt(mob.getTarget(), 60f, 60f);
                mob.lookAtEntity(mob.getTarget(), 60f, 60f);
                if(specialCd == 20) {
                    WitherSkullEntity skull = new WitherSkullEntity(mob.getWorld(), mob, 0, 0, 0) {
                        @Override public void tick() {
                            super.tick();
                            if(this.getOwner() instanceof HostileEntity shooter && shooter.getTarget() != null && shooter.getTarget().isAlive() && distanceTo(shooter.getTarget()) > 5) {
                                powerX = powerY = powerZ = 0d;
                                setVelocity(getVelocity().multiply(0.8d));
                                addVelocity(shooter.getTarget().getEyePos().subtract(getPos()).normalize().multiply(0.2d));
                            }
                            else if(powerX == 0d || powerY == 0d || powerZ == 0d) {
                                powerX = getVelocity().normalize().getX() * 0.1d;
                                powerY = getVelocity().normalize().getY() * 0.1d;
                                powerZ = getVelocity().normalize().getZ() * 0.1d;
                            }
                            velocityDirty = true;
                        }
                    };
                    skull.setVelocity(mob.getRotationVector().multiply(0.1d));
                    skull.setPosition(mob.getEyePos().add(mob.getRotationVector()));
                    mob.getWorld().spawnEntity(skull);
                }
                if(mob.getControllingVehicle() instanceof AbstractHorseEntity horse) horse.setAngry(specialCd > 0 && specialCd < 20);
                special.setSpecialCooldown(++specialCd);
            }
            else {
                cooldown = mob.isBaby() ? interval * 2 : interval;
                special.setSpecialCooldown(0);
            }
            mob.getMoveControl().strafeTo(-0.3f, 0f);
            mob.setAttacking(true);
        }
        else if(mob.getType() != EntityType.STRAY && mob.getType() != EntityType.WITHER_SKELETON) cooldown = mob.isBaby() ? interval * 2 : interval;
    }
}

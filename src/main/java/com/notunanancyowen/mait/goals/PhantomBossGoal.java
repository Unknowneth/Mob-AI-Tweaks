package com.notunanancyowen.mait.goals;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class PhantomBossGoal extends Goal {
    private final PhantomEntity phantom;
    private int attackTimer = 0;
    private int projectileCounter = 0;
    private boolean phase2 = false;
    protected PhantomBossGoal(PhantomEntity phantom) {
        this.phantom = phantom;
    }
    protected Vec3d getTargetPosition() {
        return Vec3d.ZERO;
    }
    protected void setTargetPosition(Vec3d where) {
    }
    @Override public boolean canStart() {
        return false;
    }
    @Override public void start() {
        attackTimer = 0;
        projectileCounter = 0;
        phase2 = phantom.getHealth() < phantom.getMaxHealth() * 0.5F;
    }
    @Override public void tick() {
        if(phantom.getTarget() == null) {
            double rotation = (phantom.getYaw() + (phantom.age % 900 < 450 ? 5 : -5) + 90) / 180 * Math.PI;
            phantom.setVelocity(Math.cos(rotation) * 0.5, 0, Math.sin(rotation) * 0.5);
            return;
        }
        if(attackTimer < 150) {
            if(phantom.getPos().distanceTo(getTargetPosition()) < 4) {
                double randomPoint = phantom.getRandom().nextDouble() * Math.PI * 2;
                setTargetPosition(phantom.getTarget().getEyePos().add(Math.cos(randomPoint) * 24, phantom.getRandom().nextBetween(16, 24), Math.sin(randomPoint) * 24));
            }
            Vec3d vec3d = phantom.getRotationVec(1.0F);
            Vec3d vec3d2 = phantom.getTarget().getPos().subtract(phantom.getPos()).normalize();
            if(++projectileCounter > 20 && vec3d.dotProduct(vec3d2) > 0.08 && phantom.getPos().distanceTo(phantom.getTarget().getPos()) > 16) {
                projectileCounter = 0;
                PotionEntity spit = new PotionEntity(phantom.getWorld(), phantom) {
                    @Override public void tick() {
                        super.tick();
                        if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 2, 0.01, 0.01, 0.01, 0.01);
                        if(age > 600) discard();
                    }
                };
                ItemStack ball = Items.SLIME_BLOCK.getDefaultStack();
                ball.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(phantom.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) ? Potions.STRONG_POISON : Potions.POISON));
                spit.setItem(ball);
                spit.setNoGravity(true);
                spit.setPosition(phantom.getEyePos().add(phantom.getVelocity()));
                double velocityMultiplier = phantom.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) ? 1.5d : 0.75d;
                if(phantom.getTarget() != null) spit.setVelocity(phantom.getTarget().getEyePos().add(phantom.getTarget().getVelocity()).subtract(spit.getPos()).normalize().multiply(velocityMultiplier));
                else spit.setVelocity(phantom.getRotationVector().multiply(velocityMultiplier));
                PotionEntity spit2 = new PotionEntity(phantom.getWorld(), phantom){
                    @Override public void tick() {
                        super.tick();
                        if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 2, 0.01, 0.01, 0.01, 0.01);
                        if(age > 600) discard();
                    }
                };
                spit2.setItem(ball);
                spit2.setNoGravity(true);
                spit2.setPosition(spit.getPos());
                int rotateMode = phantom.getRandom().nextInt(3);
                if(rotateMode == 0) spit2.setVelocity(spit.getVelocity().rotateX((float)(Math.PI / 9)));
                if(rotateMode == 1) spit2.setVelocity(spit.getVelocity().rotateY((float)(Math.PI / 9)));
                if(rotateMode == 2) spit2.setVelocity(spit.getVelocity().rotateZ((float)(Math.PI / 9)));
                PotionEntity spit3 = new PotionEntity(phantom.getWorld(), phantom){
                    @Override public void tick() {
                        super.tick();
                        if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 2, 0.01, 0.01, 0.01, 0.01);
                        if(age > 600) discard();
                    }
                };
                spit3.setItem(ball);
                spit3.setNoGravity(true);
                spit3.setPosition(spit.getPos());
                if(rotateMode == 0) spit3.setVelocity(spit.getVelocity().rotateX((float)(Math.PI / -9)));
                if(rotateMode == 1) spit3.setVelocity(spit.getVelocity().rotateY((float)(Math.PI / -9)));
                if(rotateMode == 2) spit3.setVelocity(spit.getVelocity().rotateZ((float)(Math.PI / -9)));
                phantom.getWorld().spawnEntity(spit);
                phantom.getWorld().spawnEntity(spit2);
                phantom.getWorld().spawnEntity(spit3);
                if(!phantom.isSilent()) phantom.getWorld().playSound(phantom, phantom.getBlockPos(), SoundEvents.BLOCK_SLIME_BLOCK_BREAK, SoundCategory.HOSTILE, 4f, 1f);
                if(phantom.getWorld() instanceof ServerWorld serverWorld) serverWorld.spawnParticles(ParticleTypes.ITEM_SLIME, phantom.getX(), phantom.getEyeY(), phantom.getZ(), phantom.getPhantomSize() * 2 + 4, 0.1, 0.1, 0.1, 0.2);
            }
            phantom.setVelocity(getTargetPosition().subtract(phantom.getPos()).normalize().multiply(0.75));
            attackTimer++;
        }
        else if(attackTimer < 250 && phase2) {
            if(attackTimer == 150) phantom.setLeftHanded(phantom.getRandom().nextBoolean());
            if(attackTimer < 180 || attackTimer == 200) setTargetPosition(phantom.getTarget().getEyePos());
            else setTargetPosition(getTargetPosition().add(phantom.getVelocity()));
            Vec3d orbit = getTargetPosition().multiply(1, 0, 1).subtract(phantom.getPos().multiply(1, 0, 1)).normalize();
            if(attackTimer < 200) {
                float rotation = (float)(Math.PI / 2);
                if(phantom.isLeftHanded()) rotation *= -1F;
                phantom.setVelocity(orbit.rotateY(rotation).add(orbit.multiply(getTargetPosition().multiply(1, 0, 1).distanceTo(phantom.getPos().multiply(1, 0, 1)) > 24 ? 0.1 : -0.1)).add(0, getTargetPosition().getY() > phantom.getY() - 20 ? 0.1 : -0.1, 0));
            }
            else {
                if(++projectileCounter > 3) {
                    projectileCounter = 0;
                    PotionEntity spit = new PotionEntity(phantom.getWorld(), phantom) {
                        @Override public void tick() {
                            super.tick();
                            if(age > 600) discard();
                        }
                        @Override protected double getGravity() {
                            return 0.03;
                        }
                    };
                    ItemStack ball = Items.SLIME_BLOCK.getDefaultStack();
                    ball.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(phantom.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) ? Potions.STRONG_POISON : Potions.POISON));
                    spit.setItem(ball);
                    spit.setPosition(phantom.getEyePos().add(phantom.getVelocity()));
                    spit.setVelocity(phantom.getVelocity().normalize().multiply(0.75F));
                    phantom.getWorld().spawnEntity(spit);
                }
                if(!phantom.isSilent() && attackTimer == 200) phantom.getWorld().playSound(phantom, phantom.getBlockPos(), SoundEvents.ENTITY_PHANTOM_SWOOP, SoundCategory.HOSTILE, 4f, 1f);
                phantom.setVelocity(orbit.multiply(0.75d));
                setTargetPosition(getTargetPosition().add(phantom.getVelocity()));
            }
            attackTimer++;
        }
        else if(attackTimer % 25 != 0) {
            phantom.setVelocity(getTargetPosition().subtract(phantom.getPos()).normalize());
            setTargetPosition(getTargetPosition().add(phantom.getVelocity()));
            if(phantom.getY() < phantom.getTarget().getY()) setTargetPosition(getTargetPosition().add(0, 10, 0));
            else attackTimer++;
            if(phantom.getTarget().getBoundingBox().intersects(phantom.getBoundingBox()) && phantom.tryAttack(phantom.getTarget())) phantom.getTarget().addVelocity(0, 1, 0);
        }
        else {
            setTargetPosition(phantom.getTarget().getEyePos());
            if(++attackTimer > (phase2 ? 300 : 200)) {
                attackTimer = 0;
                phase2 = phantom.getHealth() < phantom.getMaxHealth() * 0.5F;
            }
            if(!phantom.isSilent()) phantom.getWorld().playSound(phantom, phantom.getBlockPos(), SoundEvents.ENTITY_PHANTOM_SWOOP, SoundCategory.HOSTILE, 4f, 1f);
            projectileCounter = 0;
        }
    }
}

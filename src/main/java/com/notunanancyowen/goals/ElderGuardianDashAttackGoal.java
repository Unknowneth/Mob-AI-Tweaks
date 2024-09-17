package com.notunanancyowen.goals;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

public class ElderGuardianDashAttackGoal extends Goal {
    private final GuardianEntity mob;
    private Vec3d dashDirection= Vec3d.ZERO;
    private int dashTime = 0;
    private LivingEntity lastSeenTarget = null;
    public ElderGuardianDashAttackGoal(GuardianEntity mob) {
        this.mob = mob;
        setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    @Override public boolean canStart() {
        return mob.getHealth() < mob.getMaxHealth() * 0.8f && mob.getTarget() != null && mob.canSee(mob.getTarget()) && !mob.hasBeamTarget() && ((SpecialAttacksInterface)mob).getSpecialCooldown() <= 0;
    }
    @Override public boolean shouldContinue() {
        return mob.getTarget() != null || (dashTime > 0 && dashTime <= 5);
    }
    @Override public boolean canStop() {
        return mob.getTarget() == null || mob.hasBeamTarget() || dashTime > 5;
    }
    @Override public void start() {
        if(mob.getTarget() != null) {
            dashDirection = mob.getTarget().getPos().subtract(mob.getVelocity().multiply(4d)).subtract(mob.getPos()).normalize();
            mob.setVelocity(dashDirection.multiply(2.5d));
        }
        dashTime = 0;
        mob.velocityDirty = true;
        lastSeenTarget = mob.getTarget();
        ((SpecialAttacksInterface)mob).forceSpecialAttack();
    }
    @Override public void stop() {
        mob.setVelocity(Vec3d.ZERO);
        dashTime = 0;
        int dashCooldown = (int)(mob.getHealth() / mob.getMaxHealth() * 80f);
        if(mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) dashCooldown /= 3;
        ((SpecialAttacksInterface)mob).setSpecialCooldown(dashCooldown + 40);
    }
    @Override public boolean shouldRunEveryTick() {
        return true;
    }
    @SuppressWarnings("all")
    @Override public void tick() {
        ((SpecialAttacksInterface)mob).forceSpecialAttack();
        mob.getNavigation().stop();
        Vec3d lookDir = mob.getPos().add(dashDirection.multiply(10d));
        mob.getMoveControl().moveTo(lookDir.x, lookDir.y, lookDir.z, 2.5d);
        mob.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, lookDir);
        mob.getLookControl().lookAt(lookDir);
        if(lastSeenTarget != null) {
            mob.setTarget(lastSeenTarget);
            if (mob.getWorld().getDifficulty().getId() > 2 && (dashTime + 1) % 2 == 0 && mob.getHealth() < mob.getMaxHealth() * 0.3f) {
                ShulkerBulletEntity bullet = new ShulkerBulletEntity(mob.getWorld(), mob, lastSeenTarget, Direction.Axis.pickRandomAxis(mob.getRandom())) {
                    @Override public void onEntityHit(EntityHitResult entityHitResult) {
                        entityHitResult.getEntity().addVelocity(0, -2, 0);
                        entityHitResult.getEntity().setAir(entityHitResult.getEntity().getAir() - 1);
                        if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1, 0d, 0d, 0d, 0d);
                    }
                    @Override public void tick() {
                        if(getOwner() == null || !getOwner().isAlive()) discard();
                        else {
                            ShulkerBulletEntity thisBulletRightHere = this;
                            if(thisBulletRightHere instanceof SpecialAttacksInterface attacks) attacks.setSpecialCooldown(1);
                            super.tick();
                            setVelocity(getVelocity().multiply(0.7d));
                            this.velocityDirty  = true;
                        }
                    }
                    @Override public Text getName() {
                        return Text.translatable("mob-ai-tweaks.elder_guardian_projectile");
                    }
                };
                if(bullet instanceof SpecialAttacksInterface attacks) attacks.setSpecialCooldown(1);
                bullet.setOwner(mob);
                mob.getWorld().spawnEntity(bullet);
            }
            if (lastSeenTarget.squaredDistanceTo(mob) <= 16f || lastSeenTarget.squaredDistanceTo(mob.getPos().subtract(mob.getVelocity())) <= 16f) lastSeenTarget.damage(mob.getDamageSources().mobAttack(mob), (float)mob.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
        }
        if(mob.getVelocity().horizontalLengthSquared() > mob.getVelocity().y * mob.getVelocity().y ? mob.horizontalCollision : mob.verticalCollision) {
            dashTime = 5;
            mob.getWorld().createExplosion(mob, mob.getX(), mob.getY(), mob.getZ(), 3f, false, World.ExplosionSourceType.MOB);
        }
        dashTime++;
    }
}

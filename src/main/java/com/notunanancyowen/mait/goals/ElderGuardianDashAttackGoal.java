package com.notunanancyowen.mait.goals;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
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
        return mob.getTarget() != null || (dashTime > 0 && dashTime <= 15);
    }
    @Override public boolean canStop() {
        return mob.getTarget() == null || mob.hasBeamTarget() || dashTime > 15;
    }
    @Override public void start() {
        if(mob.getTarget() != null) {
            if(mob.getTarget().getY() < mob.getY() && !mob.isOnGround() && mob.distanceTo(mob.getTarget()) < 12) dashDirection = new Vec3d(0d, -4d, 0d);
            else dashDirection = mob.getTarget().getPos().subtract(mob.getVelocity().multiply(6d)).subtract(mob.getPos()).normalize();
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
        Vec3d lookDir = mob.getPos();
        boolean slam = dashDirection.getX() == 0 && dashDirection.getZ() == 0;
        if(slam) lookDir = lookDir.add(dashDirection.multiply(dashTime < 12 ? -10d : 20d));
        else lookDir = lookDir.add(dashDirection.multiply(10d));
        mob.getMoveControl().moveTo(lookDir.x, lookDir.y, lookDir.z, dashTime < 10 && !slam ? 0.1d : 2.5d);
        mob.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, lookDir);
        mob.getLookControl().lookAt(lookDir);
        if(dashTime < 10) {
            dashTime++;
            return;
        }
        if(lastSeenTarget != null) {
            if(mob.getWorld().getDifficulty().getId() > 2 && (dashTime + 1) % 2 == 0 && mob.getHealth() < mob.getMaxHealth() * 0.3f) {
                ShulkerBulletEntity bullet = new ShulkerBulletEntity(mob.getWorld(), mob, lastSeenTarget, Direction.Axis.pickRandomAxis(mob.getRandom()));
                if(bullet instanceof SpecialAttacksInterface attacks) attacks.setSpecialCooldown(1);
                bullet.setOwner(mob);
                mob.getWorld().spawnEntity(bullet);
            }
            if(lastSeenTarget.squaredDistanceTo(mob) <= 16f || lastSeenTarget.squaredDistanceTo(mob.getPos().subtract(mob.getVelocity())) <= 16f) lastSeenTarget.damage(mob.getDamageSources().mobAttack(mob), (float)mob.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
        }
        if(mob.getVelocity().horizontalLengthSquared() > mob.getVelocity().y * mob.getVelocity().y ? mob.horizontalCollision : mob.verticalCollision) {
            dashTime = 15;
            if(slam && dashDirection.getY() < 0 && mob.verticalCollision) {
                AreaEffectCloudEntity aoe = new AreaEffectCloudEntity(mob.getWorld(), mob.getX(), mob.getY(), mob.getZ());
                aoe.setOwner(mob);
                aoe.setDuration(40);
                aoe.setParticleType(ParticleTypes.BUBBLE);
                aoe.setRadius(4f);
                aoe.setPosition(mob.getPos());
                aoe.setRadiusGrowth(0.2f);
                aoe.setRadiusOnUse(0.0f);
                aoe.setPotionContents(new PotionContentsComponent(Potions.HARMING));
                mob.getWorld().spawnEntity(aoe);
            }
            mob.getWorld().createExplosion(mob, mob.getX(), mob.getY(), mob.getZ(), 3f, false, World.ExplosionSourceType.MOB);
        }
        dashTime++;
    }
}

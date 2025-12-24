package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets = "org.confluence.terraentity.entity.monster.demoneye.DemonEyeSurroundTargetGoal")
public abstract class DemonEyeSurroundTargetGoalMixin extends Goal {
    @Unique private int dashCooldown = 100;
    @Shadow @Final protected PathAwareEntity mob;
    @Shadow public Vec3d targetPos;
    @Override public boolean canStart() {
        if(mob.getTarget() == null) {
            dashCooldown = 100;
            return false;
        }
        if(mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) {
            dashCooldown--;
            if(dashCooldown < 0) {
                if(dashCooldown > -20) {
                    if(dashCooldown == -1 && mob.getWorld() instanceof ServerWorld server) server.spawnParticles(new DustColorTransitionParticleEffect(new Vector3f(1F, 0F, 0F), new Vector3f(0.9F, 0.1F, 0.1F).div(255F / 2F), 2F), mob.getX(), mob.getEyeY(), mob.getZ(), 9, 0.25, 0.25, 0.25, 0.25);
                    targetPos = mob.getTarget().getEyePos();
                    mob.setVelocity(targetPos.subtract(mob.getPos()).normalize().multiply(0.02d));
                    mob.getLookControl().lookAt(mob.getTarget());
                    mob.lookAtEntity(mob.getTarget(), 90F, 90F);
                }
                else if(dashCooldown > -40) {
                    mob.setVelocity(targetPos.subtract(mob.getPos()).normalize().multiply(2d));
                    mob.getLookControl().lookAt(mob.getPos().add(mob.getVelocity()));
                    mob.getWorld().getEntitiesByClass(PlayerEntity.class, mob.getBoundingBox().offset(mob.getVelocity()).expand(0.15D), (e) -> !e.isSpectator()).forEach(p -> mob.tryAttack(p));
                    if(mob.squaredDistanceTo(targetPos) <= mob.getVelocity().lengthSquared() || mob.horizontalCollision || mob.verticalCollision) dashCooldown = 100;
                }
                else dashCooldown = 100;
                return false;
            }
        }
        else dashCooldown = 100;
        return mob.getTarget().isAlive() && mob.getWorld().isNight();
    }
}

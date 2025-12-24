package com.notunanancyowen.mait.goals;

import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.SnowGolemEntity;

public class SnowGolemAttackGoal extends ProjectileAttackGoal {
    private final SnowGolemEntity mob;
    private int specialAttackTime = 100;
    private float lastRotation;
    public SnowGolemAttackGoal(SnowGolemEntity mob) {
        super(mob, 1.0d, 10, 20);
        this.mob = mob;
    }
    @Override public void tick() {
        if(specialAttackTime <= 0) {
            for(var hostiles : mob.getWorld().getOtherEntities(mob, mob.getBoundingBox().expand(20), h -> h.distanceTo(mob) < 20 && h instanceof HostileEntity h2 && h2.getTarget() != null && h2.getTarget().getId() == mob.getId())) if(hostiles instanceof HostileEntity h) mob.shootAt(h, 1f);
            if(--specialAttackTime <= -10) specialAttackTime = 100;
            mob.getJumpControl().setActive();
            mob.setYaw(lastRotation - specialAttackTime * 5.4f);
            mob.getNavigation().stop();
            return;
        }
        lastRotation = mob.getYaw();
        specialAttackTime--;
        super.tick();
    }
}

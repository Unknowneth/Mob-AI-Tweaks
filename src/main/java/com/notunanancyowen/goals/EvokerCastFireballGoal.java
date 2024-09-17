package com.notunanancyowen.goals;

import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.SpellcastingIllagerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;

import java.util.EnumSet;

public class EvokerCastFireballGoal extends Goal {
    private final SpellcastingIllagerEntity mob;
    public EvokerCastFireballGoal(SpellcastingIllagerEntity mob) {
        this.mob = mob;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }
    @Override public boolean canStart() {
        return mob.getTarget() != null && !mob.isSpellcasting() && ((SpecialAttacksInterface)mob).getSpecialCooldown() <= 0;
    }
    @Override public boolean shouldContinue() {
        return mob.getTarget() != null && !mob.isSpellcasting() && ((SpecialAttacksInterface)mob).getSpecialCooldown() <= 0;
    }
    @Override public boolean canStop() {
        return mob.getTarget() == null || mob.isSpellcasting() || ((SpecialAttacksInterface)mob).getSpecialCooldown() > 0;
    }
    @Override public boolean shouldRunEveryTick() {
        return true;
    }
    @Override public void tick() {
        if(mob.getTarget() != null) {
            mob.getNavigation().stop();
            mob.getLookControl().lookAt(mob.getTarget());
            mob.lookAtEntity(mob.getTarget(), 60f, 60f);
            int fireballCoolown = ((SpecialAttacksInterface)mob).getSpecialCooldown();
            if(mob.isSpellcasting() || fireballCoolown < -20) fireballCoolown = 21;
            else if(fireballCoolown == -10) {
                SmallFireballEntity fireball = new SmallFireballEntity(mob.getWorld(), mob, mob.getRotationVector().x, mob.getRotationVector().y, mob.getRotationVector().z);
                fireball.setPosition(mob.getEyePos().add(mob.getRotationVector()).subtract(0, 0.2d, 0));
                fireball.setVelocity(mob.getRotationVector());
                mob.getWorld().spawnEntity(fireball);
            }
            ((SpecialAttacksInterface)mob).setSpecialCooldown(--fireballCoolown);
            mob.getMoveControl().strafeTo(0.1f, 0f);
        }
        else ((SpecialAttacksInterface)mob).setSpecialCooldown(20);
    }
}

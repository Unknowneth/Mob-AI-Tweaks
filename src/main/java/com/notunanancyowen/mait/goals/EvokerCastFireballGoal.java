package com.notunanancyowen.mait.goals;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.SpellcastingIllagerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.EnumSet;

public class EvokerCastFireballGoal extends Goal {
    private final SpellcastingIllagerEntity mob;
    public EvokerCastFireballGoal(SpellcastingIllagerEntity mob) {
        this.mob = mob;
        setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    @Override public boolean canStart() {
        return mob.getTarget() != null && !mob.isSpellcasting() && ((SpecialAttacksInterface)mob).getSpecialCooldown() <= 0;
    }
    @Override public void start() {
        if(!mob.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING) && mob.getHealth() < mob.getMaxHealth() * MobAITweaks.getModConfigValue("evoker_totem_clutch_chance", 20) * 0.01) mob.setStackInHand(Hand.MAIN_HAND, Items.TOTEM_OF_UNDYING.getDefaultStack());
    }
    @Override public void stop() {
        if(mob instanceof SpecialAttacksInterface special) special.setSpecialCooldown(MobAITweaks.getModConfigValue("evokers_cast_fireball_cooldown", 20));
        if(mob.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) mob.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
    }
    @Override public boolean shouldRunEveryTick() {
        return true;
    }
    @Override public void tick() {
        if(mob instanceof SpecialAttacksInterface special) if(mob.getTarget() != null) {
            boolean holdingTotem = mob.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING);
            mob.getNavigation().stop();
            mob.getLookControl().lookAt(mob.getTarget());
            mob.lookAtEntity(mob.getTarget(), 60f, 60f);
            int fireballCoolown = special.getSpecialCooldown();
            boolean crazyMobs = mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP);
            int fireballsNeeded = MobAITweaks.getModConfigValue("evoker_cast_fireball_count", 3);
            if(crazyMobs) fireballsNeeded *= 5;
            if(mob.isSpellcasting() || fireballCoolown < -20) fireballCoolown = 21;
            else if(!holdingTotem && fireballCoolown == -10) if(mob.getPassengerList().size() < fireballsNeeded) for(int i = 0; i < (crazyMobs ? 5 : 1); i++) {
                SmallFireballEntity fireball = new SmallFireballEntity(mob.getWorld(), mob, mob.getRotationVector());
                if(i > 0) fireball.setVelocity(fireball.getVelocity().getX(), fireball.getVelocity().getY(),fireball.getVelocity().getZ(), i * 0.3F + 1, i * 3 + 1);
                fireball.setPosition(mob.getEyePos().add(mob.getRotationVector()).subtract(0, 0.2d, 0));
                mob.getWorld().spawnEntity(fireball);
                fireball.startRiding(mob, true);
            }
            else mob.getPassengerList().forEach(e -> {
                if(e instanceof SmallFireballEntity s) {
                    s.stopRiding();
                    boolean riding = mob.hasVehicle() && mob.getVehicle() != null;
                    s.setVelocity(s.getPos().subtract((riding ? mob.getVehicle() : mob).getX(), (riding ? mob.getVehicle() : mob).getBodyY(0.5), (riding ? mob.getVehicle() : mob).getZ()).normalize().multiply(0.1));
                    s.velocityDirty = true;
                    s.velocityModified = true;
                }
            });
            special.setSpecialCooldown(--fireballCoolown);
            mob.getMoveControl().strafeTo(holdingTotem ? -0.2F : 0.1F, 0F);
        }
        else special.setSpecialCooldown(MobAITweaks.getModConfigValue("evokers_cast_fireball_cooldown", 20));
    }
}

package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.ChickenEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieAttackGoal.class)
public abstract class ZombieAttackGoalMixin {
    @Shadow @Final private ZombieEntity zombie;
    @Shadow private int ticks;
    @Inject(method = "tick", at = @At("TAIL"))
    private void chickenJockeyPounce(CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("chicken_jockey_special_attacks") || zombie.getTarget() == null) return;
        int specialAttackTime = MobAITweaks.getModConfigValue("chicken_jockey_special_attack_cooldown", 100);
        boolean crazyMobs = zombie.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP);
        if(crazyMobs) specialAttackTime /= 2;
        if(zombie.getVehicle() instanceof ChickenEntity chicken && chicken.hasJockey) if(ticks > specialAttackTime + 5 && zombie.getTarget().distanceTo(zombie) < 6) {
            chicken.setSprinting(false);
            chicken.getNavigation().stop();
            chicken.getJumpControl().setActive();
            chicken.getMoveControl().strafeTo(1F, 0F);
            var targetMovement = zombie.getTarget().getVelocity();
            if(crazyMobs) targetMovement = targetMovement.negate();
            chicken.addVelocity(zombie.getTarget().getPos().subtract(targetMovement).subtract(chicken.getPos()).multiply(1, 0, 1).normalize());
            chicken.lookAtEntity(zombie.getTarget(), 60F, 60F);
            ticks = 5;
        }
        else {
            if(chicken.isOnGround() && chicken.getNavigation().isFollowingPath() && !chicken.isSprinting()) chicken.setSprinting(true);
            if(crazyMobs) chicken.getNavigation().setSpeed(1.2);
            chicken.getLookControl().lookAt(zombie.getTarget(), 30F, 30F);
        }
    }
    @Inject(method = "stop", at = @At("TAIL"))
    private void chickenStopSprinting(CallbackInfo ci) {
        if(zombie.getVehicle() instanceof ChickenEntity chicken && chicken.hasJockey) chicken.setSprinting(false);
    }
}

package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(targets = "net.minecraft.entity.mob.EvokerEntity$SummonVexGoal")
public abstract class EvokerSummonVexGoalMixin {
    @Unique private final ArrayList<VexEntity> vexes = new ArrayList<>();
    @Inject(method = "canStart", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/EvokerEntity;getWorld()Lnet/minecraft/world/World;"), cancellable = true)
    private void doNotUseWhenThereAreVexes(CallbackInfoReturnable<Boolean> cir) {
        if(!vexes.isEmpty()) {
            if(vexes.removeIf(VexEntity::isDead)) MobAITweaks.LOGGER.info("Vex removed!");
            if(MobAITweaks.getModConfigValue("vex_rework")) cir.setReturnValue(false);
        }
    }
    @ModifyConstant(method = "startTimeDelay", constant = @Constant(ordinal = 0))
    private int changeVexCooldown(int constant) {
        return MobAITweaks.getModConfigValue("evoker_summon_vex_cooldown", constant);
    }
    @ModifyConstant(method = "castSpell", constant = @Constant(intValue = 3, ordinal = 0))
    private int changeVexCount(int constant, @Local(index = 1) ServerWorld serverWorld) {
        switch(serverWorld.getDifficulty().getId()) {
            case 1 -> constant = MobAITweaks.getModConfigValue("evoker_summon_vex_count_easy", 2);
            case 2 -> constant = MobAITweaks.getModConfigValue("evoker_summon_vex_count_normal", 3);
            case 3 -> constant = MobAITweaks.getModConfigValue("evoker_summon_vex_count_hard", 4);
        }
        return constant;
    }
    @Inject(method = "castSpell", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntityAndPassengers(Lnet/minecraft/entity/Entity;)V", shift = At.Shift.AFTER))
    private void saveVex(CallbackInfo ci, @Local(index = 1) ServerWorld serverWorld, @Local(index = 5) VexEntity vexEntity) {
        if(vexes.add(vexEntity)) serverWorld.spawnParticles(ParticleTypes.FLASH, vexEntity.getX(), vexEntity.getBodyY(0.5), vexEntity.getZ(), 1, 0, 0, 0, 0);
    }
}

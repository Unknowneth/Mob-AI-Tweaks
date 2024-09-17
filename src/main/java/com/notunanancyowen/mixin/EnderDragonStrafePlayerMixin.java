package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.EnderDragonAttacksInterface;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.AbstractPhase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.boss.dragon.phase.StrafePlayerPhase;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StrafePlayerPhase.class)
public abstract class EnderDragonStrafePlayerMixin extends AbstractPhase {
    @Shadow private int seenTargetTimes;
    @Shadow private @Nullable LivingEntity target;
    @Unique private int fireballsDelay = 0;
    EnderDragonStrafePlayerMixin(EnderDragonEntity dragon) {
        super(dragon);
    }
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void changeBehavior(CallbackInfo ci) {
        if(dragon == null || target == null) {
            if(dragon != null) dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
            ci.cancel();
            return;
        }
        if (!MobAITweaks.getModConfigValue("ender_dragon_rework")) return;
        int fireballCooldown = (int)(dragon.getHealth() / dragon.getMaxHealth() * 15f) + 5;
        if(dragon instanceof EnderDragonAttacksInterface attacks && dragon.canSee(target) && seenTargetTimes >= 5 && target != null) if(attacks.isEnraged() || ++fireballsDelay > fireballCooldown) {
            Vec3d vec3d3 = dragon.getRotationVec(1.0F);
            double l = dragon.head.getX() - vec3d3.x;
            double m = dragon.head.getBodyY(0.5) + 0.5;
            double n = dragon.head.getZ() - vec3d3.z;
            double o = target.getX() - l;
            double p = target.getBodyY(0.5) - m;
            double q = target.getZ() - n;
            Vec3d vec3d4 = new Vec3d(o, p, q);
            if(!dragon.isSilent()) dragon.getWorld().syncWorldEvent(null, WorldEvents.ENDER_DRAGON_SHOOTS, dragon.getBlockPos(), 0);
            vec3d4 = vec3d4.normalize();
            DragonFireballEntity fireball = new DragonFireballEntity(dragon.getWorld(), dragon, vec3d4.x, vec3d4.y, vec3d4.z);
            fireball.refreshPositionAndAngles(l, m, n, 0.0F, 0.0F);
            dragon.getWorld().spawnEntity(fireball);
            seenTargetTimes = 0;
            fireballsDelay = 0;
        }
    }
}

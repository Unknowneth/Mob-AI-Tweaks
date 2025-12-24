package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HostileEntity.class)
public abstract class HostileEntityMixin extends PathAwareEntity {
    protected HostileEntityMixin(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void getOut(CallbackInfo ci) {
        if(getWorld() instanceof ServerWorld serverWorld && isOnFire() && !isFireImmune() && MobAITweaks.getModConfigValue("burn_and_freeze_visual_effects")) serverWorld.spawnParticles(getRandom().nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, getX(), getRandomBodyY(), getZ(), 1, 0.1, 0.1, 0.1, 0.1);
        if(hurtTime == 1 && MobAITweaks.getModConfigValue("hostile_mobs_can_escape_boats") && getTarget() != null && MobAITweaks.canBeDismounted(getVehicle())) if(lastDamageTaken > 0 && getRandom().nextFloat() * getMaxHealth() < lastDamageTaken) stopRiding();
        else if(getWorld() instanceof ServerWorld serverWorld) serverWorld.spawnParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getEyeY(), getZ(), (int)lastDamageTaken, 1, 1, 1, 1);
    }
}

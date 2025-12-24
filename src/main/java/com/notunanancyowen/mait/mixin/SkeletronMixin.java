package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets = "org.confluence.terraentity.entity.boss.Skeletron")
public abstract class SkeletronMixin extends HostileEntity {
    protected SkeletronMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }
    @SuppressWarnings("all")
    public void addSkills() {
        goalSelector.add(0, new Goal() {
            private int timer;
            private int cooldown = 60;
            private Vec3d tpPos;
            @Override public boolean canStart() {
                if(cooldown > 0) {
                    cooldown--;
                    return false;
                }
                return getTarget() != null && !getDataTracker().get(DATA_SPINNING) && getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) && ((phase < 200 && getRandom().nextInt(4) == 0) || timer > 0) && getHealth() < getMaxHealth() * 0.75;
            }
            @Override public void start() {
                timer = 1;
                if(getTarget() != null) tpPos = getTarget().getPos().add(getRandom().nextBetween(-16, 16), 16, getRandom().nextBetween(-16, 16));
            }
            @Override public void stop() {
                timer = 0;
            }
            @Override public void tick() {
                if(timer >= 60 && getTarget() != null) {
                    if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getParticleX(1), getRandomBodyY(), getParticleZ(1), Math.max(1, timer / 6), 0.2, 0.2, 0.2, 0.1);
                    cooldown = 60;
                    double d = getTarget().getX() - tpPos.getX();
                    double e = getTarget().getY() - tpPos.getY();
                    double f = getTarget().getZ() - tpPos.getZ();
                    float yaw = (float)(Math.atan2(f, d) * 180.0F / (float)Math.PI) - 90.0F;
                    float pitch = (float)(-(MathHelper.atan2(e, Math.sqrt(d * d + f * f)) * 180.0F / (float)Math.PI));
                    refreshPositionAndAngles(tpPos, yaw, pitch);
                    for(int i = -2; i <= 2; i++) {
                        WitherSkullEntity p = new WitherSkullEntity(getWorld(), SkeletronMixin.this, getRotationVector(pitch, yaw + i * 40));
                        p.setPosition(tpPos);
                        getWorld().spawnEntity(p);
                    }
                }
                else if(getWorld() instanceof ServerWorld server) {
                    server.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getParticleX(1), getRandomBodyY(), getParticleZ(1), 8, 0.4, 0.4, 0.4, 0.2);
                    server.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, tpPos.getX() + getWidth() * (getRandom().nextDouble() * 2 - 1), tpPos.getY() + getHeight() * getRandom().nextDouble(), tpPos.getZ() + getWidth() * (getRandom().nextDouble() * 2 - 1), 8, 0.4, 0.4, 0.4, 0.2);
                }
                timer++;
            }
        });
    }
    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Phase", phase);
        nbt.putBoolean("IsEnraged", enraged);
        nbt.putBoolean("Spinning", getDataTracker().get(DATA_SPINNING));
    }
    @Shadow public int phase;
    @Shadow public boolean enraged;
    @Shadow @Final public static TrackedData<Boolean> DATA_SPINNING;
}

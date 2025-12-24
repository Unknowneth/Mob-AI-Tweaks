package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "org.confluence.terraentity.entity.boss.EyeOfCthulhu")
public abstract class EyeOfCthulhuMixin extends HostileEntity {
    protected EyeOfCthulhuMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }
    @Override protected void initGoals() {
        super.initGoals();
        goalSelector.add(0, new Goal() {
            private int dashTime;
            private int dashCount;
            private int invisibleTime;
            @Override public boolean canStart() {
                return getTarget() != null;
            }
            @Override public void start() {
                dashTime = 0;
                dashCount = stage2_dashCount;
                invisibleTime = 0;
            }
            @Override public void stop() {
                dashTime = 0;
                dashCount = stage2_dashCount;
                if(invisibleTime > 0) setInvisible(false);
                invisibleTime = 0;
            }
            @Override public boolean shouldRunEveryTick() {
                return true;
            }
            @Override public void tick() {
                if(invisibleTime > 0) if(--invisibleTime == 0) {
                    if(getWorld() instanceof ServerWorld server) {
                        server.spawnParticles(new DustColorTransitionParticleEffect(new Vector3f(1F, 0F, 0F), new Vector3f(0.9F, 0.1F, 0.1F).div(255F / 2F), 2F), getX(), getEyeY(), getZ(), 9, 0.3, 0.3, 0.3, 0.3);
                        server.spawnParticles(ParticleTypes.FLASH, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
                    }
                    setInvisible(false);
                }
                boolean enraged = getHealth() / getMaxHealth() < 0.3F;
                if(getTarget() != null && stage == 2 && enraged && getWorld() instanceof ServerWorld server) {
                    if(stage2_dashCount < dashCount && stage2_dashCount == -2 && getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) {
                        server.spawnParticles(new DustColorTransitionParticleEffect(new Vector3f(1F, 0F, 0F), new Vector3f(0.9F, 0.1F, 0.1F).div(255F / 2F), 2F), getX(), getEyeY(), getZ(), 9, 0.3, 0.3, 0.3, 0.3);
                        server.spawnParticles(ParticleTypes.FLASH, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
                        Vec3d tpPos = getTarget().getPos().add(getRandom().nextBetween(-32, 32), getRandom().nextBetween(8, 12), getRandom().nextBetween(-32, 32));
                        double d = getTarget().getX() - tpPos.getX();
                        double e = getTarget().getY() - tpPos.getY();
                        double f = getTarget().getZ() - tpPos.getZ();
                        refreshPositionAndAngles(tpPos, (float)(Math.atan2(f, d) * 180.0F / (float)Math.PI) - 90.0F, (float)(-(MathHelper.atan2(e, Math.sqrt(d * d + f * f)) * 180.0F / (float)Math.PI)));
                        setInvisible(true);
                        invisibleTime = 20;
                        server.spawnParticles(new DustColorTransitionParticleEffect(new Vector3f(1F, 0F, 0F), new Vector3f(0.9F, 0.1F, 0.1F).div(255F / 2F), 2F), tpPos.getX(), tpPos.getY() + getStandingEyeHeight(), tpPos.getZ(), 9, 0.3, 0.3, 0.3, 0.3);
                        server.spawnParticles(ParticleTypes.FLASH, tpPos.getX(), tpPos.getY() + getStandingEyeHeight(), tpPos.getZ(), 1, 0, 0, 0, 0);
                    }
                    dashCount = stage2_dashCount;
                }
                if(dashPos != null && dashDir != null && getLookControl().getLookX() == dashPos.getX() && getLookControl().getLookY() == dashPos.getY() && getLookControl().getLookZ() == dashPos.getZ()) {
                    if(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) if((stage == 1 && dashTime % 6 == 0) || (enraged && stage == 2 && dashTime % 5 == 0 && dashTime > 0)) {
                        EntityType.get("terra_entity:demon_scythe_proj").ifPresent(e -> {
                            if(e.create(getWorld()) instanceof ProjectileEntity p) {
                                p.setPosition(getPos());
                                p.setOwner(EyeOfCthulhuMixin.this);
                                p.setVelocity(EyeOfCthulhuMixin.this, getPitch(), getYaw(), 0F, 0.2F, 0F);
                                var a = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                                if(a != null) try {
                                    var b = e.getClass().getDeclaredMethod("setDamage", float.class);
                                    b.invoke(e, (float)a.getValue());
                                }
                                catch (Throwable ignore) {
                                }
                                getWorld().spawnEntity(p);
                            }
                        });
                    }
                    else if(stage == 2 && dashTime == 0) for(int i = 0; i < 8; i++) for(int j = 0; j < 8; j++) {
                        int finalI = i;
                        int finalJ = j;
                        EntityType.get("terra_entity:demon_scythe_proj").ifPresent(e -> {
                            if(e.create(getWorld()) instanceof ProjectileEntity p) {
                                p.setPosition(getPos());
                                p.setOwner(EyeOfCthulhuMixin.this);
                                p.setVelocity(EyeOfCthulhuMixin.this, finalI / 8F * 360F, finalJ / 8F * 360F, 0F, 0.2F, 0F);
                                var a = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                                if(a != null) try {
                                    var b = e.getClass().getDeclaredMethod("setDamage", float.class);
                                    b.invoke(e, (float)a.getValue());
                                }
                                catch (Throwable ignore) {
                                }
                                getWorld().spawnEntity(p);
                            }
                        });
                    }
                    dashTime++;
                    dashPos = dashPos.add(dashDir.normalize().multiply(getVelocity().length()));
                }
                else dashTime = 0;
            }
        });
    }
    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Stage", stage);
        nbt.putInt("DashCount_Phase1", stage1_dashCount);
        nbt.putInt("DashCount_Phase2", stage2_dashCount);
        nbt.putInt("DashCount_Phase2_Max", stage2_dashCount_max);
        nbt.putInt("SummonCooldown", summonCD);
        nbt.putInt("SummonCooldownAll", summonCDAll);
    }
    @Shadow public int stage;
    @Shadow private Vec3d dashDir;
    @Shadow private Vec3d dashPos;
    @Shadow private int stage1_dashCount;
    @Shadow private int stage2_dashCount;
    @Shadow private int stage2_dashCount_max;
    @Shadow private int summonCDAll;
    @Shadow private int summonCD;
}

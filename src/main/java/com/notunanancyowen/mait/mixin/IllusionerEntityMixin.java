package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.IllusionerEntity;
import net.minecraft.entity.mob.SpellcastingIllagerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IllusionerEntity.class)
public abstract class IllusionerEntityMixin extends SpellcastingIllagerEntity implements RangedAttackMob {
    @Shadow @Final private Vec3d[][] mirrorCopyOffsets;
    @Shadow private int mirrorSpellTimer;
    @Unique private static final TrackedData<Integer> CLONES = DataTracker.registerData(IllusionerEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private static final TrackedData<Vector3f> CLONES1 = DataTracker.registerData(IllusionerEntityMixin.class, TrackedDataHandlerRegistry.VECTOR3F);
    @Unique private static final TrackedData<Vector3f> CLONES2 = DataTracker.registerData(IllusionerEntityMixin.class, TrackedDataHandlerRegistry.VECTOR3F);
    @Unique private static final TrackedData<Vector3f> CLONES3 = DataTracker.registerData(IllusionerEntityMixin.class, TrackedDataHandlerRegistry.VECTOR3F);
    @Unique private static final TrackedData<Vector3f> CLONES4 = DataTracker.registerData(IllusionerEntityMixin.class, TrackedDataHandlerRegistry.VECTOR3F);
    @Unique private static final TrackedData<Vector3f> CLONES5 = DataTracker.registerData(IllusionerEntityMixin.class, TrackedDataHandlerRegistry.VECTOR3F);
    @Unique private static final TrackedData<Vector3f> CLONES6 = DataTracker.registerData(IllusionerEntityMixin.class, TrackedDataHandlerRegistry.VECTOR3F);
    @Unique private static final TrackedData<Vector3f> CLONES7 = DataTracker.registerData(IllusionerEntityMixin.class, TrackedDataHandlerRegistry.VECTOR3F);
    @Unique private static final TrackedData<Vector3f> CLONES8 = DataTracker.registerData(IllusionerEntityMixin.class, TrackedDataHandlerRegistry.VECTOR3F);
    protected IllusionerEntityMixin(EntityType<? extends SpellcastingIllagerEntity> entityType, World world) {
        super(entityType, world);
    }
    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(CLONES, 0);
        builder.add(CLONES1, new Vector3f(0, 0, 0));
        builder.add(CLONES2, new Vector3f(0, 0, 0));
        builder.add(CLONES3, new Vector3f(0, 0, 0));
        builder.add(CLONES4, new Vector3f(0, 0, 0));
        builder.add(CLONES5, new Vector3f(0, 0, 0));
        builder.add(CLONES6, new Vector3f(0, 0, 0));
        builder.add(CLONES7, new Vector3f(0, 0, 0));
        builder.add(CLONES8, new Vector3f(0, 0, 0));
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void replaceBowAttackGoalWithSomethingSimpler(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("illusioner_rework") && goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof BowAttackGoal)) goalSelector.add(6, new ProjectileAttackGoal(this, 0, 5, 20F) {
            @Override public void stop() {
                super.stop();
                for (int k = 0; k < 4; k++) {
                    mirrorCopyOffsets[0][k] = mirrorCopyOffsets[1][k];
                    mirrorCopyOffsets[1][k] = new Vec3d(0.0, 0.0, 0.0);
                }
                stopUsingItem();
                setAttacking(false);
            }
            @Override public void tick() {
                if(isUsingItem()) turnHead(bodyYaw, headYaw);
                super.tick();
            }
            @Override public boolean canStart() {
                return super.canStart() && ((getMainHandStack() != null && getMainHandStack().getItem() instanceof BowItem) || (getOffHandStack() != null && getOffHandStack().getItem() instanceof BowItem));
            }
        });
    }
    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/IllusionerEntity;getWorld()Lnet/minecraft/world/World;", ordinal = 0), cancellable = true)
    private void syncToServer(CallbackInfo ci) {
        if(this.isInvisible() && deathTime < 5) if(!this.getWorld().isClient) {
            this.mirrorSpellTimer--;
            if (this.mirrorSpellTimer < 0) this.mirrorSpellTimer = 0;
            if (this.hurtTime == 1 || this.age % 1200 == 0) {
                for (int j = 0; j < 4; j++) {
                    this.mirrorCopyOffsets[0][j] = this.mirrorCopyOffsets[1][j];
                    this.mirrorCopyOffsets[1][j] = new Vec3d((-6.0F + this.random.nextInt(13)) * 0.5, Math.max(0, this.random.nextInt(6) - 4), (-6.0F + this.random.nextInt(13)) * 0.5);
                }
                boolean successfullySwitchedWithClone = false;
                for (Vec3d v : mirrorCopyOffsets[1]) {
                    for(int i = 0; i < getSafeFallDistance(); i++) {
                        BlockPos blockPos = new BlockPos((int)(v.getX() + getX()), (int)(v.getY() + getY()) - i, (int)(v.getZ() + getZ()));
                        if(getWorld().getBlockState(blockPos).canPathfindThrough(NavigationType.LAND)) {
                            refreshPositionAndAngles(blockPos.up(), getYaw(), getPitch());
                            successfullySwitchedWithClone = true;
                            break;
                        }
                    }
                    if(successfullySwitchedWithClone) break;
                }
                this.mirrorSpellTimer = 3;
            }
            else if (this.hurtTime == this.maxHurtTime - 1) {
                for (int k = 0; k < 4; k++) {
                    this.mirrorCopyOffsets[0][k] = this.mirrorCopyOffsets[1][k];
                    this.mirrorCopyOffsets[1][k] = new Vec3d(0.0, 0.0, 0.0);
                }
                this.mirrorSpellTimer = 3;
            }
            getDataTracker().set(CLONES, mirrorSpellTimer);
            getDataTracker().set(CLONES1, mirrorCopyOffsets[0][0].toVector3f());
            getDataTracker().set(CLONES2, mirrorCopyOffsets[0][1].toVector3f());
            getDataTracker().set(CLONES3, mirrorCopyOffsets[0][2].toVector3f());
            getDataTracker().set(CLONES4, mirrorCopyOffsets[0][3].toVector3f());
            getDataTracker().set(CLONES5, mirrorCopyOffsets[1][0].toVector3f());
            getDataTracker().set(CLONES6, mirrorCopyOffsets[1][1].toVector3f());
            getDataTracker().set(CLONES7, mirrorCopyOffsets[1][2].toVector3f());
            getDataTracker().set(CLONES8, mirrorCopyOffsets[1][3].toVector3f());
        }
        else {
            if(hurtTime == 1 || age % 1200 == 0) {
                for(int j = 0; j < 16; j++) this.getWorld().addParticle(ParticleTypes.CLOUD, this.getParticleX(0.5), this.getRandomBodyY(), this.offsetZ(0.5), 0.0, 0.0, 0.0);
                this.getWorld().playSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, this.getSoundCategory(), 1.0F, 1.0F, false);
            }
            mirrorSpellTimer = getDataTracker().get(CLONES);
            mirrorCopyOffsets[0][0] = new Vec3d(getDataTracker().get(CLONES1));
            mirrorCopyOffsets[0][1] = new Vec3d(getDataTracker().get(CLONES2));
            mirrorCopyOffsets[0][2] = new Vec3d(getDataTracker().get(CLONES3));
            mirrorCopyOffsets[0][3] = new Vec3d(getDataTracker().get(CLONES4));
            mirrorCopyOffsets[1][0] = new Vec3d(getDataTracker().get(CLONES5));
            mirrorCopyOffsets[1][1] = new Vec3d(getDataTracker().get(CLONES6));
            mirrorCopyOffsets[1][2] = new Vec3d(getDataTracker().get(CLONES7));
            mirrorCopyOffsets[1][3] = new Vec3d(getDataTracker().get(CLONES8));
        }
        ci.cancel();
    }
    @Override public boolean isInvulnerableTo(DamageSource damageSource) {
        if(MobAITweaks.getModConfigValue("illusioner_rework")) if(damageSource.getSource() != null && damageSource.getSource().getId() == getId()) return false;
        else if(damageSource.getAttacker() != null && damageSource.getAttacker().getId() == getId()) return false;
        return super.isInvulnerableTo(damageSource);
    }
    @Inject(method = "shootAt", at = @At("HEAD"), cancellable = true)
    private void arrowsOriginateFromClones(LivingEntity target, float pullProgress, CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("illusioner_rework")) return;
        boolean hasClones = false;
        for(Vec3d v : mirrorCopyOffsets[1]) if(v != Vec3d.ZERO) {
            hasClones = true;
            break;
        }
        if(!hasClones) return;
        try {
            ItemStack itemStack = getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, (getMainHandStack().getItem() instanceof BowItem bowItem ? bowItem : getOffHandStack().getItem() instanceof BowItem bowItem ? bowItem : null)));
            if(!itemStack.isEmpty()) itemStack.damage(1, this, getPreferredEquipmentSlot(itemStack));
            ItemStack itemStack2 = getProjectileType(itemStack);
            for(Vec3d v : mirrorCopyOffsets[1]) try {
                PersistentProjectileEntity persistentProjectileEntity = ProjectileUtil.createArrowProjectile(this, itemStack2, pullProgress, itemStack);
                double d = target.getX();
                double e = target.getBodyY(0.3333333333333333) - persistentProjectileEntity.getY();
                double f = target.getZ();
                persistentProjectileEntity.setPosition(persistentProjectileEntity.getPos().add(v));
                if(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) {
                    d -= getX() + v.getX();
                    //recalculate basically
                    e = target.getBodyY(0.3333333333333333) - persistentProjectileEntity.getY();
                    f -= getZ() + v.getY();
                }
                else {
                    d -= getX();
                    f -= getZ() ;
                }
                double g = Math.sqrt(d * d + f * f);
                persistentProjectileEntity.setVelocity(d, e + g * 0.2F, f, 1.6F, 0);
                playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
                getWorld().spawnEntity(persistentProjectileEntity);
            }
            catch (Throwable ignored) {
                break;
            }
            if(!itemStack2.isEmpty()) itemStack2.decrement(1);
        }
        catch (Throwable ignored) {
            return;
        }
        ci.cancel();
    }
}

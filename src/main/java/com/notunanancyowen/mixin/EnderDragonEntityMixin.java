package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.EnderDragonAttacksInterface;
import com.notunanancyowen.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.boss.dragon.phase.PhaseManager;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import java.util.*;

@Mixin(EnderDragonEntity.class)
public abstract class EnderDragonEntityMixin extends MobEntity implements Monster, EnderDragonAttacksInterface {
    @Shadow public abstract PhaseManager getPhaseManager();
    @Shadow public abstract boolean damagePart(EnderDragonPart part, DamageSource source, float amount);
    @Shadow protected abstract boolean parentDamage(DamageSource source, float amount);
    @Shadow @Final private EnderDragonPart body;
    @Shadow @Final public EnderDragonPart head;
    @Shadow public boolean slowedDownByBlock;
    @Shadow @Nullable public EndCrystalEntity connectedCrystal;
    @Unique private int crystalShieldTime = 0;
    @SuppressWarnings("all")
    @Unique private List<Vec3d> savedCrystalsPos = new ArrayList<>();
    @Unique private static final TrackedData<Boolean> ENRAGED = DataTracker.registerData(EnderDragonEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private static final TrackedData<Integer> TIL_DPS = DataTracker.registerData(EnderDragonEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    EnderDragonEntityMixin(EntityType<? extends EnderDragonEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "launchLivingEntities", at = @At("TAIL"))
    private void yeet(List<Entity> entities, CallbackInfo ci) {
        if(isEnraged()) for(Entity entity : entities) if(!(entity instanceof DragonFireballEntity)) entity.setFireTicks(240);
    }
    @Inject(method = "damageLivingEntities", at = @At("TAIL"))
    private void burn(List<Entity> entities, CallbackInfo ci) {
        if(isEnraged()) for(Entity entity : entities) if(!(entity instanceof DragonFireballEntity)) entity.setFireTicks(240);
    }
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void trackData(CallbackInfo ci) {
        getDataTracker().startTracking(TIL_DPS, 3);
        getDataTracker().startTracking(ENRAGED, false);
    }
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void putEnrageToNBT(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("IsEnraged", isEnraged());
        if(MobAITweaks.getModConfigValue("ender_dragon_rework")) nbt.putInt("UntilDamagePhase", getDataTracker().get(TIL_DPS));
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void getEnrageFromNBT(NbtCompound nbt, CallbackInfo ci) {
        getDataTracker().set(ENRAGED, nbt.getBoolean("IsEnraged"));
        if(MobAITweaks.getModConfigValue("ender_dragon_rework")) getDataTracker().set(TIL_DPS, nbt.getInt("UntilDamagePhase"));
    }
    @Inject(method = "tickWithEndCrystals", at = @At("HEAD"), cancellable = true)
    private void endCrystal(CallbackInfo ci) {
        if (!MobAITweaks.getModConfigValue("ender_dragon_rework")) return;
        if (isDPS()) {
            if (connectedCrystal != null) {
                if (getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.FLASH, connectedCrystal.getX(), connectedCrystal.getY(), connectedCrystal.getZ(), 1, 0d, 0d, 0d, 0d);
                connectedCrystal.setBeamTarget(null);
                connectedCrystal = null;
            }
            ci.cancel();
            return;
        }
        if (connectedCrystal == null && age % 100 == 0) {
            var crystals = getWorld().getNonSpectatingEntities(EndCrystalEntity.class, getBoundingBox().expand(64.0));
            if(!crystals.isEmpty() && crystals.size() > 1 && !getWorld().isClient) connectedCrystal = crystals.get(getRandom().nextInt(crystals.size()));
        }
        if (connectedCrystal != null) if (connectedCrystal.isRemoved() || crystalShieldTime > 200) {
            if(connectedCrystal.isAlive()) {
                heal(getMaxHealth() * 0.1F);
                connectedCrystal.setBeamTarget(null);
            }
            else if (getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.FLASH, connectedCrystal.getX(), connectedCrystal.getY(), connectedCrystal.getZ(), 1, 0d, 0d, 0d, 0d);
            connectedCrystal = null;
            crystalShieldTime = 0;
        }
        else {
            connectedCrystal.setBeamTarget(connectedCrystal.getBlockPos().down(2));
            if (age % (int)(21F - crystalShieldTime * 0.1F) == 0 && getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.FLASH, connectedCrystal.getX(), connectedCrystal.getY(), connectedCrystal.getZ(), 1, 0d, 0d, 0d, 0d);
            crystalShieldTime++;
        }
        ci.cancel();
    }
    @Inject(method = "damagePart", at = @At("HEAD"), cancellable = true)
    private void damageBodyParts(EnderDragonPart part, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(source.getAttacker() == this || source.getSource() == this) cir.setReturnValue(false);
        else if(MobAITweaks.getModConfigValue("bosses_enrage") && !getPhaseManager().getCurrent().getType().equals(PhaseType.DYING) && !part.equals(body) && getHealth() > 1f && (source.isOf(DamageTypes.BAD_RESPAWN_POINT) || (!source.isOf(DamageTypes.OUT_OF_WORLD) && !source.isOf(DamageTypes.OUTSIDE_BORDER) && !source.isOf(DamageTypes.GENERIC_KILL) && !source.isOf(DamageTypes.CRAMMING) && amount >= getMaxHealth() * 0.1f)) && !isEnraged()) {
            getDataTracker().set(ENRAGED, true);
            if(getServer() != null && getServer().getPlayerManager() != null) getServer().getPlayerManager().broadcast(Text.translatable("mob-ai-tweaks.boss_enraged").copy().formatted(Formatting.RED), false);
        }
        if(MobAITweaks.getModConfigValue("ender_dragon_rework") && isDPS() && getPhaseManager().getCurrent() != PhaseType.DYING) cir.setReturnValue(parentDamage(source, amount * (part.equals(body) ? 0.2F : part.equals(head) ? 0.8F : 0.5F)));
    }
    @SuppressWarnings("all")
    @Override public boolean damage(DamageSource source, float amount) {
        if(source.getAttacker() == this || source.getSource() == this) return false;
        if(MobAITweaks.getModConfigValue("bosses_enrage") && !getPhaseManager().getCurrent().getType().equals(PhaseType.DYING) && getHealth() > 1f && (source.isOf(DamageTypes.BAD_RESPAWN_POINT) || (!source.isOf(DamageTypes.OUT_OF_WORLD) && !source.isOf(DamageTypes.OUTSIDE_BORDER) && !source.isOf(DamageTypes.GENERIC_KILL) && !source.isOf(DamageTypes.CRAMMING) && amount >= getMaxHealth() * 0.2f)) && !isEnraged()) {
            getDataTracker().set(ENRAGED, true);
            if(getServer() != null && getServer().getPlayerManager() != null) getServer().getPlayerManager().broadcast(Text.translatable("mob-ai-tweaks.boss_enraged").copy().formatted(Formatting.RED), false);
            if(amount > getMaxHealth() * 0.5f) amount = getMaxHealth() * 0.5f;
        }
        return getWorld().isClient() ? false : damagePart(body, source, amount);
    }
    @Inject(method = "crystalDestroyed", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/dragon/EnderDragonEntity;damagePart(Lnet/minecraft/entity/boss/dragon/EnderDragonPart;Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private void progressDPS(EndCrystalEntity endCrystal, BlockPos pos, DamageSource source, CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("ender_dragon_rework")) getDataTracker().set(TIL_DPS, getDataTracker().get(TIL_DPS) - 1);
    }
    @SuppressWarnings("all")
    @Inject(method = "crystalDestroyed", at = @At("HEAD"))
    private void saveCrystal(EndCrystalEntity endCrystal, BlockPos pos, DamageSource source, CallbackInfo ci) {
        if(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) && !getWorld().isClient()) {
            ShulkerBulletEntity bullet = new ShulkerBulletEntity(getWorld(), this, source.getSource(), Direction.Axis.pickRandomAxis(getRandom())) {
                @Override public void onEntityHit(EntityHitResult entityHitResult) {
                    entityHitResult.getEntity().addVelocity(0, -2, 0);
                    entityHitResult.getEntity().setAir(entityHitResult.getEntity().getAir() - 1);
                    if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1, 0d, 0d, 0d, 0d);
                }
                @Override public void tick() {
                    if(getOwner() == null || !getOwner().isAlive()) discard();
                    else {
                        ShulkerBulletEntity thisBulletRightHere = this;
                        if(thisBulletRightHere instanceof SpecialAttacksInterface attacks) attacks.setSpecialCooldown(2);
                        super.tick();
                        setVelocity(getVelocity().multiply(1.2d));
                        this.velocityDirty  = true;
                    }
                }
                @Override protected boolean canHit(Entity entity) {
                    return false;
                }
                @Override public boolean damage(DamageSource source, float amount) {
                    return false;
                }
                @Override public Text getName() {
                    return Text.translatable("mob-ai-tweaks.end_crystal_projectile");
                }
            };
            if(bullet instanceof SpecialAttacksInterface attacks) attacks.setSpecialCooldown(2);
            bullet.setOwner(this);
            bullet.setPosition(pos.toCenterPos());
            getWorld().spawnEntity(bullet);
        }
        if(MobAITweaks.getModConfigValue("ender_dragon_rework") && savedCrystalsPos != null) if(endCrystal != null) {
            if(endCrystal.shouldShowBottom()) savedCrystalsPos.add(endCrystal.getPos());
        }
        else savedCrystalsPos.add(pos.toCenterPos());
    }
    @Override public boolean isInvulnerableTo(DamageSource damageSource) {
        if(damageSource.getAttacker() == this || damageSource.getSource() == this) return true;
        return super.isInvulnerableTo(damageSource);
    }
    @SuppressWarnings("all")
    @Override public boolean isEnraged() {
        return getDataTracker().get(ENRAGED);
    }
    @SuppressWarnings("all")
    @Override public void forceMove() {
        setYaw(MathHelper.wrapDegrees(getYaw()));
        if (slowedDownByBlock) move(MovementType.SELF, getVelocity().multiply(0.8F));
        else move(MovementType.SELF, getVelocity());
    }
    @SuppressWarnings("all")
    @Override public boolean isDPS() {
        return getDataTracker().get(TIL_DPS) < 1;
    }
    @SuppressWarnings("all")
    @Override public void endDPS() {
        for(Vec3d pos : savedCrystalsPos) getWorld().spawnEntity(new EndCrystalEntity(getWorld(), pos.x, pos.y, pos.z));
        savedCrystalsPos.clear();
        getDataTracker().set(TIL_DPS, getWorld().getDifficulty().getId() + 1);
    }
}

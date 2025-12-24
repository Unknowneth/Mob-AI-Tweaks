package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.EnderDragonAttacksInterface;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
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
    @Unique private static final TrackedData<Boolean> ENRAGED = DataTracker.registerData(EnderDragonEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private static final TrackedData<Integer> TIL_DPS = DataTracker.registerData(EnderDragonEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private static final TrackedData<Integer> CRYSTAL = DataTracker.registerData(EnderDragonEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private static final TrackedData<String> CRYSTALS = DataTracker.registerData(EnderDragonEntityMixin.class, TrackedDataHandlerRegistry.STRING);
    EnderDragonEntityMixin(EntityType<? extends EnderDragonEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "launchLivingEntities", at = @At("TAIL"))
    private void yeet(ServerWorld world, List<Entity> entities, CallbackInfo ci) {
        if(isEnraged()) for(Entity entity : entities) if(!(entity instanceof DragonFireballEntity) && !(entity instanceof ItemEntity)) entity.setOnFireFor(12f);
    }
    @Inject(method = "damageLivingEntities", at = @At("TAIL"))
    private void burn(List<Entity> entities, CallbackInfo ci) {
        if(isEnraged()) for(Entity entity : entities) if(!(entity instanceof DragonFireballEntity) && !(entity instanceof ItemEntity)) entity.setOnFireFor(12f);
    }
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void trackData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(TIL_DPS, 3);
        builder.add(ENRAGED, false);
        builder.add(CRYSTAL, -1);
        builder.add(CRYSTALS, "");
    }
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void putEnrageToNBT(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("IsEnraged", isEnraged());
        if(!MobAITweaks.getModConfigValue("ender_dragon_healing_rework")) return;
        nbt.putInt("UntilDamagePhase", getDataTracker().get(TIL_DPS));
        nbt.putString("CrystalsDestroyedPos", getDataTracker().get(CRYSTALS));
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void getEnrageFromNBT(NbtCompound nbt, CallbackInfo ci) {
        getDataTracker().set(ENRAGED, nbt.getBoolean("IsEnraged"));
        if(!MobAITweaks.getModConfigValue("ender_dragon_healing_rework")) return;
        getDataTracker().set(TIL_DPS, nbt.getInt("UntilDamagePhase"));
        getDataTracker().set(CRYSTALS, nbt.getString("CrystalsDestroyedPos"));
    }
    @Inject(method = "tickWithEndCrystals", at = @At("HEAD"), cancellable = true)
    private void endCrystal(CallbackInfo ci) {
        if (!MobAITweaks.getModConfigValue("ender_dragon_healing_rework")) return;
        if (isDPS()) {
            if (connectedCrystal != null) {
                if (getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.FLASH, connectedCrystal.getX(), connectedCrystal.getY(), connectedCrystal.getZ(), 1, 0d, 0d, 0d, 0d);
                connectedCrystal.setBeamTarget(null);
                connectedCrystal = null;
                getDataTracker().set(CRYSTAL, -1);
            }
            ci.cancel();
            return;
        }
        if (connectedCrystal == null && age % 100 == 0 && !getWorld().isClient) {
            var crystals = getWorld().getNonSpectatingEntities(EndCrystalEntity.class, getBoundingBox().expand(64.0));
            if (!crystals.isEmpty() && crystals.size() > 1) connectedCrystal = crystals.get(getRandom().nextInt(crystals.size()));
            if (connectedCrystal != null) getDataTracker().set(CRYSTAL, connectedCrystal.getId());
        }
        if (connectedCrystal != null) if (connectedCrystal.isRemoved() || crystalShieldTime > 200) {
            if(connectedCrystal.isAlive()) {
                heal(getMaxHealth() * 0.1F);
                connectedCrystal.setBeamTarget(null);
                connectedCrystal.setGlowing(false);
            }
            else if (getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.FLASH, connectedCrystal.getX(), connectedCrystal.getY(), connectedCrystal.getZ(), 1, 0d, 0d, 0d, 0d);
            connectedCrystal = null;
            getDataTracker().set(CRYSTAL, -1);
            crystalShieldTime = 0;
        }
        else {
            connectedCrystal.setBeamTarget(connectedCrystal.getBlockPos().down(2));
            if (age % (int)(21F - crystalShieldTime * 0.1F) == 0 && getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.FLASH, connectedCrystal.getX(), connectedCrystal.getY(), connectedCrystal.getZ(), 1, 0d, 0d, 0d, 0d);
            crystalShieldTime++;
        }
        if (getWorld().isClient) {
            int i = getDataTracker().get(CRYSTAL);
            if (i >= 0) if (getWorld().getEntityById(i) instanceof EndCrystalEntity crystal) {
                connectedCrystal = crystal;
                connectedCrystal.setGlowing(true);
            }
            else connectedCrystal = null;
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
        if(MobAITweaks.getModConfigValue("ender_dragon_healing_rework") && isDPS() && getPhaseManager().getCurrent() != PhaseType.DYING) cir.setReturnValue(parentDamage(source, amount * (part.equals(body) ? 0.2F : part.equals(head) ? 0.8F : 0.5F)));
    }
    @Inject(method = "crystalDestroyed", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/dragon/EnderDragonEntity;damagePart(Lnet/minecraft/entity/boss/dragon/EnderDragonPart;Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private void progressDPS(EndCrystalEntity endCrystal, BlockPos pos, DamageSource source, CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("ender_dragon_healing_rework")) getDataTracker().set(TIL_DPS, getDataTracker().get(TIL_DPS) - 1);
    }
    @SuppressWarnings("all")
    @Inject(method = "crystalDestroyed", at = @At("HEAD"))
    private void saveCrystal(EndCrystalEntity endCrystal, BlockPos pos, DamageSource source, CallbackInfo ci) {
        if(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) && !getWorld().isClient()) {
            ShulkerBulletEntity bullet = new ShulkerBulletEntity(getWorld(), this, source.getSource(), Direction.Axis.pickRandomAxis(getRandom()));
            if(bullet instanceof SpecialAttacksInterface attacks) attacks.setSpecialCooldown(2);
            bullet.setOwner(this);
            bullet.setPosition(pos.toCenterPos());
            getWorld().spawnEntity(bullet);
        }
        if(MobAITweaks.getModConfigValue("ender_dragon_healing_rework")) if(endCrystal != null) {
            if(endCrystal.shouldShowBottom()) getDataTracker().set(CRYSTALS, getDataTracker().get(CRYSTALS) + endCrystal.getPos().toString());
        }
        else getDataTracker().set(CRYSTALS, getDataTracker().get(CRYSTALS) + pos.toCenterPos().toString());
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
        if(!getWorld().isClient()) {
            String s1 = getDataTracker().get(CRYSTALS);
            String[] s2 = s1.replace(" ", "").replace("(", "").replace(")", " ").split(" ", -1);
            for(String s3 : s2) {
                String[] s4 = s3.split(",", 3);
                if(s4.length != 3) {
                    MobAITweaks.LOGGER.info("Something went wrong trying to respawn one of the End Crystals, perhaps the NBT got messed up?");
                    continue;
                }
                EndCrystalEntity crystal = EntityType.END_CRYSTAL.create(getWorld());
                crystal.refreshPositionAndAngles(Double.parseDouble(s4[0]), Double.parseDouble(s4[1]), Double.parseDouble(s4[2]), 0F, 0F);
                getWorld().spawnEntity(crystal);
            }
        }
        getDataTracker().set(CRYSTALS, "");
        int worldDifficulty = getWorld().getDifficulty().getId() + 1;
        switch(worldDifficulty) {
            case 0 -> worldDifficulty = MobAITweaks.getModConfigValue("ender_dragon_crystal_threshold_peaceful", worldDifficulty);
            case 1 -> worldDifficulty = MobAITweaks.getModConfigValue("ender_dragon_crystal_threshold_easy", worldDifficulty);
            case 2 -> worldDifficulty = MobAITweaks.getModConfigValue("ender_dragon_crystal_threshold_normal", worldDifficulty);
            case 3 -> worldDifficulty = MobAITweaks.getModConfigValue("ender_dragon_crystal_threshold_hard", worldDifficulty);
        }
        getDataTracker().set(TIL_DPS, worldDifficulty);
    }
}

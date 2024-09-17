package com.notunanancyowen.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.dataholders.WitherAttacksInterface;
import com.notunanancyowen.goals.WitherChargeAttackGoal;
import com.notunanancyowen.goals.WitherSlamAttackGoal;
import com.notunanancyowen.goals.WitherStrafeShootGoal;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherEntity.class)
public abstract class WitherEntityMixin extends HostileEntity implements RangedAttackMob, WitherAttacksInterface {
    @Shadow protected abstract void shootSkullAt(int headIndex, double targetX, double targetY, double targetZ, boolean charged);
    @Shadow protected abstract SoundEvent getDeathSound();
    @Shadow @Final private ServerBossBar bossBar;
    @Shadow public abstract int getInvulnerableTimer();
    @Shadow @Final private int[] skullCooldowns;
    @Shadow public abstract boolean shouldRenderOverlay();
    @Shadow public abstract void setTrackedEntityId(int headIndex, int id);
    @Shadow private int blockBreakingCooldown;
    @Unique private final int[] headAttacksCounter = new int[] {0, 0, 0, 0};
    @Unique private static final TrackedData<Integer> CHARGE_TIME = DataTracker.registerData(WitherEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private static final TrackedData<Integer> SLAM_TIME = DataTracker.registerData(WitherEntityMixin.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private static final TrackedData<Boolean> ENRAGED = DataTracker.registerData(WitherEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private static final TrackedData<Boolean> BLUE = DataTracker.registerData(WitherEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private DamageSource lastDamageSource;
    @Unique private boolean isReworked = true;
    WitherEntityMixin(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "shootSkullAt(ILnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"),  cancellable = true)
    private void performRangedAttack(int i, LivingEntity livingEntity, CallbackInfo ci) {
        if(!isReworked) return;
        if(getInvulnerableTimer() > 0 || getChargeTime() > 0 || getSlamTime() > 0) {
            getDataTracker().set(BLUE, false);
            ci.cancel();
            return;
        }
        double targetPosX = livingEntity.getX();
        double targetPosY = livingEntity.getY() + livingEntity.getStandingEyeHeight() * 0.5d;
        double targetPosZ = livingEntity.getZ();
        float hpRatio = getHealth() / getMaxHealth();
        if (i == 0) {
            shootSkullAt(i, targetPosX, targetPosY, targetPosZ, true);
            ci.cancel();
        }
        else if(hpRatio < 0.6666F || headAttacksCounter[i] < 0) if(headAttacksCounter[i] <= 0 || hpRatio < 0.1666F) {
            headAttacksCounter[i] = hpRatio <= 0.3333F ? 4 : 8;
            shootSkullAt(i, targetPosX, targetPosY, targetPosZ, true);
            ci.cancel();
        }
        else headAttacksCounter[i]--;
    }
    @Inject(method = "shootSkullAt(IDDDZ)V", at = @At("HEAD"), cancellable = true)
    private void hasShotBlueSkull(int headIndex, double targetX, double targetY, double targetZ, boolean charged, CallbackInfo ci) {
        if(getInvulnerableTimer() > 0 || getChargeTime() > 0 || getSlamTime() > 0) {
            getDataTracker().set(BLUE, false);
            ci.cancel();
        }
        else getDataTracker().set(BLUE, charged);
    }
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void trackData(CallbackInfo ci) {
        getDataTracker().startTracking(CHARGE_TIME, 0);
        getDataTracker().startTracking(SLAM_TIME, 0);
        getDataTracker().startTracking(ENRAGED, false);
        getDataTracker().startTracking(BLUE, false);
    }
    @SuppressWarnings("all")
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addNewAttacks(CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("wither_rework")) return;
        goalSelector.add(0, new WitherChargeAttackGoal(this));
        goalSelector.add(0, new WitherSlamAttackGoal(this));
        goalSelector.add(1, new WitherStrafeShootGoal(this));
        goalSelector.remove(goalSelector.getGoals().stream().filter(goal -> goal.getGoal().getClass().getPackageName().startsWith("net.minecraft")).findFirst().get().getGoal());
    }
    @Inject(method = "mobTick", at = @At("TAIL"))
    private void fixBossBar(CallbackInfo ci) {
        if(isReworked && bossBar.getStyle() != BossBar.Style.NOTCHED_6) bossBar.setStyle(BossBar.Style.NOTCHED_6);
        if(isEnraged()) {
            if(getHealth() >= getMaxHealth()) {
                bossBar.setName(bossBar.getName().copy().formatted(Formatting.WHITE));
                getDataTracker().set(ENRAGED, false);
            }
            else bossBar.setName(bossBar.getName().copy().formatted(Formatting.RED));
            if(!getWorld().isClient()) for(int i = 0; i < skullCooldowns.length; i++) if(skullCooldowns[i] > 0) skullCooldowns[i] -= 4;
            else if(skullCooldowns[i] < 0) skullCooldowns[i] = 0;
        }
        else if((getChargeTime() > 0 || getSlamTime() > 0) && !getWorld().isClient()) for(int i = 0; i < skullCooldowns.length; i++) skullCooldowns[i] = getRandom().nextBetween(5, 15);
    }
    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/WitherEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V", shift = At.Shift.AFTER))
    private void flyUpToChasePlayer(CallbackInfo ci) {
        if(isReworked && !isAiDisabled() && getChargeTime() <= 0 && getSlamTime() <= 0 && getHealth() <= getMaxHealth() && getTarget() != null) {
            LivingEntity target = getTarget();
            double speedMultiplier = distanceTo(target) > 12 ? (target.isFallFlying() ? 3.2d : 1.7d) : 1.1d;
            setVelocity(getVelocity().getX() * speedMultiplier, (target.getY() > getEyeY() ? -getVelocity().y : getVelocity().y) * (Math.abs(target.getY() - getEyeY()) > 8 ? 2.8d : 1.2d), getVelocity().getZ() * speedMultiplier);
            getLookControl().lookAt(target);
            lookAtEntity(target, 30f, 30f);
        }
    }
    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/WitherEntity;shouldRenderOverlay()Z", ordinal = 0))
    private boolean halfHealth(boolean original) {
        return isReworked ? getHealth() <= getMaxHealth() * 0.5F : original;
    }
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void fixHealthBarDesync(CallbackInfo ci) {
        if(isAiDisabled()) bossBar.setPercent(getHealth() / getMaxHealth());
    }
    @ModifyReturnValue(method = "shouldRenderOverlay", at = @At("TAIL"))
    private boolean showShield(boolean original) {
        if(!isReworked) return original;
        int i = getSlamTime();
        if(i > 55) i = 0;
        if(getChargeTime() > 0) i = getChargeTime();
        return i > 0 && (i > 20 || i / 3 % 2 != 1);
    }
    @Inject(method = "onSummoned", at = @At("TAIL"))
    private void onSpawned(CallbackInfo ci) {
        if(getServer() != null && getServer().getPlayerManager() != null) getServer().getPlayerManager().broadcast(Text.translatable("mob-ai-tweaks.boss_summoned", getName().getString()).copy().formatted(Formatting.DARK_PURPLE), false);
        if(isReworked) bossBar.setStyle(BossBar.Style.NOTCHED_6);
    }
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void putNewStuffToNBT(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("blockBreakingCooldown", blockBreakingCooldown);
        nbt.putInt("ChargeAttackTime", getChargeTime());
        nbt.putInt("SlamAttackTime", getSlamTime());
        nbt.putBoolean("IsEnraged", isEnraged());
        nbt.putBoolean("IsBlue", beBlue());
        nbt.put("SkullsCooldown", toNbtList(skullCooldowns[0], skullCooldowns[1]));
        nbt.put("UntilChargedSkull", toNbtList(headAttacksCounter[0], headAttacksCounter[1], headAttacksCounter[2], headAttacksCounter[3]));
        isReworked = MobAITweaks.getModConfigValue("wither_rework");
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void getNewStuffFromNBT(NbtCompound nbt, CallbackInfo ci) {
        try {
            var skullCooldownReader = nbt.getList("SkullsCooldown", NbtElement.INT_TYPE);
            for(int i = 0; i < 2; i++) skullCooldowns[i] = skullCooldownReader.getInt(i);
            var chargedSkullReader = nbt.getList("UntilChargedSkull", NbtElement.INT_TYPE);
            for(int i = 0; i < 4; i++) headAttacksCounter[i] = chargedSkullReader.getInt(i);
        }
        catch (Throwable ignore) {
        }
        if(nbt.contains("blockBreakingCooldown")) blockBreakingCooldown = nbt.getInt("blockBreakingCooldown");
        if(nbt.contains("ChargeAttackTime")) getDataTracker().set(CHARGE_TIME, nbt.getInt("ChargeAttackTime"));
        if(nbt.contains("SlamAttackTime")) getDataTracker().set(SLAM_TIME, nbt.getInt("SlamAttackTime"));
        getDataTracker().set(ENRAGED, nbt.getBoolean("IsEnraged"));
        getDataTracker().set(BLUE, nbt.getBoolean("IsBlue"));
    }
    @ModifyConstant(method = "mobTick", constant = @Constant(floatValue = 10.0F, ordinal = 0))
    private float fixSpawnHealingValue(float constant) {
        return getMaxHealth() / 30.0F;
    }
    @ModifyConstant(method = "mobTick", constant = @Constant(intValue = 20, ordinal = 1))
    private int realHealInterval(int constant) {
        return getTarget() == null ? 10 : isEnraged() || !isReworked ? constant : age + 1;
    }
    @ModifyReturnValue(method = "createWitherAttributes", at = @At("TAIL"))
    private static DefaultAttributeContainer.Builder attribution(DefaultAttributeContainer.Builder original) {
        if(MobAITweaks.getModConfigValue("wither_rework")) return original.add(EntityAttributes.GENERIC_ARMOR, 12.0d).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 120.0d);
        return original;
    }
    @Override public boolean damage(DamageSource source, float amount) {
        lastDamageSource = source;
        if(blockBreakingCooldown >= 20) blockBreakingCooldown = 0;
        if(isInvulnerableTo(source) || (getInvulnerableTimer() > 0 && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) || source == getDamageSources().mobAttack(this) || source.isOf(DamageTypes.BAD_RESPAWN_POINT) || source.isOf(DamageTypes.IN_FIRE) || source.isOf(DamageTypes.ON_FIRE) || source.isOf(DamageTypes.LAVA) || source.isOf(DamageTypes.WITHER) || source.isIn(DamageTypeTags.WITHER_IMMUNE_TO) || source.getAttacker() == this || source.getAttacker() instanceof WitherEntity) return false;
        else if(source.isOf(DamageTypes.MAGIC)) amount += (float)(int)(amount * 1.15f);
        else if(source.isOf(DamageTypes.ARROW) && shouldRenderOverlay()) amount *= 0.5f;
        else if(MobAITweaks.getModConfigValue("bosses_enrage") && !source.isOf(DamageTypes.OUT_OF_WORLD) && !source.isOf(DamageTypes.OUTSIDE_BORDER) && !source.isOf(DamageTypes.GENERIC_KILL) && !source.isOf(DamageTypes.CRAMMING) && !isEnraged() && amount >= getMaxHealth() * 0.2f) {
            getDataTracker().set(ENRAGED, true);
            heal(amount * 2f);
            if(getServer() != null && getServer().getPlayerManager() != null) getServer().getPlayerManager().broadcast(Text.translatable("mob-ai-tweaks.boss_enraged").copy().formatted(Formatting.RED), false);
            bossBar.setName(bossBar.getName().copy().formatted(Formatting.RED));
            return super.damage(source, getMaxHealth() * 0.2f);
        }
        return super.damage(source, amount);
    }
    @Override public void onDeath(DamageSource damageSource) {
        if(!MobAITweaks.getModConfigValue("wither_death_animation")) {
            if(getServer() != null && getServer().getPlayerManager() != null) getServer().getPlayerManager().broadcast(Text.translatable("mob-ai-tweaks.boss_defeated", getName().getString()).copy().formatted(Formatting.DARK_PURPLE), false);
            super.onDeath(damageSource);
            return;
        }
        lastDamageSource = damageSource;
        bossBar.setVisible(false);
        bossBar.clearPlayers();
        setTarget(null);
        for(int i = 0; i < 3; i++) setTrackedEntityId(i, 0);
        super.onDeath(damageSource);
    }
    @Override protected void updatePostDeath() {
        if(!MobAITweaks.getModConfigValue("wither_death_animation") || lastDamageSource == null || lastDamageSource.isOf(DamageTypes.GENERIC_KILL)) {
            super.updatePostDeath();
            return;
        }
        getNavigation().stop();
        setVelocity(-getVelocity().getX(), 0.1d, -getVelocity().getZ());
        for(var goal : goalSelector.getGoals()) goal.stop();
        for(var target : targetSelector.getGoals()) target.stop();
        if(getWorld().isClient()) {
            hurtTime = getRandom().nextBetween(1, 9);
            getWorld().addParticle(ParticleTypes.EXPLOSION, getX() + getRandom().nextBetween(-3, 3), MathHelper.lerp(0.5d, getEyeY(), getY()) + getRandom().nextBetween(-3, 3), getZ() + getRandom().nextBetween(-3, 3), 0d, 0d, 0d);
            getWorld().addParticle(ParticleTypes.FLASH, getX() + getRandom().nextBetween(-3, 3), MathHelper.lerp(0.5d, getEyeY(), getY()) + getRandom().nextBetween(-3, 3), getZ() + getRandom().nextBetween(-3, 3), 0d, 0d, 0d);
            return;
        }
        if(++deathTime % 10 == 0) {
            if(shouldDropXp() && lastDamageSource != null && lastDamageSource.getAttacker() != null) dropXp();
            shootSkullAt(getRandom().nextInt(4), getX() + getRandom().nextBetween(-100, 100), getY() + getRandom().nextBetween(-100, 100), getZ() + getRandom().nextBetween(-100, 100), getRandom().nextBoolean());
        }
        int totalDeathTime = DEATH_TICKS * 10;
        if(deathTime == totalDeathTime - 1) {
            getWorld().createExplosion(this, this.getDamageSources().explosion(this, this), new ExplosionBehavior(), getPos(), 8f, false, World.ExplosionSourceType.MOB);
            if(!isSilent()) getWorld().syncGlobalEvent(WorldEvents.WITHER_BREAKS_BLOCK, getBlockPos(), 0);
            if(getDeathSound() != null) playSound(getDeathSound(), getSoundVolume() * 1.3f, getSoundPitch());
        }
        else if(!isRemoved() && deathTime >= totalDeathTime) {
            if(lastDamageSource != null && lastDamageSource.getAttacker() != null && getServer() != null) drop(lastDamageSource);
            if(getServer() != null && getServer().getPlayerManager() != null) getServer().getPlayerManager().broadcast(Text.translatable("mob-ai-tweaks.boss_defeated", getName().getString()).copy().formatted(Formatting.DARK_PURPLE), false);
            getWorld().emitGameEvent(this, GameEvent.ENTITY_DIE, getPos());
            remove(RemovalReason.KILLED);
        }
    }
    @Override protected void drop(DamageSource damageSource) {
        if(!MobAITweaks.getModConfigValue("wither_death_animation") || isEnraged() || deathTime >= DEATH_TICKS * 10) super.drop(damageSource);
    }
    @Override protected void setRotation(float yaw, float pitch) {
        super.setRotation((float)serverYaw, pitch);
    }
    @SuppressWarnings("all")
    @Override public void setChargeTime(int i) {
        if(i >= 20 && getTarget() != null) blockBreakingCooldown = 0;
        getDataTracker().set(CHARGE_TIME, i);
    }
    @SuppressWarnings("all")
    @Override public void setSlamTime(int i) {
        if(i >= 20 && getTarget() != null && !canSee(getTarget())) blockBreakingCooldown = 0;
        getDataTracker().set(SLAM_TIME, i);
    }
    @SuppressWarnings("all")
    @Override public int getChargeTime() {
        return getDataTracker().get(CHARGE_TIME);
    }
    @SuppressWarnings("all")
    @Override public int getSlamTime() {
        return getDataTracker().get(SLAM_TIME);
    }
    @SuppressWarnings("all")
    @Override public boolean isEnraged() {
        return getDataTracker().get(ENRAGED);
    }
    @SuppressWarnings("all")
    @Override public boolean beBlue() {
        return getHealth() <= getMaxHealth() * 0.1666F || getDataTracker().get(BLUE);
    }
}

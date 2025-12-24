package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.goals.PhantomBossGoal;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;

@Mixin(PhantomEntity.class)
public abstract class PhantomEntityMixin extends FlyingEntity implements Monster {
    @Unique private static final TrackedData<Boolean> FROM_SPAWNER = DataTracker.registerData(PhantomEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private static final TrackedData<Boolean> BOSS = DataTracker.registerData(PhantomEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private final ServerBossBar bossBar = new ServerBossBar(getDisplayName(), BossBar.Color.BLUE, BossBar.Style.PROGRESS);
    @Shadow public abstract void setPhantomSize(int size);
    @Shadow public abstract int getPhantomSize();
    @Shadow Vec3d targetPosition = Vec3d.ZERO;
    @Shadow protected abstract void initGoals();
    PhantomEntityMixin(EntityType<? extends PhantomEntity> type, World world) {
        super(type, world);
    }
    @Override public boolean damage(DamageSource source, float amount) {
        float damageMultiplier = MobAITweaks.getModConfigValue("phantom_projectile_damage_boost", 200) * 0.01F + 1F;
        if(damageMultiplier <= 0F) return false;
        if(!getDataTracker().get(BOSS)) if(source.isIn(DamageTypeTags.IS_PROJECTILE)) if(amount > 0f) amount *= damageMultiplier;
        else amount += damageMultiplier;
        return super.damage(source, amount);
    }
    @Inject(method = "initDataTracker", at = @At("HEAD"))
    private void trackData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(FROM_SPAWNER, false);
        builder.add(BOSS, false);
    }
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void putNewStuffToNBT(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("IsBoss", getDataTracker().get(BOSS));
        nbt.putBoolean("IsFromSpawner", getDataTracker().get(FROM_SPAWNER));
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void getNewStuffFromNBT(NbtCompound nbt, CallbackInfo ci) {
        getDataTracker().set(BOSS, nbt.getBoolean("IsBoss"));
        getDataTracker().set(FROM_SPAWNER, nbt.getBoolean("IsFromSpawner"));
    }
    @Inject(method = "initialize", at = @At("TAIL"))
    private void onSpawn(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
        if(spawnReason == SpawnReason.TRIAL_SPAWNER || spawnReason == SpawnReason.SPAWNER) getDataTracker().set(FROM_SPAWNER, MobAITweaks.getModConfigValue("phantoms_from_spawner_always_attack"));
        else if(spawnReason == SpawnReason.NATURAL && !getEntityWorld().getEntitiesByClass(PhantomEntity.class, getBoundingBox().expand(128, 128, 128), p -> p.getDataTracker().get(BOSS)).isEmpty()) discard();
        else if(MobAITweaks.getModConfigValue("ominous_mobs") && getRandom().nextInt(100) < MobAITweaks.getModConfigValue("phantom_boss_spawn_chance", 1)) for(var p : getWorld().getPlayers()) if(MobAITweaks.isOminous(p)) getDataTracker().set(BOSS, true);
    }
    @Inject(method = "onTrackedDataSet", at = @At("TAIL"))
    private void becomeBoss(TrackedData<?> data, CallbackInfo ci) {
        if(data.equals(BOSS)) if(getDataTracker().get(BOSS)) {
            noClip = true;
            int phantomBossSize = MobAITweaks.getModConfigValue("phantom_boss_size", 20);
            if(getPhantomSize() < phantomBossSize) setPhantomSize(phantomBossSize);
            var attribute2 = getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            int phantomBossHP = MobAITweaks.getModConfigValue("phantom_boss_health", 100);
            if(attribute2 != null && attribute2.getValue() != phantomBossHP) {
                attribute2.setBaseValue(phantomBossHP);
                setHealth(phantomBossHP);
            }
            var attribute3 = getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
            int phantomBossFR = MobAITweaks.getModConfigValue("phantom_boss_follow_range", 160);
            if(attribute3 != null && attribute3.getValue() < phantomBossFR) attribute3.setBaseValue(phantomBossFR);
            var attribute4 = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            if(attribute4 != null && attribute4.getValue() != 14) attribute4.setBaseValue(14);
        }
        else {
            bossBar.clearPlayers();
            bossBar.setVisible(false);
            noClip = false;
            setPhantomSize(1);
            var attribute2 = getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            int phantomBossHP = 20;
            if(attribute2 != null && attribute2.getValue() != phantomBossHP) {
                attribute2.setBaseValue(phantomBossHP);
                setHealth(phantomBossHP);
            }
            var attribute3 = getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
            int phantomBossFR = 16;
            if(attribute3 != null && attribute3.getValue() != phantomBossFR) attribute3.setBaseValue(phantomBossFR);
            initGoals();
        }
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void reworkTargeting(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("ominous_mobs")) goalSelector.add(0, new PhantomBossGoal((PhantomEntity)(FlyingEntity)this) {
            @Override public boolean canStart() {
                return getDataTracker().get(BOSS);
            }
            @Override public void start() {
                super.start();
                goalSelector.clear(p -> !equals(p));
                if(getTarget() != null) targetPosition = getTarget().getEyePos();
            }
            @Override protected Vec3d getTargetPosition() {
                return targetPosition;
            }
            @Override protected void setTargetPosition(Vec3d where) {
                targetPosition = where;
            }
        });
        if(!MobAITweaks.getModConfigValue("phantom_rework")) return;
        targetSelector.getGoals().clear();
        targetSelector.add(0, new Goal() {
            private boolean shootSpitBall = false;
            private int delay = toGoalTicks(20);
            @Override public boolean canStart() {
                if (delay > 0) {
                    delay--;
                    return false;
                }
                else if(getAttacker() != null) {
                    setTarget(getAttacker());
                    return true;
                }
                else if(getPhantomSize() > MobAITweaks.getModConfigValue("phantoms_become_hostile_size", 3) || getDataTracker().get(FROM_SPAWNER)/* || getDataTracker().get(BOSS)*/) {
                    delay = toGoalTicks(60);
                    List<PlayerEntity> list = getWorld().getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(16.0, 64.0, 16.0), p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && distanceTo(p) < 64);
                    if (list.isEmpty()) return false;
                    list.sort(Comparator.comparing(Entity::getY).reversed());
                    for (PlayerEntity player : list) if (isTarget(player, TargetPredicate.DEFAULT)) {
                        setTarget(player);
                        return true;
                    }
                    return false;
                }
                else {
                    delay = toGoalTicks(60);
                    List<AnimalEntity> list = getWorld().getEntitiesByClass(AnimalEntity.class, getBoundingBox().expand(16.0, 64.0, 16.0), p -> distanceTo(p) < 64);
                    if (list.isEmpty()) return false;
                    list.sort(Comparator.comparing(Entity::getY).reversed());
                    for (AnimalEntity animal : list) if(animal instanceof CatEntity) return false;
                    else if (isTarget(animal, TargetPredicate.DEFAULT) && !animal.isBaby()) {
                        setTarget(animal);
                        return true;
                    }
                    return false;
                }
            }
            @Override public boolean shouldContinue() {
                return getTarget() != null && isTarget(getTarget(), TargetPredicate.DEFAULT);
            }
            @Override public void start() {
                if(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) shootSpitBall = true;
                super.start();
            }
            @Override public void tick() {
                if(!getDataTracker().get(BOSS) && shootSpitBall && getTarget() instanceof LivingEntity target && Math.abs(Math.atan2(target.getZ() - getZ(), target.getX() - getX()) - Math.atan2(getVelocity().getZ(), getVelocity().getX())) < 0.1 && distanceTo(target) > 10) {
                    PotionEntity spit = new PotionEntity(getWorld(), PhantomEntityMixin.this);
                    ItemStack ball = Items.SLIME_BLOCK.getDefaultStack();
                    ball.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.STRONG_POISON));
                    spit.setItem(ball);
                    spit.setNoGravity(true);
                    spit.setPosition(getEyePos().add(getVelocity()));
                    if(getTarget() != null) spit.setVelocity(getTarget().getEyePos().add(getTarget().getVelocity()).subtract(spit.getPos()).normalize().multiply(2d));
                    else spit.setVelocity(getRotationVector().multiply(2d));
                    getWorld().spawnEntity(spit);
                    if(!isSilent()) getWorld().playSound(PhantomEntityMixin.this, getBlockPos(), SoundEvents.BLOCK_SLIME_BLOCK_BREAK, SoundCategory.HOSTILE, 4f, 1f);
                    if(getWorld() instanceof ServerWorld serverWorld) serverWorld.spawnParticles(ParticleTypes.ITEM_SLIME, getX(), getEyeY(), getZ(), getPhantomSize() * 2 + 4, 0.1, 0.1, 0.1, 0.2);
                    shootSpitBall = false;
                }
                super.tick();
            }
        });
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void bossHealthBar(CallbackInfo ci) {
        if(getDataTracker().get(BOSS)) {
            double followRange = getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE);
            if(getTarget() instanceof ServerPlayerEntity target && distanceTo(target) < followRange) bossBar.addPlayer(target);
            if(getLastAttacker() instanceof ServerPlayerEntity attacker && distanceTo(attacker) < followRange) bossBar.addPlayer(attacker);
            for(var player : bossBar.getPlayers()) if(distanceTo(player) >= followRange) bossBar.removePlayer(player);
            bossBar.setPercent(getHealth() / getMaxHealth());
            if(hasCustomName()) bossBar.setName(getDisplayName());
            else bossBar.setName(Text.translatable("mob-ai-tweaks.ominous_phantom"));
            handSwingProgress = 1f;
        }
    }
    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/PhantomEntity;isAffectedByDaylight()Z"))
    private boolean doNotBurnInDaylightIfBoss(boolean original) {
        return original && !getDataTracker().get(BOSS);
    }
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"), index = 0)
    private ParticleEffect replaceParticleWhenBoss(ParticleEffect parameters) {
        if(getDataTracker().get(BOSS)) return new DustColorTransitionParticleEffect(new Vector3f(25F, 255F, 10F).div(255F), new Vector3f(10F, 10F, 10F).div(255F / 2F), 2F);
        return parameters;
    }
    @Override public boolean onKilledOther(ServerWorld world, LivingEntity other) {
        if(MobAITweaks.getModConfigValue("phantom_rework") && !getDataTracker().get(BOSS) && other instanceof AnimalEntity) setPhantomSize(getPhantomSize() + 1);
        return super.onKilledOther(world, other);
    }
    @Override protected void onKilledBy(@Nullable LivingEntity adversary) {
        if(getDataTracker().get(BOSS)) bossBar.setPercent(0f);
        if(MobAITweaks.getModConfigValue("phantom_rework") && getPhantomSize() > MobAITweaks.getModConfigValue("phantom_reset_insomnia_size", 3) && adversary instanceof PlayerEntity player && player instanceof ServerPlayerEntity serverPlayer) serverPlayer.getStatHandler().setStat(player, Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST), 0);
        super.onKilledBy(adversary);
    }
    @Override protected void onRemoval(RemovalReason reason) {
        bossBar.clearPlayers();
        bossBar.setVisible(false);
        if(MobAITweaks.getModConfigValue("bosses_announce_spawn_and_death") && getDataTracker().get(BOSS) && getServer() != null && getServer().getPlayerManager() != null) {
            boolean useTerrariaText = FabricLoader.getInstance().isModLoaded("terra_entity");
            var message = Text.translatable(useTerrariaText ? (reason == Entity.RemovalReason.KILLED ? "message.terraentity.boss_leave" : "message.terraentity.boss_discard") : "mob-ai-tweaks.boss_defeated", Text.translatable("mob-ai-tweaks.ominous_phantom").getString()).copy().formatted(Formatting.DARK_PURPLE);
            if(useTerrariaText) message.formatted(Formatting.BOLD);
            if(reason == RemovalReason.KILLED || useTerrariaText) getServer().getPlayerManager().broadcast(message, false);
        }
        super.onRemoval(reason);
    }
}

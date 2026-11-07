package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.goals.SlimeDispenserGoal;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SlimeEntity.class)
public abstract class SlimeEntityMixin extends MobEntity {
    @Shadow public abstract int getSize();
    @Shadow public abstract void setSize(int size, boolean heal);
    @Shadow public abstract EntityType<? extends SlimeEntity> getType();
    @Shadow public abstract boolean isSmall();
    @Shadow public float targetStretch;
    @Shadow private boolean onGroundLastTick;
    @Shadow protected abstract ParticleEffect getParticles();
    @Unique private int attackAnimationTime;
    protected SlimeEntityMixin(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "initGoals", at = @At("HEAD"))
    private void addMoreBehaviors(CallbackInfo ci) {
        targetSelector.add(0, new ActiveTargetGoal<>(this, SlimeEntity.class, true, s -> !getMainHandStack().isOf(Items.DISPENSER) && getRandom().nextInt(100) < MobAITweaks.getModConfigValue("slime_merge_chance", 1) && getTarget() == null && s.getType() == getType() && s instanceof SlimeEntity ss && getOffHandStack() == ss.getOffHandStack() && !ss.getMainHandStack().isOf(Items.DISPENSER) && (getHealth() < getMaxHealth() ? ss.getSize() < getSize() : ss.getSize() <= getSize())) {
            @Override public void setTargetEntity(@Nullable LivingEntity targetEntity) {
                super.setTargetEntity(targetEntity);
                if(targetEntity instanceof SlimeEntity s) s.setTarget(SlimeEntityMixin.this);
            }
        });
        goalSelector.add(4, new SlimeDispenserGoal((SlimeEntity)(MobEntity)this));
    }
    @Inject(method = "pushAwayFrom", at = @At("TAIL"))
    private void mergeWithKindness(Entity entity, CallbackInfo ci) {
        if(entity instanceof SlimeEntity s && getSize() <= s.getSize()) if(getTarget() == s) {
            if(getSize() < s.getSize()) setSize(getSize(), true);
            else if(getSize() > s.getSize()) {
                s.setSize(s.getSize(), true);
                discard();
                return;
            }
            else setSize(getSize() + 1, true);
            if(!isRemoved()) s.discard();
        }
        else if(getSize() != s.getSize() && s.getTarget() == this) {
            if(isSmall()) s.setSize(s.getSize(), true);
            else s.setSize(s.getSize() + 1, true);
            discard();
        }
    }
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void becomeMagmaCube(RemovalReason reason, CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("slimes_turn_into_magma_cubes_in_lava") && !getWorld().isClient() && isInLava() && isDead()) {
            MagmaCubeEntity magmaCube = EntityType.MAGMA_CUBE.create(getWorld());
            if(magmaCube != null) {
                if(isPersistent()) magmaCube.setPersistent();
                magmaCube.setCustomName(getCustomName());
                magmaCube.setAiDisabled(isAiDisabled());
                magmaCube.setInvulnerable(isInvulnerable());
                magmaCube.setSize(getSize(), true);
                magmaCube.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0.0F);
                getWorld().spawnEntity(magmaCube);
            }
            super.remove(reason);
            ci.cancel();
        }
    }
    @Inject(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/SlimeEntity;setSize(IZ)V"))
    private void copyOffhandItemIGuess(RemovalReason reason, CallbackInfo ci, @Local SlimeEntity slime) {
        //transfer offhand item to split slimes
        slime.setStackInHand(Hand.OFF_HAND, getOffHandStack());
        slime.setEquipmentDropChance(EquipmentSlot.OFFHAND, 0F);
    }
    @Inject(method = "initialize", at = @At("TAIL"))
    private void spawnWithBlocksInside(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
        if(getType() != EntityType.SLIME || spawnReason == SpawnReason.CHUNK_GENERATION) return;
        if(spawnReason != SpawnReason.JOCKEY && getVehicle() == null && world.getRandom().nextInt(100) < MobAITweaks.getModConfigValue("slimes_with_dispenser_chance", 20) && difficulty.getLocalDifficulty() > MobAITweaks.getModConfigValue("slimes_with_dispenser_local_difficulty", 150) * 0.01F) {
            setSize(3, true);
            setStackInHand(Hand.MAIN_HAND, Items.DISPENSER.getDefaultStack());
            setEquipmentDropChance(EquipmentSlot.MAINHAND, 1F);
        }
        //Use offhand to save data since slimes can't use items anyway
        //Also allows for commands to do stuff to them better
        if(!MobAITweaks.getModConfigValue("slimes_can_be_made_of_honey") || !(world instanceof ServerWorld)) return;
        var biome = world.getBiome(getBlockPos());
        setStackInHand(Hand.OFF_HAND, (biome.isIn(BiomeTags.IS_BADLANDS) || biome.isIn(ConventionalBiomeTags.IS_BIRCH_FOREST) || (getWorld().getBlockState(getSteppingPos()).isOf(Blocks.HONEYCOMB_BLOCK)) ? Items.HONEY_BLOCK : Items.SLIME_BLOCK).getDefaultStack());
        setEquipmentDropChance(EquipmentSlot.OFFHAND, 0F);
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void playAttackAnimation(CallbackInfo ci) {
        if(getWorld().isClient() && getMainHandStack().isOf(Items.DISPENSER)) {
            if(isAttacking()) attackAnimationTime++;
            else attackAnimationTime = 0;
            if(attackAnimationTime > 0) targetStretch = (float)Math.sin(attackAnimationTime / 30F * Math.PI) * 0.5F;
            if(attackAnimationTime >= 20) targetStretch *= -2F;
            if(attackAnimationTime == 20) {
                playSound(SoundEvents.BLOCK_DISPENSER_DISPENSE);
                playSound(SoundEvents.BLOCK_DISPENSER_LAUNCH);
                var offset = getRotationVector();
                for(int i = 0; i < 5; i++) getWorld().addParticle(getParticles(), getX() + offset.getX(), getY() + offset.getY() + 0.4, getZ() + offset.getZ(), offset.getX() * getSize() * i, offset.getY() * getSize() * i, offset.getZ() * getSize() * i);
                for(int i = 2; i < 7; i++) getWorld().addParticle(ParticleTypes.SMOKE, getX() + offset.getX(), getY() + offset.getY() + 0.4, getZ() + offset.getZ(), offset.getX() * i * 0.1F, offset.getY() * i * 0.1F, offset.getZ() * i * 0.1F);
            }
        }
    }
    @Inject(method = "getParticles", at = @At("HEAD"), cancellable = true)
    private void replaceParticles(CallbackInfoReturnable<ParticleEffect> cir) {
        if(!getOffHandStack().isEmpty() && getOffHandStack().getItem() instanceof BlockItem b) cir.setReturnValue(new BlockStateParticleEffect(ParticleTypes.BLOCK, b.getBlock().getDefaultState()));
    }
    @Override protected void updatePostDeath() {
        if(!MobAITweaks.getModConfigValue("slimes_death_animation")) {
            super.updatePostDeath();
            return;
        }
        if(getWorld().isClient() && hurtTime < 2) hurtTime++;
        if(isSmall()) {
            if(!getWorld().isClient()) super.updatePostDeath();
            else if(hurtTime < maxHurtTime / 2 && isOnGround() && onGroundLastTick && getVelocity().getY() < 0) targetStretch = -1.5F;
            return;
        }
        if(getWorld().isClient()) {
            if(hurtTime < maxHurtTime / 2 && isOnGround() && onGroundLastTick && getVelocity().getY() < 0) targetStretch = -1.5F;
            getWorld().addParticle(getParticles(), getParticleX(1), getRandomBodyY(), getParticleZ(1), 0.0, 0.0, 0.0);
            return;
        }
        if(++deathTime > maxHurtTime * 2) discard();
    }
}

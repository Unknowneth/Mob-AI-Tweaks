package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.block.SkullBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;

@Mixin(AllayEntity.class)
public abstract class AllayEntityMixin extends PathAwareEntity {
    @Shadow public abstract Brain<AllayEntity> getBrain();
    @Shadow public abstract void travel(Vec3d movementInput);
    @Shadow public abstract void setDancing(boolean dancing);
    AllayEntityMixin(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "mobTick", at = @At("HEAD"), cancellable = true)
    private void defendLikedPlayer(CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("allay_rework")) return;
        setAttacking(getMainHandStack().getItem() instanceof BowItem || getMainHandStack().getItem() instanceof CrossbowItem || getMainHandStack().getItem() instanceof ToolItem || getMainHandStack().getItem() instanceof MaceItem);
        if(isPanicking()) {
            stopUsingItem();
            return;
        }
        if(getTarget() != null) if(attackMobsPerhaps(getTarget())) {
            super.mobTick();
            ci.cancel();
            if(getTarget().isAlive()) return;
            setTarget(null);
            stopUsingItem();
            return;
        }
        if(age % 20 == 0) getBrain().getOptionalRegisteredMemory(MemoryModuleType.LIKED_NOTEBLOCK).ifPresent(n -> {
            if(getWorld().getBlockState(n.pos().up()).getBlock() instanceof SkullBlock skullType) switch(skullType.getSkullType()) {
                case SkullBlock.Type.CREEPER:
                    List<HostileEntity> list1 = getWorld().getEntitiesByClass(HostileEntity.class, getBoundingBox().expand(16.0, 16.0, 16.0), h -> (h instanceof SpiderEntity || h instanceof CreeperEntity) && distanceTo(h) < 16);
                    if(list1.isEmpty()) return;
                    list1.sort(Comparator.comparing(this::distanceTo));
                    attackMobsPerhaps(list1.getFirst());
                    break;
                case SkullBlock.Type.ZOMBIE:
                    List<HostileEntity> list2 = getWorld().getEntitiesByClass(HostileEntity.class, getBoundingBox().expand(16.0, 16.0, 16.0), h -> h.hasInvertedHealingAndHarm() && distanceTo(h) < 16);
                    if(list2.isEmpty()) return;
                    list2.sort(Comparator.comparing(this::distanceTo));
                    attackMobsPerhaps(list2.getFirst());
                    break;
                case SkullBlock.Type.SKELETON:
                    List<HostileEntity> list3 = getWorld().getEntitiesByClass(HostileEntity.class, getBoundingBox().expand(16.0, 16.0, 16.0), h -> distanceTo(h) < 16);
                    if(list3.isEmpty()) return;
                    list3.sort(Comparator.comparing(this::distanceTo));
                    attackMobsPerhaps(list3.getFirst());
                    break;
                case SkullBlock.Type.WITHER_SKELETON:
                    List<LivingEntity> list4 = getWorld().getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(16.0, 16.0, 16.0), h -> !(h instanceof AllayEntity) && !h.isInCreativeMode() && h.isSpectator() && h.isInvulnerable() && distanceTo(h) < 16);
                    if(list4.isEmpty()) return;
                    list4.sort(Comparator.comparing(this::distanceTo));
                    attackMobsPerhaps(list4.getFirst());
                    break;
                case SkullBlock.Type.PIGLIN:
                    List<AnimalEntity> list5 = getWorld().getEntitiesByClass(AnimalEntity.class, getBoundingBox().expand(16.0, 16.0, 16.0), h -> distanceTo(h) < 16);
                    if(list5.isEmpty()) return;
                    list5.sort(Comparator.comparing(this::distanceTo));
                    attackMobsPerhaps(list5.getFirst());
                    break;
                case SkullBlock.Type.PLAYER:
                    List<PlayerEntity> list6 = getWorld().getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(16.0, 16.0, 16.0), h -> !h.isSpectator() && !h.isCreative() && distanceTo(h) < 16);
                    if(list6.isEmpty()) return;
                    list6.sort(Comparator.comparing(this::distanceTo));
                    attackMobsPerhaps(list6.getFirst());
                    break;
                default:
            }
        });
        getBrain().getOptionalRegisteredMemory(MemoryModuleType.LIKED_PLAYER).ifPresent(s -> {
            if(getServer() != null) {
                PlayerEntity player = getServer().getPlayerManager().getPlayer(s);
                if(player != null) if(player.getAttacker() != null) if(attackMobsPerhaps(player.getAttacker())) {
                    super.mobTick();
                    ci.cancel();
                }
                else stopUsingItem();
            }
        });
    }
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void avoidFriendlyFire(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(source.getAttacker() instanceof AllayEntity || source.getSource() instanceof AllayEntity) cir.setReturnValue(false);
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void tickHandSwingFix(CallbackInfo ci) {
        tickHandSwing();
    }
    @Unique private boolean attackMobsPerhaps(LivingEntity target) {
        if(!MobAITweaks.getModConfigValue("allay_rework") || MobAITweaks.allayItemBlacklist.contains(getMainHandStack().getItem().toString())) return false;
        boolean canLookAtTarget = false;
        if(getMainHandStack().isOf(Items.WIND_CHARGE)) {
            Vec3d offset = getRotationVector(0f, getYaw());
            offset = target.getPos().add(offset.getX() * -8d, 8d, offset.getZ() * -8d).add(target.getVelocity());
            if(squaredDistanceTo(offset) < 36 && age % 20 == 0) {
                WindChargeEntity windCharge = new WindChargeEntity(getWorld(), getY(), getEyeY(), getZ(), getRotationVector());
                windCharge.setOwner(this);
                windCharge.setVelocity(getRotationVector().multiply(3d));
                windCharge.setPosition(getEyePos());
                getWorld().spawnEntity(windCharge);
                getWorld().playSound(this, getBlockPos(), SoundEvents.ENTITY_BREEZE_SHOOT, SoundCategory.NEUTRAL, 1f, getWorld().getRandom().nextFloat() * 0.4f + 0.8f);
            }
            setVelocity(offset.subtract(getPos()).normalize().multiply(getAttributeValue(EntityAttributes.GENERIC_FLYING_SPEED) * 1.5d));
            canLookAtTarget = true;
        }
        else if(getMainHandStack().isOf(Items.FIRE_CHARGE)) {
            Vec3d offset = getRotationVector(0f, getYaw());
            offset = target.getPos().add(offset.getX() * -5.19615d, 9d, offset.getZ() * -5.19615d).add(target.getVelocity());
            if(squaredDistanceTo(offset) < 36 && (age / 21) % 2 == 0 && age % 7 == 0) {
                SmallFireballEntity fireball = new SmallFireballEntity(getWorld(), this, getRotationVector());
                fireball.setVelocity(fireball.getVelocity().getX(), fireball.getVelocity().getY(),fireball.getVelocity().getZ(), 1, 1);
                fireball.setPosition(getEyePos());
                getWorld().spawnEntity(fireball);
                getWorld().playSound(this, getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.NEUTRAL, 1f, getWorld().getRandom().nextFloat() * 0.4f + 0.8f);
            }
            setVelocity(offset.subtract(getPos()).normalize().multiply(getAttributeValue(EntityAttributes.GENERIC_FLYING_SPEED) * 1.5d));
            canLookAtTarget = true;
        }
        else if(getMainHandStack().getItem() instanceof CrossbowItem crossbow) {
            Vec3d offset = getRotationVector(0f, getYaw());
            offset = target.getPos().add(offset.getX() * -12d, 4.36765d, offset.getZ() * -12d).add(target.getVelocity());
            if(squaredDistanceTo(offset) < 144) setCurrentHand(Hand.MAIN_HAND);
            int arrowCount = 1;
            boolean hasFlame = false;
            boolean hasWithering = false;
            boolean hasFeatherweight = false;
            if(getMainHandStack().hasEnchantments()) {
                var enchants = getMainHandStack().getEnchantments().getEnchantments();
                hasFlame = enchants.stream().anyMatch(e -> e.matchesId(Identifier.ofVanilla("flame")));
                hasWithering = enchants.stream().anyMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "withering_munitions")));
                hasFeatherweight = enchants.stream().anyMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "featherweight_munitions")));
                if(getWorld() instanceof ServerWorld server) arrowCount = EnchantmentHelper.getProjectileCount(server, getMainHandStack(), this, arrowCount);
            }
            if(getItemUseTime() >= crossbow.getMaxUseTime(getMainHandStack(), this) * 2) for(int i = 0; i < arrowCount; i++) if(hasWithering) {
                WitherSkullEntity skull = new WitherSkullEntity(getWorld(), this, getRotationVector().multiply(1.5d).rotateY(i > 0 ? arrowCount * (float)(Math.PI * 0.1d) - i * (float)(Math.PI * 0.2d) : 0F));
                if(hasFlame) skull.setFireTicks(300);
                getWorld().spawnEntity(skull);
                if(i != 0) continue;
                stopUsingItem();
                getWorld().playSound(this, getBlockPos(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.NEUTRAL, 1f, getWorld().getRandom().nextFloat() * 0.4f + 0.8f);
            }
            else {
                ArrowEntity arrow = new ArrowEntity(getWorld(), this, Items.ARROW.getDefaultStack(), arrowCount > 1 ? getMainHandStack() : getActiveItem());
                arrow.setPosition(getEyePos());
                arrow.setVelocity(getRotationVector().multiply(3.2d).rotateY(i > 0 ? arrowCount * (float)(Math.PI * 0.1d) - i * (float)(Math.PI * 0.2d) : 0F));
                arrow.setCritical(true);
                if(hasFlame) arrow.setFireTicks(300);
                if(hasFeatherweight) arrow.setNoGravity(true);
                getWorld().spawnEntity(arrow);
                if(i != 0) continue;
                stopUsingItem();
                getWorld().playSound(this, getBlockPos(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.NEUTRAL, 1f, getWorld().getRandom().nextFloat() * 0.4f + 0.8f);
            }
            setVelocity(offset.subtract(getPos()).normalize().multiply(getAttributeValue(EntityAttributes.GENERIC_FLYING_SPEED) * 1.5d));
            canLookAtTarget = true;
        }
        else if(getMainHandStack().getItem() instanceof BowItem) {
            Vec3d offset = getRotationVector(0f, getYaw());
            offset = target.getPos().add(offset.getX() * -9d, 5.19615d, offset.getZ() * -9d).add(target.getVelocity());
            if(squaredDistanceTo(offset) < 49) setCurrentHand(Hand.MAIN_HAND);
            if(getItemUseTime() >= 20) {
                ArrowEntity arrow = new ArrowEntity(getWorld(), this, Items.ARROW.getDefaultStack(), getActiveItem());
                arrow.setPosition(getEyePos());
                arrow.setVelocity(getRotationVector().multiply(1.6d));
                arrow.setCritical(true);
                if(getActiveItem().hasEnchantments() && getActiveItem().getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesId(Identifier.ofVanilla("flame")))) arrow.setFireTicks(300);
                getWorld().spawnEntity(arrow);
                stopUsingItem();
                getWorld().playSound(this, getBlockPos(), SoundEvents.ENTITY_SKELETON_SHOOT, SoundCategory.NEUTRAL, 1f, getWorld().getRandom().nextFloat() * 0.4f + 0.8f);
            }
            setVelocity(offset.subtract(getPos()).normalize().multiply(getAttributeValue(EntityAttributes.GENERIC_FLYING_SPEED) * 1.5d));
            canLookAtTarget = true;
        }
        else if(getMainHandStack().getItem() instanceof ToolItem || getMainHandStack().getItem() instanceof MaceItem) {
            if(getBoundingBox().intersects(target.getBoundingBox()) && tryAttack(target)) swingHand(Hand.MAIN_HAND);
            else setVelocity(target.getEyePos().subtract(getPos()).normalize().multiply(getAttributeValue(EntityAttributes.GENERIC_FLYING_SPEED) * 1.5d));
            canLookAtTarget = true;
        }
        if(canLookAtTarget) {
            if(!target.equals(getTarget())) setTarget(target);
            setDancing(false);
            lookAtEntity(target, 30f, 30f);
            getLookControl().lookAt(target, 60f, 60f);
            getNavigation().stop();
        }
        return canLookAtTarget;
    }
}

package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Optional;

@Mixin(ShulkerEntity.class)
public abstract class ShulkerEntityMixin extends GolemEntity {
    @Shadow @Nullable public abstract DyeColor getColor();
    @Unique private static final TrackedData<Boolean> BREEDING = DataTracker.registerData(ShulkerEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
    protected ShulkerEntityMixin(EntityType<? extends GolemEntity> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void addAbilityToBreed(DataTracker.Builder builder, CallbackInfo ci){
        builder.add(BREEDING, false);
    }
    @ModifyArgs(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/GolemEntity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private void takeIncreasedDamageFromPickaxes(Args args) {
        if(args.get(0) instanceof DamageSource d && d.getAttacker() != null && d.getAttacker().getWeaponStack() != null && d.getAttacker().getWeaponStack().isIn(ItemTags.PICKAXES) && args.get(1) instanceof Float f) args.set(1, (MobAITweaks.getModConfigValue("shulker_pickaxe_damage_boost", 150) * 0.01F + 1F) * f);
    }
    @Inject(method = "spawnNewShulker", at = @At("HEAD"))
    private void healWhenBreeding(CallbackInfo ci) {
        if(getDataTracker().get(BREEDING)) {
            int healAmount = MobAITweaks.getModConfigValue("shulker_breed_heal_amount", 10);
            heal(healAmount);
            if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), healAmount, 0.1, 0.1, 0.1, 0.1);
        }
    }
    @Inject(method = "spawnNewShulker", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void stopBreeding(CallbackInfo ci) {
        getDataTracker().set(BREEDING, false);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void targetOtherShulkersWhenBred(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("shulkers_are_breedable")) targetSelector.add(4, new ActiveTargetGoal<>(this, ShulkerEntity.class, true, s -> s.getId() != getId() && s.getDataTracker().get(BREEDING)) {
            @Override protected Box getSearchBox(double distance) {
                Direction direction = ((ShulkerEntity)this.mob).getAttachedFace();
                if (direction.getAxis() == Direction.Axis.X) return this.mob.getBoundingBox().expand(4.0, distance, distance);
                else return direction.getAxis() == Direction.Axis.Z ? this.mob.getBoundingBox().expand(distance, distance, 4.0) : this.mob.getBoundingBox().expand(distance, 4.0, distance);
            }
            @Override public void tick() {
                super.tick();
                if(getTarget() instanceof ShulkerEntity s && !s.getDataTracker().get(BREEDING)) setTarget(null);
            }
        });
    }
    @Override protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if(MobAITweaks.getModConfigValue("shulkers_are_breedable") && player.getStackInHand(hand).isIn(MobAITweaks.SHULKER_BREEDING_ITEMS)) {
            getDataTracker().set(BREEDING, true);
            if(!getWorld().isClient) player.getStackInHand(hand).decrementUnlessCreative(1, player);
            return ActionResult.success(getWorld().isClient);
        }
        if(MobAITweaks.getModConfigValue("shulkers_are_dyeable") && player.getStackInHand(hand).getItem() instanceof DyeItem dye) {
            if(dye.getColor().equals(getColor())) return ActionResult.PASS;
            ((ShulkerEntity)(GolemEntity)this).setVariant(Optional.of(dye.getColor()));
            if(!getWorld().isClient) player.getStackInHand(hand).decrementUnlessCreative(1, player);
            return ActionResult.success(getWorld().isClient);
        }
        return ActionResult.PASS;
    }
    @Override protected void onKilledBy(@Nullable LivingEntity adversary) {
        if(getWorld().getGameRules().getBoolean(GameRules.DO_MOB_LOOT) && !getWorld().isClient) if(!shouldDropLoot()) {
            dropStack(ShulkerBoxBlock.getItemStack(getColor()));
            emitGameEvent(GameEvent.ENTITY_PLACE);
            int healAmount = MobAITweaks.getModConfigValue("shulker_breed_heal_amount", 10);
            heal(healAmount);
            if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), healAmount, 0.1, 0.1, 0.1, 0.1);
        }
        else if(MobAITweaks.getModConfigValue("shulkers_are_dyeable") && getColor() != null) {
            int amountToDrop = MobAITweaks.getModConfigValue("shulker_duplicate_dye_amount", 3);
            if(adversary != null && adversary.getWorld() instanceof ServerWorld server && getRecentDamageSource() != null) amountToDrop += (int)(EnchantmentHelper.getEquipmentDropChance(server, adversary, getRecentDamageSource(), 1F) * 100F) - 100;
            dropStack(new ItemStack(DyeItem.byColor(getColor()), amountToDrop));
            emitGameEvent(GameEvent.ENTITY_PLACE);
        }
        super.onKilledBy(adversary);
    }
    @Override protected boolean shouldDropLoot() {
        if(MobAITweaks.getModConfigValue("shulkers_can_be_silk_touched")) {
            DamageSource d = getRecentDamageSource();
            if(d != null && d.getAttacker() != null && d.getAttacker().getWeaponStack() != null && d.getAttacker().getWeaponStack().isIn(ItemTags.PICKAXES) && d.getAttacker().getWeaponStack().getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesId(Identifier.of("minecraft", "silk_touch")))) return false;
        }
        return super.shouldDropLoot();
    }
}

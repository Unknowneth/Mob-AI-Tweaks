package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity {
    @Shadow public abstract boolean isAttacking();
    @Shadow public abstract void setAttacking(boolean attacking);
    @Shadow public abstract @Nullable LivingEntity getTarget();
    @Shadow public abstract void setTarget(@Nullable LivingEntity target);
    @Shadow public abstract boolean hasPositionTarget();
    @Shadow public abstract void setPositionTarget(BlockPos target, int range);
    @Shadow public abstract BlockPos getPositionTarget();
    @Shadow public abstract float getPositionTargetRange();
    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void addExtraCodeToNbt(NbtCompound nbt, CallbackInfo ci) {
        if(age == 0) return;
        NbtCompound usedItem = new NbtCompound();
        if(isUsingItem()) {
            usedItem.putInt("time", getItemUseTime());
            usedItem.putByte("slot", (dataTracker.get(LIVING_FLAGS) & 2) > 0 ? (byte)1 : (byte)0);
        }
        nbt.put("UsedItem", usedItem);
        nbt.putBoolean("IsAggressive", isAttacking());
        if(hasPositionTarget()) nbt.putIntArray("PositionTarget", new int[]{getPositionTarget().getX(), getPositionTarget().getY(), getPositionTarget().getZ(), (int)getPositionTargetRange()});
        if(!getWorld().isClient() && getTarget() != null) nbt.putUuid("Target", getTarget().getUuid());
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void getExtraCodeToNbt(NbtCompound nbt, CallbackInfo ci) {
        if(age == 0) return;
        if(nbt.contains("UsedItem")) {
            NbtCompound usedItem = nbt.getCompound("UsedItem");
            boolean hasSlotParameter = usedItem.contains("slot");
            boolean hasTimeParameter = usedItem.contains("time");
            boolean useOffHand = hasSlotParameter && usedItem.getByte("slot") == (byte)1;
            var itemToUse = activeItemStack != null ? activeItemStack : useOffHand ? getOffHandStack() : getMainHandStack();
            if(hasTimeParameter) itemUseTimeLeft = itemToUse != null && !itemToUse.isEmpty() ? itemToUse.getMaxUseTime(this) - usedItem.getInt("time") : 0;
            if(!hasSlotParameter || usedItem.getByte("slot") == (byte)-1) clearActiveItem();
            else setCurrentHand(useOffHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
        }
        setAttacking(nbt.getBoolean("IsAggressive"));
        if(nbt.contains("PositionTarget")) {
            int[] positionTargetData = nbt.getIntArray("PositionTarget");
            if(positionTargetData.length == 4) setPositionTarget(new BlockPos(positionTargetData[0], positionTargetData[1], positionTargetData[2]), positionTargetData[3]);
        }
        if(nbt.contains("Target") && getWorld() instanceof ServerWorld serverWorld && serverWorld.getEntity(nbt.getUuid("Target")) instanceof LivingEntity target) setTarget(target);
    }
    @Inject(method = "isAttacking", at = @At("TAIL"), cancellable = true)
    private void overrideIsAttackingForSpecialMobs(CallbackInfoReturnable<Boolean> cir) {
        if(this instanceof SpecialAttacksInterface s) if(s.attackType().equals("GRENADE") && s.getSpecialCooldown() > 0) cir.setReturnValue(true);
        else if(s.attackType().equals("FLIP") && Math.abs(s.getSpecialCooldown()) < 10 && s.getSpecialCooldown() != 0) cir.setReturnValue(false);
    }
    @Inject(method = "dropLoot", at = @At("TAIL"))
    private void dropSpecialLoot(DamageSource damageSource, boolean causedByPlayer, CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("enchantments_from_mod") || getWorld().getServer() == null || getWorld().getServer().getReloadableRegistries() == null) return;
        LootTable lootTable = getWorld().getServer().getReloadableRegistries().getLootTable(RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(MobAITweaks.MOD_ID, getType().toString().replace(".", "/"))));
        LootContextParameterSet.Builder builder = new LootContextParameterSet.Builder((ServerWorld)getWorld()).add(LootContextParameters.THIS_ENTITY, this).add(LootContextParameters.ORIGIN, getPos()).add(LootContextParameters.DAMAGE_SOURCE, damageSource).addOptional(LootContextParameters.ATTACKING_ENTITY, damageSource.getAttacker()).addOptional(LootContextParameters.DIRECT_ATTACKING_ENTITY, damageSource.getSource());
        if(causedByPlayer && attackingPlayer != null) builder = builder.add(LootContextParameters.LAST_DAMAGE_PLAYER, attackingPlayer).luck(attackingPlayer.getLuck());
        LootContextParameterSet lootContextParameterSet = builder.build(LootContextTypes.ENTITY);
        lootTable.generateLoot(lootContextParameterSet, getLootTableSeed(), this::dropStack);
    }
}

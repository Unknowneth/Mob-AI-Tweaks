package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DrownedEntity.class, priority = 999)
public abstract class DrownedEntityMixin extends ZombieEntity implements RangedAttackMob, SpecialAttacksInterface {
    @Unique private int lastThrownTridentId = -1;
    DrownedEntityMixin(EntityType<? extends DrownedEntity> type, World world) {
        super(type, world);
    }
    @Override public Box getBoundingBox(EntityPose pose) {
        if(isSwimming()) {
            double x = (getBoundingBox().minY + getBoundingBox().maxY) * 0.5;
            return new Box(-x, 0.6, -x, x, 1.0, x).expand(getScale() * getScaleFactor());
        }
        return super.getBoundingBox(pose);
    }
    @Override protected Box getHitbox() {
        if(isSwimming()) {
            double x = (super.getHitbox().minY + super.getHitbox().maxY) * 0.5;
            return new Box(-x, 0.6, -x, x, 1.0, x).expand(getScale() * getScaleFactor());
        }
        return super.getHitbox();
    }
    @Inject(method = "updateSwimming", at = @At("TAIL"))
    private void trySwimming(CallbackInfo ci) {
        if(isSwimming() && getTarget() != null && distanceTo(getTarget()) < 2 && !getTarget().isSubmergedInWater() && isSubmergedInWater()) {
            Vec3d swimToLook = getRotationVector().multiply(getAttributeValue(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY)).multiply(0.12);
            addVelocity(swimToLook.getX(), 0.06, swimToLook.getZ());
        }
    }
    @ModifyReturnValue(method = "isInSwimmingPose", at = @At("TAIL"))
    private boolean swim(boolean original) {
        if(!isUsingItem() && original) setPose(EntityPose.SWIMMING);
        else setPose(EntityPose.STANDING);
        return original;
    }
    @ModifyReturnValue(method = "createDrownedAttributes", at = @At("TAIL"))
    private static DefaultAttributeContainer.Builder attribution(DefaultAttributeContainer.Builder original) {
        return original.add(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY, 0.5d);
    }
    @ModifyArg(method = "shootAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/TridentEntity;<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V"), index = 2)
    private ItemStack enchantTrident(ItemStack stack) {
        if(MobAITweaks.getModConfigValue("drowned_rework") && !FabricLoader.getInstance().isModLoaded("fintastic") && getActiveItem() != null) return getActiveItem();
        return stack;
    }
    @Inject(method = "shootAt", at = @At("TAIL"))
    private void trident(LivingEntity target, float pullProgress, CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("drowned_rework") || FabricLoader.getInstance().isModLoaded("fintastic")) return;
        equipStack(getActiveHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        stopUsingItem();
    }
    @SuppressWarnings("all")
    @Override public void setSpecialCooldown(int i) {
        lastThrownTridentId = i;
    }
    @SuppressWarnings("all")
    @Override public int getSpecialCooldown() {
        return lastThrownTridentId;
    }
    @SuppressWarnings("all")
    @Override public String attackType() {
        return "TRIDENT";
    }
}

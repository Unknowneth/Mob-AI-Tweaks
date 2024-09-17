package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import com.notunanancyowen.goals.SkeletonSpecificGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.StrayEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StrayEntity.class)
public abstract class StrayEntityMixin extends AbstractSkeletonEntity {
    StrayEntityMixin(EntityType<? extends StrayEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "createArrowProjectile", at = @At("RETURN"))
    private void arrowFix(ItemStack arrow, float damageModifier, CallbackInfoReturnable<PersistentProjectileEntity> cir) {
        cir.getReturnValue().setCritical((!(getControllingVehicle() != null ? getControllingVehicle().isOnGround() : isOnGround()) && fallDistance < 1f && hurtTime == 0) || cir.getReturnValue().isCritical());
    }
    @Override protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        super.initEquipment(random, localDifficulty);
        if(random.nextInt(8) == 4 && localDifficulty.getLocalDifficulty() > 1.5f) equipStack(EquipmentSlot.MAINHAND, new ItemStack(MobAITweaks.getRandomBow(random), 1));
    }
    @Override protected void initGoals() {
        super.initGoals();
        if(MobAITweaks.getModConfigValue("stray_special_attacks")) goalSelector.add(9, new SkeletonSpecificGoal(this, 120));
    }
}

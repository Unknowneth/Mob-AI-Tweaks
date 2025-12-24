package com.notunanancyowen.mait.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin extends RangedWeaponItem {
    @Unique private int autoReloadTime = 0;
    public CrossbowItemMixin(Settings settings) {
        super(settings);
    }
    @Inject(method = "createArrowEntity", at = @At("RETURN"), cancellable = true)
    private void replaceArrowWithSomethingElse(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack projectileStack, boolean critical, CallbackInfoReturnable<ProjectileEntity> cir) {
        if(cir.getReturnValue() != null) weaponStack.getEnchantments().getEnchantments().forEach(e -> {
            if(e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "withering_munitions"))) {
                WitherSkullEntity skull = new WitherSkullEntity(world, shooter, cir.getReturnValue().getVelocity());
                if(shooter instanceof MobEntity mob && mob.getTarget() != null) skull.setVelocity(mob.getTarget().getEyePos().subtract(cir.getReturnValue().getPos()).normalize().multiply(skull.getVelocity().length()));
                skull.setFireTicks(cir.getReturnValue().getFireTicks());
                skull.setPosition(cir.getReturnValue().getPos());
                skull.setCharged(!projectileStack.isOf(Items.ARROW));
                cir.setReturnValue(skull);
                return;
            }
            if(e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "featherweight_munitions"))) {
                cir.getReturnValue().setNoGravity(true);
                if(shooter instanceof MobEntity mob && mob.getTarget() != null) cir.getReturnValue().setVelocity(mob.getTarget().getEyePos().subtract(cir.getReturnValue().getPos()).normalize().multiply(cir.getReturnValue().getVelocity().length()));
            }
        });
    }
    @ModifyVariable(method = "shoot", at = @At("HEAD"), index = 7, argsOnly = true)
    private LivingEntity forceNoTarget(LivingEntity value, @Local(index = 2, argsOnly = true) ProjectileEntity projectile) {
        if(projectile instanceof WitherSkullEntity || projectile.hasNoGravity()) return null;
        return value;
    }
    @Inject(method = "usageTick", at = @At("TAIL"))
    private void fullAuto(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if(world.isClient() || CrossbowItem.isCharged(stack) || stack.getEnchantments().getEnchantments().stream().noneMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "full_auto_retrofit")))) return;
        onStoppedUsing(stack, world, user, remainingUseTicks);
        if(CrossbowItem.getPullTime(stack, user) >= getMaxUseTime(stack, user) && CrossbowItem.isCharged(stack)) user.clearActiveItem();
    }
    @Inject(method = "loadProjectiles", at = @At("HEAD"))
    private static void fullAuto(LivingEntity shooter, ItemStack crossbow, CallbackInfoReturnable<Boolean> cir) {
        if(crossbow.getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "full_auto_retrofit"))) && shooter.isUsingItem()) shooter.clearActiveItem();
    }
    @Override public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if(entity instanceof PlayerEntity player && !world.isClient() && !selected && !CrossbowItem.isCharged(stack) && stack.getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "auto_loading_holster")))) if(++autoReloadTime > stack.getMaxUseTime(player)) stack.onStoppedUsing(world, player, autoReloadTime = 0);
        else stack.usageTick(world, player, autoReloadTime);
        super.inventoryTick(stack, world, entity, slot, selected);
    }
}

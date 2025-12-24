package com.notunanancyowen.mait.goals;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.EnumSet;
import java.util.function.Supplier;

public class RangedGunAttackGoal extends Goal {
    private final HostileEntity mob;
    private final int range;
    private final double speed;
    private final boolean noStrafe;
    private int cooldown = 0;
    private int ammoCount = 0;
    private boolean strafeLeft = false;
    public RangedGunAttackGoal(HostileEntity mob) {
        this.mob = mob;
        this.range = 16;
        this.speed = 1d;
        this.noStrafe = false;
        setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    public RangedGunAttackGoal(HostileEntity mob, int range, double speed, boolean noStrafe) {
        this.mob = mob;
        this.range = range;
        this.speed = speed;
        this.noStrafe = noStrafe;
        setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    private int holdingGunType() {
        if(Registries.ITEM.getId(mob.getMainHandStack().getItem()).getNamespace().contains("musketmod") || Registries.ITEM.getId(mob.getOffHandStack().getItem()).getNamespace().contains("musketmod")) return 0;
        if(mob.getMainHandStack().getItem().getClass().getSimpleName().contains("Gun") || mob.getMainHandStack().getItem().getClass().getSuperclass().getSimpleName().contains("Gun")) return 1;
        if(mob.getOffHandStack().getItem().getClass().getSimpleName().contains("Gun") || mob.getOffHandStack().getItem().getClass().getSuperclass().getSimpleName().contains("Gun")) return -1;
        return 0;
    }
    @Override public boolean canStart() {
        return mob.getTarget() != null && holdingGunType() != 0;
    }
    @Override public boolean shouldContinue() {
        return canStart();
    }
    @Override public boolean shouldRunEveryTick() {
        return true;
    }
    @Override public void start() {
        strafeLeft = mob.getRandom().nextBoolean();
        mob.setAttacking(true);
        super.start();
    }
    @Override public void tick() {
        if(cooldown > 0) if(--cooldown == 0) strafeLeft = mob.getRandom().nextBoolean();
        int holdingGunType = holdingGunType();
        if(mob.getTarget() != null && holdingGunType != 0) {
            if(mob.distanceTo(mob.getTarget()) < range) {
                mob.getNavigation().stop();
                float strafeSpeed = MobAITweaks.getModConfigValue("skeleton_strafe_speed_buff") ? 1F : 0.5F;
                mob.getMoveControl().strafeTo(mob.distanceTo(mob.getTarget()) < 10 ? -strafeSpeed : strafeSpeed, noStrafe ? 0f : strafeLeft ? -strafeSpeed : strafeSpeed);
            }
            else mob.getNavigation().startMovingTo(mob.getTarget(), speed);
            mob.getLookControl().lookAt(mob.getTarget(), 60F, 60F);
            mob.lookAtEntity(mob.getTarget(), 60F, 60F);
            mob.setSprinting(false);
            ItemStack weaponUsed = holdingGunType > 0 ? mob.getMainHandStack() : mob.getOffHandStack();
            Identifier id = Registries.ITEM.getId(weaponUsed.getItem());
            if(id.getNamespace().contains("tacz")) {
                if(cooldown <= 0 && mob.age % 5 == 0) {
                    try {
                        var c = mob.getMainHandStack().getItem().getClass();
                        if(c.getSuperclass().getInterfaces()[0].getDeclaredMethod("getCurrentAmmoCount", ItemStack.class).invoke(mob.getMainHandStack().getItem(), mob.getMainHandStack()) instanceof Integer i && i > 0) {
                            if(ammoCount == 0) ammoCount = i;
                            c.getDeclaredMethod("shoot", ItemStack.class, Supplier.class, Supplier.class, boolean.class, LivingEntity.class).invoke(weaponUsed.getItem(), weaponUsed, (Supplier<Float>) () -> mob.prevPitch, (Supplier<Float>) () -> mob.prevYaw, true, mob);
                            mob.swingHand(holdingGunType > 0 ? Hand.MAIN_HAND : Hand.OFF_HAND);
                        }
                        else {
                            c.getDeclaredMethod("reloadAmmo", ItemStack.class, int.class, boolean.class).invoke(weaponUsed.getItem(), weaponUsed, ammoCount, true);
                            cooldown = (ammoCount == 2 ? 60 : ammoCount == 1 ? 100 : 40);
                            ammoCount = 0;
                        }
                        return;
                    }
                    catch (Throwable ignore) {
                    }//take 2, if ShooterData is needed for newer versions of tacz
                    try {
                        var c = mob.getMainHandStack().getItem().getClass();
                        var dataHolder = LivingEntity.class.getDeclaredMethod("getDataHolder").invoke(mob);
                        if(c.getSuperclass().getInterfaces()[0].getDeclaredMethod("getCurrentAmmoCount", ItemStack.class).invoke(mob.getMainHandStack().getItem(), mob.getMainHandStack()) instanceof Integer i && i > 0) {
                            if(ammoCount == 0) ammoCount = i;
                            c.getDeclaredMethod("shoot", dataHolder.getClass(), ItemStack.class, Supplier.class, Supplier.class, LivingEntity.class).invoke(weaponUsed.getItem(), dataHolder, weaponUsed, (Supplier<Float>) () -> mob.prevPitch, (Supplier<Float>) () -> mob.prevYaw, mob);
                            mob.swingHand(holdingGunType > 0 ? Hand.MAIN_HAND : Hand.OFF_HAND);
                        }
                        else {
                            c.getSuperclass().getInterfaces()[0].getDeclaredMethod("setCurrentAmmoCount", ItemStack.class, int.class).invoke(weaponUsed.getItem(), weaponUsed, ammoCount);
                            cooldown = (ammoCount == 2 ? 60 : ammoCount == 1 ? 100 : 40);
                            ammoCount = 0;
                        }
                    }
                    catch (Throwable ignore) {
                    }
                    return;
                }
                return;
            }
            if(cooldown > 0) return;
            if(id.getNamespace().contains("gunswithoutroses")) for(int i = 0; i < (id.getPath().contains("shotgun") ? 4 : 1); i++) if(Registries.ENTITY_TYPE.get(Identifier.of(id.getNamespace(), "bullet")).create(mob.getWorld()) instanceof ProjectileEntity projectile) {
                projectile.setOwner(mob);
                var shootTo = mob.getTarget().getEyePos().subtract(mob.getEyePos()).normalize().multiply(id.getPath().contains("sniper") ? 3d : 2d);
                if(id.getPath().contains("shotgun")) projectile.setVelocity(shootTo.getX(), shootTo.getY(), shootTo.getZ(), 1F, 3F);
                else projectile.setVelocity(shootTo);
                if(id.getPath().contains("blaze")) projectile.setFireTicks(300);
                projectile.setPosition(mob.getEyePos());
                mob.getWorld().spawnEntity(projectile);
            }
            if(id.getNamespace().contains("simple_vanilla_guns")) if(Registries.ENTITY_TYPE.get(Identifier.of(id.getNamespace(), "projectile_bullet_projectile")).create(mob.getWorld()) instanceof ProjectileEntity projectile) {
                projectile.setOwner(mob);
                projectile.setVelocity(mob.getTarget().getEyePos().subtract(mob.getEyePos()).normalize().multiply(2d));
                projectile.setPosition(mob.getEyePos());
                mob.getWorld().spawnEntity(projectile);
            }
            cooldown = (7 - mob.getWorld().getDifficulty().getId()) * 10;
        }
    }
    @Override public void stop() {
        mob.setAttacking(false);
        if(holdingGunType() != 0 && mob.getTarget() == null) {
            ItemStack weaponUsed = holdingGunType() > 0 ? mob.getMainHandStack() : mob.getOffHandStack();
            Identifier id = Registries.ITEM.getId(weaponUsed.getItem());
            if(id.getNamespace().contains("tacz")) try {
                if(ammoCount > 0) mob.getMainHandStack().getItem().getClass().getDeclaredMethod("reloadAmmo", ItemStack.class, int.class, boolean.class).invoke(weaponUsed.getItem(), weaponUsed, ammoCount, true);
            }
            catch (Throwable ignore) {
            }
            cooldown = 0;
            ammoCount = 0;
        }
        super.stop();
    }
}

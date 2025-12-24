package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin extends PersistentProjectileEntity {
    @Unique private static final TrackedData<Boolean> DESPISE = DataTracker.registerData(TridentEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private int targetId = -1;
    @Shadow private boolean dealtDamage;
    @Shadow public abstract ItemStack getWeaponStack();
    TridentEntityMixin(EntityType<? extends PersistentProjectileEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
    private void includeDespiseEnchantment1(World world, LivingEntity owner, ItemStack stack, CallbackInfo ci) {
        boolean hasHoming = stack.getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "despise")));
        getDataTracker().set(DESPISE, hasHoming);
        if(hasHoming && getOwner() instanceof PlayerEntity player) {
            List<PathAwareEntity> list = getWorld().getEntitiesByClass(PathAwareEntity.class, player.getBoundingBox().expand(320.0, 320.0, 320.0), h -> {
                Vec3d vec3d = player.getRotationVector();
                Vec3d vec3d2 = new Vec3d(h.getX() - player.getX(), h.getEyeY() - player.getEyeY(), h.getZ() - player.getZ());
                double d = vec3d2.length();
                vec3d2 = vec3d2.normalize();
                double e = vec3d.dotProduct(vec3d2);
                return e > 1.0 - 0.025 / d && player.canSee(h);
            });
            double d = 320d;
            if(!list.isEmpty()) for(PathAwareEntity m : list) if(distanceTo(player) < d) {
                d = distanceTo(m);
                targetId = m.getId();
            }
        }
        else if(owner instanceof PathAwareEntity mob && mob.getTarget() != null) targetId = mob.getTarget().getId();
    }
    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
    private void includeDespiseEnchantment2(World world, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        getDataTracker().set(DESPISE, stack.getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "despise"))));
    }
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void trackData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(DESPISE, false);
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void passOwnership(CallbackInfo ci) {
        if(getDataTracker().get(DESPISE)) if(!dealtDamage && !inGround && getWorld().getEntityById(targetId) instanceof LivingEntity target) {
            addVelocity(target.getEyePos().subtract(getPos()).normalize().multiply(0.34d));
            setVelocity(getVelocity().multiply(0.86d));
            setGlowing(true);
        }
        else setGlowing(false);
        if(dealtDamage && getOwner() instanceof DrownedEntity drowned) if(drowned.isDead()) {
            pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
            setOwner(drowned.getTarget() != null ? drowned.getTarget() : drowned.getAttacker() != null ? drowned.getAttacker() : getWorld().getEntityById(targetId));
            if(getWeaponStack() != null) getWeaponStack().setDamage(getRandom().nextInt(getWeaponStack().getMaxDamage() / 4 * 3));
        }
        else if(drowned instanceof SpecialAttacksInterface special) special.setSpecialCooldown(getId());
    }
    @Override public boolean hasNoGravity() {
        return super.hasNoGravity() || (getDataTracker().get(DESPISE) && !dealtDamage && !inGround && getWorld().getEntityById(targetId) instanceof LivingEntity);
    }
}

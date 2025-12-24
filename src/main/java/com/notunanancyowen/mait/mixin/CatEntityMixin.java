package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;

@Mixin(CatEntity.class)
public abstract class CatEntityMixin extends TameableEntity {
    CatEntityMixin(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
    }
    @Unique private boolean intercept = false;
    @Inject(method = "initGoals", at = @At("HEAD"))
    private void avoidIncomingFire(CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("cats_avoid_projectiles")) return;
        Goal avoidOrInterceptProjectiles = new Goal() {
            private ProjectileEntity projectileToAvoid;
            @Override public boolean canStart() {
                double fleeDistance = 8;
                for(ProjectileEntity p : getWorld().getEntitiesByClass(ProjectileEntity.class, getBoundingBox().expand(fleeDistance, fleeDistance, fleeDistance), p -> (p.getX() != p.prevX || p.getY() != p.prevY || p.getZ() != p.prevZ) && distanceTo(p) < fleeDistance)) {
                    projectileToAvoid = p;
                    return true;
                }
                return false;
            }
            @Override public void start() {
                intercept = true;
                getJumpControl().setActive();
                Vec3d dodge = getPos().multiply(1, 0, 1).subtract(projectileToAvoid.getPos().multiply(1, 0, 1));
                setVelocity(dodge.normalize().multiply(projectileToAvoid.getVelocity().length() * 0.5F));
                getNavigation().startMovingAlong(getNavigation().findPathTo(dodge.getX() * getRandom().nextBetween(2, 6) + getX(), getY(), dodge.getZ() * getRandom().nextBetween(2, 6) + getZ(), 0), 1.2);
            }
            @Override public void tick() {
                getLookControl().lookAt(projectileToAvoid);
                lookAtEntity(projectileToAvoid, 30F, 30F);
            }
            @Override public void stop() {
                intercept = false;
                projectileToAvoid = null;
            }
        };
        avoidOrInterceptProjectiles.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK));
        goalSelector.add(0, avoidOrInterceptProjectiles);
    }
    @Override public boolean damage(DamageSource source, float amount) {
        if(intercept && !getWorld().isClient) if(source.getSource() instanceof FlyingItemEntity i) {
            var item = dropItem(i.getStack().getItem());
            if(item != null) item.setPosition(source.getSource().getPos());
            emitGameEvent(GameEvent.ENTITY_PLACE);
            source.getSource().discard();
            intercept = false;
            return false;
        }
        return super.damage(source, amount);
    }
}

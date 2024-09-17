package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;

@Mixin(PhantomEntity.class)
public abstract class PhantomEntityMixin extends FlyingEntity implements Monster {
    @Shadow public abstract void setPhantomSize(int size);
    @Shadow public abstract int getPhantomSize();
    PhantomEntityMixin(EntityType<? extends PhantomEntity> type, World world) {
        super(type, world);
    }
    @Override public boolean damage(DamageSource source, float amount) {
        if(MobAITweaks.getModConfigValue("phantom_rework")) if(source.isIn(DamageTypeTags.IS_PROJECTILE)) if(amount > 0f) amount *= 3f;
        else amount += 3f;
        return super.damage(source, amount);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void reworkTargeting(CallbackInfo ci) {
        if(!MobAITweaks.getModConfigValue("phantom_rework")) return;
        targetSelector.getGoals().clear();
        targetSelector.add(0, new Goal() {
            private int delay = toGoalTicks(20);
            @Override public boolean canStart() {
                if (delay > 0) {
                    delay--;
                    return false;
                }
                else if(getAttacker() != null) {
                    setTarget(getAttacker());
                    return true;
                }
                else if(getPhantomSize() > 3) {
                    delay = toGoalTicks(60);
                    List<PlayerEntity> list = getWorld().getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(16.0, 64.0, 16.0), p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && distanceTo(p) < 64);
                    if (list.isEmpty()) return false;
                    list.sort(Comparator.comparing(Entity::getY).reversed());
                    for (PlayerEntity player : list) if (isTarget(player, TargetPredicate.DEFAULT)) {
                        setTarget(player);
                        return true;
                    }
                    return false;
                }
                else {
                    delay = toGoalTicks(60);
                    List<AnimalEntity> list = getWorld().getEntitiesByClass(AnimalEntity.class, getBoundingBox().expand(16.0, 64.0, 16.0), p -> distanceTo(p) < 64);
                    if (list.isEmpty()) return false;
                    list.sort(Comparator.comparing(Entity::getY).reversed());
                    for (AnimalEntity animal : list) if(animal instanceof CatEntity) return false;
                    else if (isTarget(animal, TargetPredicate.DEFAULT) && !animal.isBaby()) {
                        setTarget(animal);
                        return true;
                    }
                    return false;
                }
            }
            @Override public boolean shouldContinue() {
                return getTarget() != null && isTarget(getTarget(), TargetPredicate.DEFAULT);
            }
        });
    }
    @Override public boolean onKilledOther(ServerWorld world, LivingEntity other) {
        if(MobAITweaks.getModConfigValue("phantom_rework") && other instanceof AnimalEntity) setPhantomSize(getPhantomSize() + 1);
        return super.onKilledOther(world, other);
    }
}

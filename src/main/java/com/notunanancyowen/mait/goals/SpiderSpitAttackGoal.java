package com.notunanancyowen.mait.goals;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.projectile.LlamaSpitEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;

import java.util.EnumSet;

public class SpiderSpitAttackGoal extends Goal {
    private final SpiderEntity mob;
    private boolean movingLeft = false;
    private int cooldown = 0;
    public SpiderSpitAttackGoal(SpiderEntity mob) {
        this.mob = mob;
        setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    @Override public boolean shouldRunEveryTick() {
        return true;
    }
    @Override public boolean canStart() {
        if(!mob.getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) || mob.getTarget() == null) return false;
        return ++cooldown >= 70 && cooldown < 100;
    }
    @Override public void tick() {
        mob.getNavigation().stop();
        if(mob.getTarget() == null) return;
        if(mob.getTarget().getEyeY() >= mob.getY()) {
            mob.getMoveControl().strafeTo(mob.getTarget().distanceTo(mob) < 8 ? -1F : 1F, movingLeft ? -1F : 1F);
            mob.lookAtEntity(mob.getTarget(), 60F, 60F);
        }
        mob.getLookControl().lookAt(mob.getTarget(), 60F, 60F);
    }
    @Override public void start() {
        mob.getNavigation().stop();
        movingLeft = mob.getRandom().nextBoolean();
    }
    @Override public void stop() {
        LlamaSpitEntity spit = new LlamaSpitEntity(EntityType.LLAMA_SPIT, mob.getWorld()) {
            @Override protected void onBlockHit(BlockHitResult blockHitResult) {
                if(!getWorld().isClient() && getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                    BlockPos pos = blockHitResult.getBlockPos().offset(blockHitResult.getSide());
                    if(getWorld().isAir(pos)) getWorld().setBlockState(pos, Blocks.COBWEB.getDefaultState());
                }
                super.onBlockHit(blockHitResult);
            }
            @Override protected void onEntityHit(EntityHitResult entityHitResult) {
                super.onEntityHit(entityHitResult);
                if(!getWorld().isClient()) {
                    if(getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                        BlockPos pos = entityHitResult.getEntity().getBlockPos();
                        if(getWorld().isAir(pos)) getWorld().setBlockState(pos, Blocks.COBWEB.getDefaultState());
                    }
                    else if(entityHitResult.getEntity() instanceof LivingEntity hitEntity) hitEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 5), mob);
                    discard();
                }
            }
        };
        spit.setPosition(mob.getEyePos());
        spit.setOwner(mob);
        if(mob.getTarget() instanceof LivingEntity target) {
            boolean ominous = MobAITweaks.isOminous(target);
            spit.setVelocity(target.getEyePos().add(target.getVelocity()).subtract(spit.getPos()).normalize().multiply(ominous ? 3d : 2d));
            if(ominous) spit.setNoGravity(true);
        }
        else spit.setVelocity(mob.getRotationVector().multiply(2d));
        mob.getWorld().spawnEntity(spit);
        mob.getMoveControl().strafeTo(0F, 0F);
        mob.setSidewaysSpeed(0F);
        cooldown = 0;
    }
}

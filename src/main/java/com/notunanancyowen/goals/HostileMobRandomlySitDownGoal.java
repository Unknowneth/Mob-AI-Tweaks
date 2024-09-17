package com.notunanancyowen.goals;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;


public class HostileMobRandomlySitDownGoal extends Goal {
    private final HostileEntity mob;
    public HostileMobRandomlySitDownGoal(HostileEntity mob) {
        this.mob = mob;
    }
    @Override public boolean canStart() {
        return mob.getRandom().nextInt(150) == 15 && mob.isOnGround() && mob.getAttacker() == null && mob.getTarget() == null && mob.getVehicle() == null && !mob.getNavigation().isFollowingPath() && !mob.getMoveControl().isMoving() && !mob.getLookControl().isLookingAtSpecificPosition();
    }
    @Override public boolean shouldContinue() {
        return false;
    }
    @Override public void start() {
        AreaEffectCloudEntity comfySpot = new AreaEffectCloudEntity(mob.getWorld(), mob.getX(), mob.getY() - 0.4d, mob.getZ()) {
            @Override public void tick() {
                if(getOwner() instanceof HostileEntity owner && (owner.getVehicle() != this || owner.getTarget() != null || owner.getAttacker() != null)) {
                    if(owner.getAttacker() != null && !owner.getAttacker().isInvulnerable()) owner.setTarget(owner.getAttacker());
                    discard();
                    return;
                }
                super.tick();
                if(age > 60 && getOwner() instanceof HostileEntity owner) {
                    owner.setPitch(60F);
                    if(age % 10 == 0) owner.heal(1F);
                }
                if(getOwner() != null) if(getWorld().getBlockState(getOwner().getBlockPos().down()).isAir() || !getWorld().getBlockState(getOwner().getBlockPos().up()).isAir()) discard();
            }
            @Override public void remove(RemovalReason reason) {
                if(getOwner() instanceof HostileEntity owner) {
                    owner.stopRiding();
                    owner.refreshPositionAndAngles(owner.getX(), getBlockY() + 1, owner.getZ(), owner.getYaw(), 0F);
                }
                super.remove(reason);
            }
            @Override public Text getName() {
                return mob.getName();
            }
            @Override public Text getDisplayName() {
                return mob.getDisplayName();
            }
            @Override public Text getCustomName() {
                return mob.getCustomName();
            }
            @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
            }
            @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
            }
        };
        comfySpot.setOwner(mob);
        comfySpot.addCommandTag(MobAITweaks.MOD_ID + ":" + mob.getName().getString() + "'s seat");
        comfySpot.setDuration(mob.getRandom().nextBetween(200, 500));
        comfySpot.setRadius(0);
        mob.getWorld().spawnEntity(comfySpot);
        mob.startRiding(comfySpot, true);
    }
}

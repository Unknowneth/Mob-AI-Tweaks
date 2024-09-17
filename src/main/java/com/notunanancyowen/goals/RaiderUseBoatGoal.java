package com.notunanancyowen.goals;

import com.notunanancyowen.dataholders.BoatFunctionsAccessor;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.BoatItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.Vec3d;

public class RaiderUseBoatGoal extends Goal {
    private final RaiderEntity mob;
    private int randomMoveTimer = 0;
    private boolean canSpawnBoatAgain = false;
    public RaiderUseBoatGoal(RaiderEntity mob) {
        this.mob = mob;
    }
    @Override public boolean canStart() {
        boolean shouldStart = mob.getControllingVehicle() instanceof BoatEntity;
        if(mob instanceof InventoryOwner inventory && !shouldStart) {
            boolean isInWater = mob.getWorld().getBlockState(mob.supportingBlockPos.orElse(mob.getBlockPos())).getFluidState().isIn(FluidTags.WATER);
            if(!canSpawnBoatAgain) canSpawnBoatAgain = !isInWater && mob.isOnGround();
            else if(isInWater) for(int i = 0; i < inventory.getInventory().size(); i++) if(inventory.getInventory().getStack(i).getItem() instanceof BoatItem boat) {
                BoatEntity boat2 = new BoatEntity(mob.getWorld(), mob.getX(), mob.isOnGround() ? mob.getY() + 1 : mob.getEyeY(), mob.getZ());
                boat2.setYaw(mob.getYaw());
                try {
                    var field = boat.getClass().getDeclaredField("type");
                    field.setAccessible(true);
                    boat2.setVariant((BoatEntity.Type)field.get(boat));
                }
                catch (NoSuchFieldException | IllegalAccessException | NullPointerException e) {
                    boat2.setVariant(BoatEntity.Type.DARK_OAK);
                }
                mob.getWorld().spawnEntity(boat2);
                mob.startRiding(boat2);
                inventory.getInventory().setStack(i, ItemStack.EMPTY);
                canSpawnBoatAgain = false;
                return true;
            }
        }
        return shouldStart;
    }
    @Override public void tick() {
        if(mob.getControllingVehicle() instanceof BoatEntity boat && boat instanceof BoatFunctionsAccessor paddles) {
            boolean movingBoat = mob.getNavigation().isFollowingPath() || mob.getTarget() != null || randomMoveTimer > 0;
            if(mob.getRaid() != null) {
                movingBoat = true;
                Vec3d toVillage = mob.getRaid().getCenter().toCenterPos().subtract(boat.getPos());
                boat.setYaw((float)Math.toDegrees(Math.atan2(toVillage.z, toVillage.x)));
            }
            boolean moveLeftPaddle = !movingBoat && mob.getHeadYaw() < mob.getYaw();
            boolean moveRightPaddle = !movingBoat && mob.getHeadYaw() > mob.getYaw();
            if(movingBoat) boat.setYaw(mob.getHeadYaw());
            else if(mob.getRandom().nextInt(30) == 15 && randomMoveTimer == 0) randomMoveTimer = mob.getRandom().nextBetween(20, 80);
            if(randomMoveTimer > 0) randomMoveTimer--;
            boat.setInputs(moveLeftPaddle, moveRightPaddle, movingBoat, false);
            paddles.forceUpdatePaddles(movingBoat, movingBoat);
            if(mob.getTarget() != null && boat.horizontalCollision) {
                mob.dismountVehicle();
                if(mob instanceof InventoryOwner inventory) {
                    inventory.getInventory().addStack(boat.asItem().getDefaultStack());
                    boat.discard();
                }
                if(canSpawnBoatAgain) canSpawnBoatAgain = false;
            }
        }
    }
}

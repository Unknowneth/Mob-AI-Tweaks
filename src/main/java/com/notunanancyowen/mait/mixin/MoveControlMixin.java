package com.notunanancyowen.mait.mixin;


import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@SuppressWarnings("all")
@Mixin(MoveControl.class)
public abstract class MoveControlMixin {
    @Shadow @Final protected MobEntity entity;
    @Shadow protected float sidewaysMovement;
    @Shadow protected float forwardMovement;
    @Shadow protected double speed;
    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0F, ordinal = 0))
    private float ignoreBlockedPathWhenStrafing(float constant) {
        return sidewaysMovement;
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;setSidewaysSpeed(F)V", shift = At.Shift.AFTER), cancellable = true)
    private void strafeMovementFix(CallbackInfo ci) {
        if(!entity.isOnGround() || !entity.horizontalCollision || entity.getTarget() == null) return;
        BlockState blockState = entity.getWorld().getBlockState(entity.getBlockPos());
        if(!blockState.isIn(BlockTags.DOORS) && !blockState.isIn(BlockTags.FENCES)) {
            entity.getJumpControl().setActive();
            try {
                var f = MoveControl.class;
                for(var g : f.getDeclaredClasses()) if(g.isEnum()) {
                    var h = g.getEnumConstants();
                    for(var m : f.getDeclaredFields()) if(m.toString().contains("$")) {
                        m.setAccessible(true);
                        m.set(entity.getMoveControl(), Arrays.stream(h).toArray()[h.length - 1]);
                        break;
                    }
                    break;
                }
            }
            catch (Throwable ignore) {
                return;
            }
            ci.cancel();
        }
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;setForwardSpeed(F)V", ordinal = 2))
    private void keepMovingForward(CallbackInfo ci) {
        entity.setSidewaysSpeed(0F);
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;setMovementSpeed(F)V", ordinal = 1))
    private void stopSidewaysMovement(CallbackInfo ci) {
        entity.setSidewaysSpeed(0F);
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;setMovementSpeed(F)V", ordinal = 2), cancellable = true)
    private void jumpMotionFix(CallbackInfo ci) {
        if(entity.isOnGround() || entity.getTarget() == null) return;
        forwardMovement = entity.forwardSpeed;
        float moveAmount = (float)(speed * entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
        entity.setMovementSpeed(moveAmount);
        if(forwardMovement != 0) entity.setForwardSpeed(Math.signum(forwardMovement) * moveAmount);
        entity.setSidewaysSpeed(Math.signum(sidewaysMovement) * moveAmount);
        ci.cancel();
    }
    @Inject(method = "isPosWalkable", at = @At("HEAD"), cancellable = true)
    private void walkableCheckFix(float x, float z, CallbackInfoReturnable<Boolean> cir) {
        boolean actualValue = true;
        var nav = entity.getNavigation();
        if(entity.isOnGround() && nav != null) {
            var pathNodes = nav.getNodeMaker();
            if(pathNodes != null) {
                var node = pathNodes.getDefaultNodeType(entity, BlockPos.ofFloored(entity.getX() + x, entity.getBlockY(), entity.getZ() + z));
                if((!entity.isFireImmune() && (node == PathNodeType.DANGER_FIRE || node == PathNodeType.DAMAGE_FIRE)) || node == PathNodeType.DANGER_OTHER || node == PathNodeType.DAMAGE_OTHER || node == PathNodeType.DAMAGE_CAUTIOUS) actualValue = false;
            }
        }
        cir.setReturnValue(actualValue);
    }
}

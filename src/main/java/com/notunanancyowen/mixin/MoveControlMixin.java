package com.notunanancyowen.mixin;

import com.notunanancyowen.MobAITweaks;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
    @Unique private static String theCorrectOne = "";
    @Shadow @Final protected MobEntity entity;
    @Shadow protected float sidewaysMovement;
    @Shadow protected float forwardMovement;
    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0F, ordinal = 0))
    private float ignoreBlockedPathWhenStrafing(float constant) {
        return sidewaysMovement;
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;setSidewaysSpeed(F)V", shift = At.Shift.AFTER), cancellable = true)
    private void strafeMovementFix(CallbackInfo ci) {
        if(entity.isOnGround() && entity.horizontalCollision) {
            entity.getJumpControl().setActive();
            if(!theCorrectOne.equals("none")) try {
                var f = MoveControl.class;
                for(var g : f.getDeclaredClasses()) if(g.isEnum()) {
                    var h = g.getEnumConstants();
                    if(!theCorrectOne.isBlank()) {
                        var m = f.getDeclaredField(theCorrectOne);
                        m.set(entity.getMoveControl(), Arrays.stream(h).toArray()[h.length - 1]);
                        break;
                    } //handles reflection for both mod loaders
                    try {
                        theCorrectOne = "state";
                        var m = f.getDeclaredField(theCorrectOne);
                        m.set(entity.getMoveControl(), Arrays.stream(h).toArray()[h.length - 1]);
                        MobAITweaks.LOGGER.info(theCorrectOne + " is the one!");
                        MobAITweaks.LOGGER.info("You must be on Fabric");
                        break;
                    }
                    catch (Throwable ignore) {
                        MobAITweaks.LOGGER.info(theCorrectOne + " ain't it...");
                        MobAITweaks.LOGGER.info("You are not on Fabric");
                    }
                    try {
                        theCorrectOne = "operation";
                        var m = f.getDeclaredField(theCorrectOne);
                        m.set(entity.getMoveControl(), Arrays.stream(h).toArray()[h.length - 1]);
                        MobAITweaks.LOGGER.info(theCorrectOne + " is the one!");
                        MobAITweaks.LOGGER.info("You must be on Forge");
                        break;
                    }
                    catch (Throwable ignore) {
                        MobAITweaks.LOGGER.info(theCorrectOne + " ain't it...");
                        MobAITweaks.LOGGER.info("You are not on Forge");
                    }
                    theCorrectOne = "none";
                    MobAITweaks.LOGGER.info("Reflection attempt at MoveControlMixin failed...");
                    MobAITweaks.LOGGER.info("Mod loader unknown, either that or the mappings are incorrect");
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
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;setMovementSpeed(F)V", shift = At.Shift.AFTER, ordinal = 2))
    private void jumpMotionFix(CallbackInfo ci) {
        if(forwardMovement != 0F) entity.setForwardSpeed(forwardMovement);
        entity.setSidewaysSpeed(sidewaysMovement);
    }
    @Inject(method = "isPosWalkable", at = @At("HEAD"), cancellable = true)
    private void walkableCheckFix(float x, float z, CallbackInfoReturnable<Boolean> cir) {
        boolean actualValue = true;
        var nav = entity.getNavigation();
        if(entity.isOnGround() && nav != null) {
            var pathNodes = nav.getNodeMaker();
            if(pathNodes != null) {
                var node = pathNodes.getDefaultNodeType(entity.getWorld(), MathHelper.floor(entity.getX() + x), entity.getBlockY(), MathHelper.floor(entity.getZ() + z));
                if((!entity.isFireImmune() && (node == PathNodeType.DANGER_FIRE || node == PathNodeType.DAMAGE_FIRE)) || node == PathNodeType.DANGER_OTHER || node == PathNodeType.DAMAGE_OTHER || node == PathNodeType.DAMAGE_CAUTIOUS) actualValue = false;
            }
        }
        cir.setReturnValue(actualValue);
    }
}

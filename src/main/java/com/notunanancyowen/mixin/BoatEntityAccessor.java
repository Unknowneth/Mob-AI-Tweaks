package com.notunanancyowen.mixin;

import com.notunanancyowen.dataholders.BoatFunctionsAccessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BoatEntity.class)
public abstract class BoatEntityAccessor implements BoatFunctionsAccessor {
    @Unique private boolean moveLeftPaddle = false;
    @Unique private boolean moveRightPaddle = false;
    @Shadow protected abstract void updatePaddles();
    @Shadow public abstract void setPaddleMovings(boolean leftMoving, boolean rightMoving);
    @Shadow @Nullable public abstract LivingEntity getControllingPassenger();
    @SuppressWarnings("all")
    @Override public void forceUpdatePaddles(boolean left, boolean right) {
        moveLeftPaddle = left;
        moveRightPaddle = right;
        updatePaddles();
    }
    @Inject(method = "setPaddleMovings", at = @At("HEAD"), cancellable = true)
    private void forcePaddleMovement(boolean leftMoving, boolean rightMoving, CallbackInfo ci) {
        if(getControllingPassenger() instanceof PlayerEntity) moveRightPaddle = moveLeftPaddle = false;
        else if(!leftMoving && moveLeftPaddle && !rightMoving && moveRightPaddle) {
            setPaddleMovings(true, true);
            ci.cancel();
        }
        else if(!leftMoving && moveLeftPaddle) {
            setPaddleMovings(true, false);
            ci.cancel();
        }
        else if(!rightMoving && moveRightPaddle) {
            setPaddleMovings(false, true);
            ci.cancel();
        }
    }
}

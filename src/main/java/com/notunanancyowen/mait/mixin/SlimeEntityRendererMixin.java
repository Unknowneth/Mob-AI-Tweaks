package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.client.render.entity.SlimeEntityRenderer;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SlimeEntityRenderer.class)
public class SlimeEntityRendererMixin {
    @Unique private static final Identifier[] TEXTURE = new Identifier[] {
            Identifier.of(MobAITweaks.MOD_ID, "textures/slimes/dispenser.png"),
            Identifier.of(MobAITweaks.MOD_ID, "textures/slimes/honey.png"),
            Identifier.of(MobAITweaks.MOD_ID, "textures/slimes/honey_dispenser.png")
    };
    @Inject(method = "getTexture(Lnet/minecraft/entity/mob/SlimeEntity;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    private void becomeDispenserSlime(SlimeEntity slimeEntity, CallbackInfoReturnable<Identifier> cir) {
        if(slimeEntity.getOffHandStack().isOf(Items.HONEY_BLOCK)) cir.setReturnValue(TEXTURE[slimeEntity.getMainHandStack().isOf(Items.DISPENSER) ? 2 : 1]);
        else if(slimeEntity.getMainHandStack().isOf(Items.DISPENSER)) cir.setReturnValue(TEXTURE[0]);
    }
}

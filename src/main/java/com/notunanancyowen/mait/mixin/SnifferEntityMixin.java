package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SnifferEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnifferEntity.class)
public abstract class SnifferEntityMixin extends AnimalEntity {
    @Shadow protected abstract SnifferEntity setState(SnifferEntity.State state);
    @Unique private Vec3d lastInteractionPos = Vec3d.ZERO;
    @Unique private Vec3d whereToLookTemporarily = Vec3d.ZERO;
    @Unique private Vec3d foundBlockPos = null;
    @Unique private int pointToBiomeTime = 0;
    @Unique private int foundBlockTime = 0;
    @SuppressWarnings("all") @Unique private static String previousBiomeChecked = "";
    SnifferEntityMixin(EntityType<? extends AnimalEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "mobTick", at = @At("HEAD"), cancellable = true)
    private void pointToThisSpecificDirection(CallbackInfo ci) {
        if(isPanicking()) {
            lastInteractionPos = Vec3d.ZERO;
            whereToLookTemporarily = Vec3d.ZERO;
            foundBlockPos = null;
            pointToBiomeTime = 0;
            foundBlockTime = 0;
            return;
        }
        if(pointToBiomeTime > 0) {
            getLookControl().lookAt(whereToLookTemporarily);
            lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, whereToLookTemporarily);
            Vec3d whereToLook = whereToLookTemporarily.subtract(lastInteractionPos).normalize();
            Vec3d particleSpawn = lastInteractionPos.add(0d, getHeight() + 0.5d, 0d).add(whereToLook.multiply(getRandom().nextFloat()));
            if(getWorld() instanceof ServerWorld server) for(int i = 0; i < 10; i++) {
                particleSpawn = particleSpawn.add(whereToLook.multiply(i * 0.3d));
                server.spawnParticles(ParticleTypes.HAPPY_VILLAGER, particleSpawn.x, particleSpawn.y, particleSpawn.z, 1, 0d, 0d, 0d, 0f);
            }
            pointToBiomeTime--;
            if(isOnGround()) getJumpControl().setActive();
            getNavigation().stop();
            setForwardSpeed(age % 2 == 0 ? -0.1f : 0.1f);
            ci.cancel();
        }
        else if(foundBlockPos != null) {
            if(squaredDistanceTo(foundBlockPos.x, lastInteractionPos.y, foundBlockPos.z) > 256d) {
                foundBlockPos = null;
                foundBlockTime = 0;
                return;
            }
            if(squaredDistanceTo(foundBlockPos) > getScale() * getScale() * 16d) {
                setState(SnifferEntity.State.SNIFFING);
                getNavigation().startMovingTo(foundBlockPos.x, getY() - 1, foundBlockPos.z, 1.5d);
            }
            else {
                setState(SnifferEntity.State.SCENTING);
                if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.HAPPY_VILLAGER, foundBlockPos.x, Math.min(getY(), lastInteractionPos.y), foundBlockPos.z, 2, 0.1d, 0.2d, 0.1d, 0.1f);
                if(--foundBlockTime <= 0) foundBlockPos = null;
                getLookControl().lookAt(whereToLookTemporarily);
                setForwardSpeed(age % 2 == 0 ? -0.1f : 0.1f);
                lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, whereToLookTemporarily);
            }
            ci.cancel();
        }
    }
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void interactionChanges(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if(!MobAITweaks.getModConfigValue("sniffer_rework") || !player.isSneaking() || pointToBiomeTime > 0) return;
        String s = player.getStackInHand(hand).getItem().toString();
        if(!MobAITweaks.snifferItemToBiome.isEmpty() && MobAITweaks.snifferItemToBiome.containsKey(s)) {
            s = MobAITweaks.snifferItemToBiome.get(s);
            String[] biomeName = s.split(":", 2);
            Identifier biomeId = Identifier.of(biomeName[0].contains("#") ? biomeName[0].replace("#", "") : biomeName[0], biomeName[1]);
            if(getWorld() instanceof ServerWorld server) {
                if(squaredDistanceTo(lastInteractionPos) < 144 && s.equals(previousBiomeChecked)) {
                    lastInteractionPos = getPos();
                    pointToBiomeTime = 60;
                    setState(SnifferEntity.State.SCENTING);
                    if(MobAITweaks.getModConfigValue("sniffer_announces_biome_find")) player.sendMessage(Text.translatable("mob-ai-tweaks.sniffer_found_biome", getName().getString()), false);
                }
                else {
                    var biome = server.locateBiome(b -> biomeName[0].contains("#") ? b.isIn(TagKey.of(RegistryKeys.BIOME, biomeId)) : b.matchesId(biomeId), getBlockPos(), 6400, 32, 64);
                    if(biome != null) {
                        lastInteractionPos = getPos();
                        whereToLookTemporarily = biome.getFirst().toCenterPos();
                        pointToBiomeTime = 60;
                        setState(SnifferEntity.State.SCENTING);
                    }
                    if(MobAITweaks.getModConfigValue("sniffer_announces_biome_find")) player.sendMessage(Text.translatable(biome != null ? "mob-ai-tweaks.sniffer_found_biome" : "mob-ai-tweaks.sniffer_found_no_biome", getName().getString()), false);
                }
                previousBiomeChecked = s;
            }
            cir.setReturnValue(ActionResult.SUCCESS_NO_ITEM_USED);
        }
        if(getVehicle() == null && foundBlockTime <= 0) {
            if(s.contains(":")) s = s.split(":", 2)[1];
            if(s.contains("_ingot")) s = s.replace("_ingot", "");
            if(s.contains("_ore")) s = s.replace("_ore", "");
            if(s.contains("_block")) s = s.replace("_block", "");
            if(s.contains("block_of_")) s = s.replace("block_of_", "");
            for(int i = -16; i <= 16; i++) for(int j = 0; j >= -64; j--) for(int k = -16; k <= 16; k++) {
                var blockPos = getBlockPos().add(i, j, k);
                if(getWorld().getBlockState(blockPos).getBlock().toString().replace("}", "").replace("Block{", "").contains(s)) {
                    if(foundBlockPos == null) {
                        foundBlockPos = blockPos.toCenterPos();
                        if(squaredDistanceTo(foundBlockPos.x, getY(), foundBlockPos.z) > 256d) {
                            foundBlockPos = null;
                            return;
                        }
                        cir.setReturnValue(ActionResult.SUCCESS_NO_ITEM_USED);
                        if(MobAITweaks.getModConfigValue("sniffer_announces_block_find") && !getWorld().isClient()) player.sendMessage(Text.translatable("mob-ai-tweaks.sniffer_found_block", getName().getString()), false);
                        whereToLookTemporarily = foundBlockPos;
                        lastInteractionPos = getPos();
                        foundBlockPos = foundBlockPos.add(0d, lastInteractionPos.y - foundBlockPos.y, 0d);
                        foundBlockTime = 80;
                    }
                    return;
                }
            }
        }
    }
}

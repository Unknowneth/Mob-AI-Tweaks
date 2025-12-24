package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.dataholders.SpecialAttacksInterface;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void announceBossSummon(CallbackInfo ci) {
        LivingEntity me = (LivingEntity)(Object)this;
        if(me.age == 1 && me.getType().isIn(MobAITweaks.BOSS_THAT_ANNOUNCES_SUMMON) && MobAITweaks.getModConfigValue("bosses_announce_spawn_and_death") && me.getServer() != null && me.getServer().getPlayerManager() != null) {
            boolean useTerrariaText = FabricLoader.getInstance().isModLoaded("terra_entity");
            var message = Text.translatable(useTerrariaText ? "message.terraentity.boss_spawn" : "mob-ai-tweaks.boss_summoned", me.getName().getString()).copy().formatted(Formatting.DARK_PURPLE);
            if(useTerrariaText) message.formatted(Formatting.BOLD);
            me.getServer().getPlayerManager().broadcast(message, false);
        }
        if(this instanceof Tameable t && t.getOwner() != null && !me.getType().isIn(MobAITweaks.NO_NATURAL_HEALTH_REGENERATION)) {
            int h = MobAITweaks.getModConfigValue("tamed_mobs_regen_amount", 1);
            int r = MobAITweaks.getModConfigValue("tamed_mobs_regen_cooldown", 20);
            if(r > 0 && h > 0 && me.age % r == 0) me.heal(h);
        }
    }
    @Inject(method = "onRemoval", at = @At("HEAD"))
    private void announceBossDeath(Entity.RemovalReason reason, CallbackInfo ci) {
        LivingEntity me = (LivingEntity)(Object)this;
        if(me.getType().isIn(MobAITweaks.BOSS_THAT_ANNOUNCES_DEFEAT) && MobAITweaks.getModConfigValue("bosses_announce_spawn_and_death") && me.getServer() != null && me.getServer().getPlayerManager() != null) {
            boolean useTerrariaText = FabricLoader.getInstance().isModLoaded("terra_entity");
            var message = Text.translatable(useTerrariaText ? (reason == Entity.RemovalReason.KILLED ? "message.terraentity.boss_leave" : "message.terraentity.boss_discard") : "mob-ai-tweaks.boss_defeated", me.getName().getString()).copy().formatted(Formatting.DARK_PURPLE);
            if(useTerrariaText) message.formatted(Formatting.BOLD);
            if(reason == Entity.RemovalReason.KILLED || useTerrariaText) me.getServer().getPlayerManager().broadcast(message, false);
        }
    }
    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void iFramesWhenDodging(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if(this instanceof SpecialAttacksInterface s && s.attackType().equals("DODGE") && s.getSpecialCooldown() != 0) cir.setReturnValue(true);
    }
    @Inject(method = "blockedByShield", at = @At("HEAD"), cancellable = true)
    private void reflectAttacks(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity me = (LivingEntity)(Object)this;
        if(me.getActiveItem() != null && me.getActiveItem().getItem() instanceof ShieldItem) me.getActiveItem().getEnchantments().getEnchantments().forEach(e -> {
            if(e.matchesId(Identifier.of(MobAITweaks.MOD_ID, "perfect_parry")) && source.getSource() instanceof ProjectileEntity p && me.isUsingItem() && me.getItemUseTime() < (me.getActiveItem().getEnchantments().getLevel(e) + 1) * 10) {
                if(p.getWorld() instanceof ServerWorld server) {
                    server.spawnParticles(ParticleTypes.GUST, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0);
                    server.spawnParticles(ParticleTypes.SMALL_GUST, p.getX(), p.getY(), p.getZ(), me.getActiveItem().getEnchantments().getLevel(e) + 1, 1, 1, 1, 1);
                    if(p.getType().create(server) instanceof ProjectileEntity p2) {
                        var uuid = p2.getUuid();
                        p2.readNbt(p.writeNbt(new NbtCompound()));
                        p2.setUuid(uuid);
                        p2.setPosition(p.getPos());
                        p2.setVelocity(me, me.getPitch(), me.getYaw(), 0F, (float)p.getVelocity().length(), 0F);
                        //This right here makes sure that if a projectile explodes, it does it in the void so it doesn't dupe the reflected attack
                        if(server.spawnNewEntityAndPassengers(p2)) p.setPos(p.getX(), -64, p.getZ());
                    }
                }
                p.discard();
                cir.setReturnValue(true);
            }
        });
    }
    @Inject(method = "damage", at = @At("TAIL"))
    private void becomeIllusioner(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if((LivingEntity)(Object)this instanceof AnimalEntity a) {
            if(a instanceof ChickenEntity c && MobAITweaks.getModConfigValue("chickens_shed_feathers") && c.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_LOOT) && c.getRandom().nextInt(100) < MobAITweaks.getModConfigValue("chickens_shed_feather_chance", 50) && !c.getWorld().isClient() && !c.isBaby()) {
                ItemEntity feathers = c.dropItem(Items.FEATHER);
                if(feathers != null) {
                    if(c.isOnFire()) feathers.setFireTicks(c.getFireTicks());
                    feathers.setVelocity(c.getRandom().nextGaussian() - 0.5d, c.getRandom().nextGaussian(), c.getRandom().nextGaussian() - 0.5d);
                }
                c.emitGameEvent(GameEvent.ENTITY_PLACE);
            }
            if(a.getRandom().nextInt(100) < MobAITweaks.getModConfigValue("illusioner_bad_omen_spawn_chance", 1) && !a.isInLove() && !(a instanceof Tameable t && t.getOwner() != null) && !(a instanceof Saddleable s && s.isSaddled()) && source.getAttacker() instanceof LivingEntity l && MobAITweaks.isOminous(l)) {
                var i = EntityType.ILLUSIONER.create(a.getWorld());
                if(i == null) return;
                if(a.getWorld() instanceof ServerWorld server) {
                    for(int j = 0; j < 8; j++) server.spawnParticles(ParticleTypes.CLOUD, a.getParticleX(0.5), a.getRandomBodyY(), a.offsetZ(0.5), 2, 0.1, 0.1, 0.1, 0.1);
                    i.initialize(server, server.getLocalDifficulty(a.getBlockPos()), SpawnReason.TRIGGERED, null);
                }
                i.refreshPositionAndAngles(a.getPos(), a.getYaw(), a.getPitch());
                a.getWorld().spawnEntity(i);
                a.discard();
            }
        }
    }
    @Inject(method = "canSee", at = @At("TAIL"), cancellable = true)
    private void canSeeGlowingMobs(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if(!MobAITweaks.getModConfigValue("line_of_sight_rework")) return;
        LivingEntity l = (LivingEntity)(Object)this;
        if(l.hasStatusEffect(StatusEffects.BLINDNESS)) cir.setReturnValue(false);
        //If you're wondering why it checks if getTarget() is null, its because they get stuck trying to get to their targets if this is always true.
        //This is because their pathfinding also uses this.
        //By setting it to check ONLY if the mob has no target, they can still run targeting code to find targets behind walls and pathfind to them normally.
        if(entity.isGlowing()) if(l instanceof MobEntity me && me.getTarget() == null) cir.setReturnValue(true);
    }
}

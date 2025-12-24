package com.notunanancyowen.mait.goals;

import com.notunanancyowen.mait.MobAITweaks;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.GameEventTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.EntityPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.Vibrations;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class HostileMobRandomlySitDownGoal extends Goal implements Vibrations {
    private final HostileEntity mob;
    private final Callback callback;
    private final ListenerData listenerData;
    private final EntityGameEventHandler<Vibrations.VibrationListener> gameEventHandler;
    public HostileMobRandomlySitDownGoal(HostileEntity mob) {
        this.mob = mob;
        callback = new Callback() {
            private final PositionSource positionSource = new EntityPositionSource(mob, mob.getStandingEyeHeight());
            @Override public int getRange() {
                return 6;
            }
            @Override public PositionSource getPositionSource() {
                return positionSource;
            }
            @Override public boolean accepts(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event, GameEvent.Emitter emitter) {
                if(event.isIn(MobAITweaks.IGNORED_BY_SLEEPING_MOBS)) return false;
                if(emitter.sourceEntity() instanceof AreaEffectCloudEntity aoe && mob.getVehicle() instanceof AreaEffectCloudEntity seat && aoe.getId() == seat.getId()) return false;
                if(emitter.sourceEntity() instanceof HostileEntity h && mob.getId() == h.getId()) return false;
                return world.getWorldBorder().contains(pos) && ((emitter.sourceEntity() instanceof PlayerEntity p && mob.canTarget(p) && !p.isSneaking() && event.isIn(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) || event.isIn(MobAITweaks.WAKES_UP_SLEEPING_MOBS)) && shouldContinue();
            }
            @Override public void accept(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event, @Nullable Entity sourceEntity, @Nullable Entity entity, float distance) {
                if(!shouldContinue() || mob.getVehicle() == null) return;
                double y = mob.getVehicle().getBlockY() + 1;
                mob.getVehicle().discard();
                mob.stopRiding();
                mob.refreshPositionAndAngles(mob.getX(), y, mob.getZ(), mob.getYaw(), 0F);
            }
        };
        listenerData = new ListenerData();
        gameEventHandler = new EntityGameEventHandler<>(new Vibrations.VibrationListener(this));
    }
    @Override public boolean canStart() {
        boolean canSit = mob.getRandom().nextInt(150) == 15 && mob.isOnGround() && mob.getAttacker() == null && mob.getTarget() == null && mob.getVehicle() == null && !mob.getNavigation().isFollowingPath() && !mob.getMoveControl().isMoving() && !mob.getLookControl().isLookingAtSpecificPosition();
        if(canSit) { //heaviest check so it's the last one checked
            var id = Registries.ENTITY_TYPE.getId(mob.getType());
            if(MobAITweaks.hostilesThatCannotSit.contains(id.getNamespace() + ":" + id.getPath())) return false;
        }
        return canSit;
    }
    @Override public boolean shouldContinue() {
        return MobAITweaks.getModConfigValue("hostile_mobs_sitting_can_be_startled") && mob.getVehicle() instanceof AreaEffectCloudEntity aoe && aoe.getCommandTags().contains(MobAITweaks.MOD_ID + ":" + mob.getName().getString() + "'s seat");
    }
    @Override public void start() {
        AreaEffectCloudEntity comfySpot = new AreaEffectCloudEntity(mob.getWorld(), mob.getX(), mob.getY() - 0.4d, mob.getZ()) {
            @Override public void updateEventHandler(BiConsumer<EntityGameEventHandler<?>, ServerWorld> callback) {
                if(getWorld() instanceof ServerWorld server && MobAITweaks.getModConfigValue("hostile_mobs_sitting_can_be_startled")) callback.accept(gameEventHandler, server);
            }
            @Override public void tick() {
                super.tick();
                if((age < 60 || age % 20 == 0) && age > 0 && MobAITweaks.getModConfigValue("hostile_mobs_sitting_can_be_startled")) HostileMobRandomlySitDownGoal.this.tick();
            }
        };
        comfySpot.setOwner(mob);
        comfySpot.addCommandTag(MobAITweaks.MOD_ID + ":" + mob.getName().getString() + "'s seat");
        comfySpot.setDuration(mob.getRandom().nextBetween(200, 500));
        comfySpot.setRadius(0);
        comfySpot.setParticleType(new BlockStateParticleEffect(ParticleTypes.BLOCK, mob.getSteppingBlockState()));
        mob.getWorld().spawnEntity(comfySpot);
        mob.startRiding(comfySpot, true);
    }
    @Override public void tick() {
        Vibrations.Ticker.tick(mob.getWorld(), getVibrationListenerData(), getVibrationCallback());
    }
    @Override public ListenerData getVibrationListenerData() {
        return listenerData;
    }
    @Override public Callback getVibrationCallback() {
        return callback;
    }
}

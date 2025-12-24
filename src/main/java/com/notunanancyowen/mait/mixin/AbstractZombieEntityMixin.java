package com.notunanancyowen.mait.mixin;

import com.notunanancyowen.mait.MobAITweaks;
import com.notunanancyowen.mait.goals.HostileMobRandomlySitDownGoal;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BannerPatterns;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.village.VillagerType;
import net.minecraft.world.GameRules;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ZombieEntity.class)
public abstract class AbstractZombieEntityMixin extends HostileEntity {
    @Shadow public abstract boolean isBaby();
    @Shadow @Final private static Identifier LEADER_ZOMBIE_BONUS_MODIFIER_ID;
    @Shadow protected abstract boolean burnsInDaylight();
    AbstractZombieEntityMixin(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addNewAttacks(CallbackInfo ci) {
        if(MobAITweaks.getModConfigValue("hostile_mobs_can_sit")) goalSelector.add(10, new HostileMobRandomlySitDownGoal(this));
        if(MobAITweaks.getModConfigValue("illagers_and_normal_zombies_fight")) targetSelector.add(!MobAITweaks.getModConfigValue("illagers_and_zombies_prioritize_player") ? 2 : 3, new ActiveTargetGoal<>(this, IllagerEntity.class, true, l -> !getType().isIn(MobAITweaks.ZOMBIES_NOT_TARGETABLE_BY_ILLAGERS)));
    }
    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/HostileEntity;tickMovement()V"), cancellable = true)
    private void disableAIWhenSitting(CallbackInfo ci) {
        if(getVehicle() instanceof AreaEffectCloudEntity aoe && aoe.getOwner() != null && aoe.getOwner().getId() == getId() && aoe.getCommandTags().contains(MobAITweaks.MOD_ID + ":" + getName().getString() + "'s seat")) {
            if(aoe.age > 60) {
                setPitch(60F);
                if(age % 10 == 0) heal(1F);
            }
            else if(aoe.age > 40) setPitch((aoe.age - 40) * 3);
            if(aoe.age == aoe.getDuration() + aoe.getWaitTime() - 1) {
                stopRiding();
                refreshPositionAndAngles(getX(), aoe.getBlockY() + 1, getZ(), getYaw(), 0F);
            }
            else if(hurtTime > 0 || (getTarget() != null && !getTarget().isSneaking() && (getTarget().getVelocity().getX() != 0F || getTarget().getVelocity().getZ() != 0F)) || getAttacker() != null || isDead() || getWorld().getBlockState(getBlockPos().down()).isAir() || !getWorld().getBlockState(getBlockPos().up()).isAir()) {
                if(getAttacker() != null && canTarget(getAttacker())) setTarget(getAttacker());
                stopRiding();
                refreshPositionAndAngles(getX(), aoe.getBlockY() + 1, getZ(), getYaw(), 0F);
                aoe.discard();
            }
            ci.cancel();
        }
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickSlimers(CallbackInfo ci) {
        if(getWorld().isClient() && getFirstPassenger() instanceof SlimeEntity s) if(getHandSwingProgress(1F) > 0F) s.targetStretch += (float)Math.sin(getHandSwingProgress(1.0F) * 2 * Math.PI) * 0.5F;
        else if(s.isAttacking()) s.targetStretch = -0.5F;
    }
    @Inject(method = "initialize", at = @At("TAIL"))
    private void onSpawned(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
        if(spawnReason == SpawnReason.NATURAL && MobAITweaks.getModConfigValue("hostile_mobs_spawn_with_effects_chance", 10) > 0 && world.getDifficulty().getId() == 3 && getRandom().nextFloat() < (MobAITweaks.getModConfigValue("hostile_mobs_spawn_with_effects_chance", 10) * 0.01F) * difficulty.getClampedLocalDifficulty()) switch(getRandom().nextInt(getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP) ? 5 : 4)) {
            case 2 -> addStatusEffect(new StatusEffectInstance(burnsInDaylight() ? StatusEffects.FIRE_RESISTANCE : StatusEffects.SPEED, -1));
            case 0 -> addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1));
            case 4 -> addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, -1));
            case 1 -> addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, -1));
            default -> addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, -1));
        }
        boolean doorEligible = getType().isIn(MobAITweaks.ZOMBIES_WITH_DOORS) && !isBaby() && !hasPassengers() && !hasVehicle() && spawnReason == SpawnReason.NATURAL;
        if(getType().isIn(MobAITweaks.ZOMBIES_WITH_SLIMES) && MobAITweaks.getModConfigValue("zombie_slimers") && !isBaby() && world.getRandom().nextInt(100) < MobAITweaks.getModConfigValue("zombie_slimer_spawn_chance", 1) && spawnReason == SpawnReason.NATURAL && difficulty.getLocalDifficulty() > MobAITweaks.getModConfigValue("zombie_slimer_local_difficulty", 150) * 0.01F) {
            SlimeEntity slime = EntityType.SLIME.create(getWorld());
            if(slime == null) return;
            slime.refreshPositionAndAngles(getPos(), getYaw(), 0F);
            slime.initialize(world, difficulty, SpawnReason.JOCKEY, null);
            slime.setSize(1, false);
            slime.startRiding(this, true);
            setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
            doorEligible = false;
        }
        if(MobAITweaks.getModConfigValue("zombie_dads_from_bedrock") && !isBaby() && world.getRandom().nextInt(100) < MobAITweaks.getModConfigValue("zombie_dads_spawn_chance", 1) && spawnReason == SpawnReason.NATURAL && difficulty.getLocalDifficulty() > MobAITweaks.getModConfigValue("zombie_dads_local_difficulty", 150) * 0.01F) if(getType() == EntityType.ZOMBIE) {
            if(world.getRandom().nextBoolean()) {
                ZombieEntity zombie = EntityType.ZOMBIE.create(getWorld());
                if(zombie == null) return;
                zombie.refreshPositionAndAngles(getPos(), getYaw(), 0F);
                zombie.setBaby(true);
                zombie.initialize(world, difficulty, SpawnReason.JOCKEY, null);
                world.spawnEntity(zombie);
                zombie.startRiding(this, true);
            }
            else {
                SkeletonEntity skeleton = EntityType.SKELETON.create(getWorld());
                if(skeleton == null) return;
                skeleton.refreshPositionAndAngles(getPos(), getYaw(), 0F);
                skeleton.setBaby(true);
                skeleton.initialize(world, difficulty, SpawnReason.JOCKEY, null);
                skeleton.tryEquip(MobAITweaks.getRandomBow(world.getRandom()).getDefaultStack());
                world.spawnEntity(skeleton);
                skeleton.startRiding(this, true);
            }
            doorEligible = false;
        }
        else if(getType() == EntityType.HUSK) {
            if(world.getRandom().nextBoolean()) {
                HuskEntity zombie = EntityType.HUSK.create(getWorld());
                if(zombie == null) return;
                zombie.refreshPositionAndAngles(getPos(), getYaw(), 0F);
                zombie.setBaby(true);
                zombie.initialize(world, difficulty, SpawnReason.JOCKEY, null);
                world.spawnEntity(zombie);
                zombie.startRiding(this, true);
            }
            else {
                BoggedEntity skeleton = EntityType.BOGGED.create(getWorld());
                if(skeleton == null) return;
                skeleton.refreshPositionAndAngles(getPos(), getYaw(), 0F);
                skeleton.setBaby(true);
                skeleton.initialize(world, difficulty, SpawnReason.JOCKEY, null);
                skeleton.tryEquip(MobAITweaks.getRandomBow(world.getRandom()).getDefaultStack());
                world.spawnEntity(skeleton);
                skeleton.startRiding(this, true);
            }
            doorEligible = false;
        }
        else if(getType() == EntityType.ZOMBIE_VILLAGER) {
            ZombieVillagerEntity zombie = EntityType.ZOMBIE_VILLAGER.create(getWorld());
            if(zombie == null) return;
            zombie.refreshPositionAndAngles(getPos(), getYaw(), 0F);
            zombie.setBaby(true);
            zombie.initialize(world, difficulty, SpawnReason.JOCKEY, null);
            zombie.tryEquip(MobAITweaks.getRandomCrossbow(world.getRandom()).getDefaultStack());
            world.spawnEntity(zombie);
            zombie.startRiding(this, true);
            doorEligible = false;
        }
        else if(getType() == EntityType.ZOMBIFIED_PIGLIN) {
            ZombifiedPiglinEntity zombie = EntityType.ZOMBIFIED_PIGLIN.create(getWorld());
            if(zombie == null) return;
            zombie.refreshPositionAndAngles(getPos(), getYaw(), 0F);
            zombie.setBaby(true);
            zombie.initialize(world, difficulty, SpawnReason.JOCKEY, null);
            zombie.tryEquip(MobAITweaks.getRandomCrossbow(world.getRandom()).getDefaultStack());
            world.spawnEntity(zombie);
            zombie.startRiding(this, true);
            doorEligible = false;
        }
        if(MobAITweaks.getModConfigValue("zombie_leader_rework") && getType() == EntityType.ZOMBIE && getAttributes().hasModifierForAttribute(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS, LEADER_ZOMBIE_BONUS_MODIFIER_ID)) {
            if(world.getRandom().nextInt(100) >= MobAITweaks.getModConfigValue("zombie_leader_spawn_chance", 10)) {
                var a = getAttributes().getCustomInstance(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS);
                if(a != null) a.removeModifier(LEADER_ZOMBIE_BONUS_MODIFIER_ID);
                var b = getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
                if(b != null) b.removeModifier(LEADER_ZOMBIE_BONUS_MODIFIER_ID);
                return;
            }
            var whateverThisIs = Items.CYAN_BANNER.getDefaultStack();
            whateverThisIs.apply(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT, patterns -> new BannerPatternsComponent.Builder().addAll(patterns).add(RegistryEntry.of((BannerPattern)world.getRegistryManager().get(RegistryKey.ofRegistry(BannerPatterns.GRADIENT_UP.getRegistry())).get(BannerPatterns.GRADIENT_UP.getValue())), ((DyeItem)Items.LIGHT_BLUE_DYE).getColor()).build());
            whateverThisIs.apply(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT, patterns -> new BannerPatternsComponent.Builder().addAll(patterns).add(RegistryEntry.of((BannerPattern)world.getRegistryManager().get(RegistryKey.ofRegistry(BannerPatterns.CURLY_BORDER.getRegistry())).get(BannerPatterns.CURLY_BORDER.getValue())), ((DyeItem)Items.BLUE_DYE).getColor()).build());
            whateverThisIs.apply(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT, patterns -> new BannerPatternsComponent.Builder().addAll(patterns).add(RegistryEntry.of((BannerPattern)world.getRegistryManager().get(RegistryKey.ofRegistry(BannerPatterns.RHOMBUS.getRegistry())).get(BannerPatterns.RHOMBUS.getValue())), ((DyeItem)Items.YELLOW_DYE).getColor()).build());
            whateverThisIs.apply(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT, patterns -> new BannerPatternsComponent.Builder().addAll(patterns).add(RegistryEntry.of((BannerPattern)world.getRegistryManager().get(RegistryKey.ofRegistry(BannerPatterns.BORDER.getRegistry())).get(BannerPatterns.BORDER.getValue())), ((DyeItem)Items.GREEN_DYE).getColor()).build());
            whateverThisIs.apply(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT, patterns -> new BannerPatternsComponent.Builder().addAll(patterns).add(RegistryEntry.of((BannerPattern)world.getRegistryManager().get(RegistryKey.ofRegistry(BannerPatterns.FLOWER.getRegistry())).get(BannerPatterns.FLOWER.getValue())), ((DyeItem)Items.PINK_DYE).getColor()).build());
            whateverThisIs.apply(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT, patterns -> new BannerPatternsComponent.Builder().addAll(patterns).add(RegistryEntry.of((BannerPattern)world.getRegistryManager().get(RegistryKey.ofRegistry(BannerPatterns.SKULL.getRegistry())).get(BannerPatterns.SKULL.getValue())), ((DyeItem)Items.LIME_DYE).getColor()).build());
            whateverThisIs.set(DataComponentTypes.ITEM_NAME, Text.translatable("mob-ai-tweaks.zombie_leader_banner"));
            whateverThisIs.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
            equipStack(EquipmentSlot.HEAD, whateverThisIs);
            if(FabricLoader.getInstance().isModLoaded("spears")) equipStack(EquipmentSlot.MAINHAND, Registries.ITEM.get(Identifier.ofVanilla("iron_spear")).getDefaultStack());
            setEquipmentDropChance(EquipmentSlot.HEAD, 100.0F);
            ZombieHorseEntity zombieHorse = EntityType.ZOMBIE_HORSE.create(getWorld());
            if(zombieHorse == null) return;
            zombieHorse.refreshPositionAndAngles(getPos(), getYaw(), 0F);
            zombieHorse.initialize(world, difficulty, SpawnReason.JOCKEY, null);
            zombieHorse.setBaby(false);
            zombieHorse.setTame(true);
            var attribution = zombieHorse.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if(attribution != null) attribution.setBaseValue(0.5d);
            world.spawnEntity(zombieHorse);
            startRiding(zombieHorse, true);
        }
        else if(doorEligible) {
            if(MobAITweaks.getModConfigValue("zombie_with_door_chance", 1) <= random.nextInt(100) || MobAITweaks.getModConfigValue("zombie_with_door_local_difficulty", 250) * 0.01F >= difficulty.getLocalDifficulty()) return;
            setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
            getCommandTags().add("door_zombie");
            NbtCompound nbt = new NbtCompound();
            String d = "{Passengers:[{id:\"minecraft:block_display\",block_state:{Name:\"minecraft:oak_door\",Properties:{facing:\"east\",half:\"lower\",hinge:\"left\",open:\"false\"}},transformation:[1f,0f,0f,-0.7f,0f,1f,0f,-2f,0f,0f,1f,-0.5f,0f,0f,0f,1f],teleport_duration:3,Tags:[\"door_zombie\"]},{id:\"minecraft:block_display\",block_state:{Name:\"minecraft:oak_door\",Properties:{facing:\"east\",half:\"upper\",hinge:\"left\",open:\"false\"}},transformation:[1f,0f,0f,-0.7f,0f,1f,0f,-1f,0f,0f,1f,-0.5f,0f,0f,0f,1f],teleport_duration:3,Tags:[\"door_zombie\"]}],Tags:[\"door_zombie\"]}";
            var biome = VillagerType.forBiome(world.getBiome(getBlockPos()));
            if(random.nextBoolean() && getWorld().getGameRules().getBoolean(MobAITweaks.MOBS_ARE_OP)) d = d.replace("oak_", "iron_");
            else if(biome.equals(VillagerType.DESERT) || biome.equals(VillagerType.SAVANNA)) d = d.replace("oak_", "acacia_");
            else if(biome.equals(VillagerType.SNOW) || biome.equals(VillagerType.TAIGA)) d = d.replace("oak_", "spruce_");
            else if(biome.equals(VillagerType.JUNGLE)) d = d.replace("oak_", "jungle_");
            try {
                nbt = StringNbtReader.parse(d);
            }
            catch (Throwable ignore) {
            }
            nbt.putString("id", "minecraft:block_display");
            var m = EntityType.loadEntityWithPassengers(nbt, getWorld(), e -> {
                e.refreshPositionAndAngles(getPos().add(0d, -1d, 0d), getYaw() - 90, 0F);
                e.addCommandTag("door_zombie");
                e.getPassengerList().forEach(f -> f.addCommandTag("door_zombie"));
                return e;
            });
            if(m == null) return;
            getWorld().spawnEntity(m);
            m.startRiding(this, true);
            var k = getAttributes().getCustomInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
            if(k != null) k.addTemporaryModifier(new EntityAttributeModifier(Identifier.of(MobAITweaks.MOD_ID, "door_zombie"), MobAITweaks.getModConfigValue("zombie_with_door_knockback_resist_boost", 50) * 0.01, EntityAttributeModifier.Operation.ADD_VALUE));
            var s = getAttributes().getCustomInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if(s != null) s.addTemporaryModifier(new EntityAttributeModifier(Identifier.of(MobAITweaks.MOD_ID, "door_zombie"), MobAITweaks.getModConfigValue("zombie_with_door_speed_boost", -5) * 0.01, EntityAttributeModifier.Operation.ADD_VALUE));
        }
    }
    @Override protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        super.updatePassengerPosition(passenger, positionUpdater);
        if(passenger instanceof SlimeEntity s) {
            var v = getRotationVector(0F, getBodyYaw()).multiply(0.6).add(getPos());
            positionUpdater.accept(passenger, v.getX(), v.getY() + getHeight() * 0.6 + Math.sin(getHandSwingProgress(1.0F) * Math.PI) * 0.4, v.getZ());
            s.setAttacking(false);
            if(getTarget() != null && passenger.age % 60 > 50 && s.getTarget() != null) {
                if(getNavigation().isFollowingPath()) getMoveControl().strafeTo(-1F, 0F);
                lookAtEntity(s.getTarget(), 30F, 30F);
            }
            if(!handSwinging && passenger.age % 60 == 0 && s.getTarget() != null) {
                s.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL);
                s.setAttacking(true);
                SnowballEntity b = new SnowballEntity(getWorld(), s) {
                    @Override protected void onEntityHit(EntityHitResult entityHitResult) {
                        if(entityHitResult != null && entityHitResult.getEntity() instanceof LivingEntity hitEntity && getOwner() != null) if(hitEntity.damage(getOwner().getDamageSources().thrown(this, getOwner()), getWorld().getDifficulty().getId() + 1)) hitEntity.setVelocity(hitEntity.getVelocity().multiply(3, 2, 3));
                        if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 10, 0.1d, 0.1d, 0.1d, 0.5d);
                        super.onEntityHit(entityHitResult);
                    }
                    @Override public void tick() {
                        if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 1, 0.1d, 0.1d, 0.1d, 0.1d);
                        super.tick();
                    }
                };
                b.setItem(Items.SLIME_BALL.getDefaultStack());
                b.setVelocity(b, 0F, getBodyYaw(), 0F, 1F, 5 - getWorld().getDifficulty().getId());
                getWorld().spawnEntity(b);
            }
            setAttacking(false);
        }
        if(passenger instanceof HostileEntity h && h.isBaby()) {
            var v = getEyePos().subtract(getRotationVector(0f, getBodyYaw()).multiply(0.25));
            positionUpdater.accept(passenger, v.getX(), v.getY() - 0.5, v.getZ());
            h.bodyYaw = getBodyYaw();
            h.setAttacking(true);
        }
        if(passenger instanceof DisplayEntity.BlockDisplayEntity && passenger.getCommandTags().contains("door_zombie")) {
            passenger.getPassengerList().forEach(p -> {
                p.setYaw(getBodyYaw() + (float)Math.sin(handSwingProgress * Math.PI) * (isLeftHanded() ? handSwingProgress * -40 + 20 : handSwingProgress * 40 - 20) - 90);
                if(p.getPitch() == p.prevPitch) p.setPitch(0);
            });
            setAttacking(false);
        }
    }
    @Inject(method = "damage", at = @At("HEAD"))
    private void damageDoor(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(isBlocking() && getFirstPassenger() instanceof DisplayEntity.BlockDisplayEntity e) if(source.isIn(DamageTypeTags.BYPASSES_SHIELD) || e.getCommandTags().contains("can_break")) {
            if(getWorld() instanceof ServerWorld s && e.getFirstPassenger() != null) {
                NbtCompound nbt = new NbtCompound();
                e.getFirstPassenger().writeNbt(nbt);
                String r = "minecraft:oak_door";
                if(nbt.contains("block_state")) r = nbt.getCompound("block_state").getString("Name");
                Block b = Registries.BLOCK.get(Identifier.of(r));
                bodyYaw += 90;
                s.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, b.getDefaultState()), getX() + Math.cos(bodyYaw / 180d * Math.PI) * 0.7, getY() + 1.5, getZ() + Math.sin(bodyYaw / 180d * Math.PI) * 0.7, 8, 0.1, 0.1, 0.1, 0.1);
                s.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, b.getDefaultState()), getX() + Math.cos(bodyYaw / 180d * Math.PI) * 0.7, getY() + 0.5, getZ() + Math.sin(bodyYaw / 180d * Math.PI) * 0.7, 8, 0.1, 0.1, 0.1, 0.1);
                bodyYaw -= 90;
            }
            if(e instanceof DisplayEntity.BlockDisplayEntity && e.getCommandTags().contains("door_zombie")) {
                e.getPassengerList().forEach(f -> {
                    if(f instanceof DisplayEntity.BlockDisplayEntity) f.discard();
                });
                e.discard();
            }
            playSound(SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR);
            var k = getAttributes().getCustomInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
            if(k != null) k.removeModifier(Identifier.of(MobAITweaks.MOD_ID, "door_zombie"));
            var m = getAttributes().getCustomInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if(m != null) m.removeModifier(Identifier.of(MobAITweaks.MOD_ID, "door_zombie"));
        }
        else {
            playSound(SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR);
            float swing = (float)getRandom().nextBetween(-(int)amount, (int)amount);
            e.getPassengerList().forEach(p -> p.setPitch(swing));
            float durabilityMultiplier = 1F;
            if(getWorld() instanceof ServerWorld s && e.getFirstPassenger() != null) {
                NbtCompound nbt = new NbtCompound();
                e.getFirstPassenger().writeNbt(nbt);
                String r = "minecraft:oak_door";
                if(nbt.contains("block_state")) r = nbt.getCompound("block_state").getString("Name");
                Block b = Registries.BLOCK.get(Identifier.of(r));
                durabilityMultiplier = b.getHardness() / Blocks.OAK_DOOR.getHardness();
                bodyYaw += 90;
                s.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, b.getDefaultState()), getX() + Math.cos(bodyYaw / 180d * Math.PI) * 0.7, getY() + 1.5, getZ() + Math.sin(bodyYaw / 180d * Math.PI) * 0.7, 8, 0.1, 0.1, 0.1, 0.1);
                s.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, b.getDefaultState()), getX() + Math.cos(bodyYaw / 180d * Math.PI) * 0.7, getY() + 0.5, getZ() + Math.sin(bodyYaw / 180d * Math.PI) * 0.7, 8, 0.1, 0.1, 0.1, 0.1);
                bodyYaw -= 90;
            }
            if(e.getCommandTags().stream().noneMatch(t -> t.startsWith("damaged_received:"))) e.getCommandTags().add("damaged_received:" + (int)amount);
            else {
                AtomicInteger i = new AtomicInteger(0);
                e.getCommandTags().forEach(t -> {
                    if(t.startsWith("damaged_received:")) i.set(Integer.parseInt(t.replace("damaged_received:", "")));
                });
                int j = (int)(i.get() + amount);
                if(e.getCommandTags().removeIf(t -> t.startsWith("damaged_received:"))) if(j < MobAITweaks.getModConfigValue("zombie_with_door_health", 40) * durabilityMultiplier) e.getCommandTags().add("damaged_received:" + j);
                else e.getCommandTags().add("can_break");
            }
        }
    }
    @Override protected void updatePostDeath() {
        getPassengerList().forEach(e -> {
            if(getWorld() instanceof ServerWorld s && e.getFirstPassenger() instanceof DisplayEntity.BlockDisplayEntity) {
                NbtCompound nbt = new NbtCompound();
                e.getFirstPassenger().writeNbt(nbt);
                String r = "minecraft:oak_door";
                if(nbt.contains("block_state")) r = nbt.getCompound("block_state").getString("Name");
                Block b = Registries.BLOCK.get(Identifier.of(r));
                if(getWorld().getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) dropStack(b.asItem().getDefaultStack());
                bodyYaw += 90;
                s.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, b.getDefaultState()), getX() + Math.cos(bodyYaw / 180d * Math.PI) * 0.7, getY() + 1.5, getZ() + Math.sin(bodyYaw / 180d * Math.PI) * 0.7, 8, 0.1, 0.1, 0.1, 0.1);
                s.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, b.getDefaultState()), getX() + Math.cos(bodyYaw / 180d * Math.PI) * 0.7, getY() + 0.5, getZ() + Math.sin(bodyYaw / 180d * Math.PI) * 0.7, 8, 0.1, 0.1, 0.1, 0.1);
                bodyYaw -= 90;
                playSound(SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR);
            }
            if(e instanceof DisplayEntity.BlockDisplayEntity && e.getCommandTags().contains("door_zombie")) {
                e.getPassengerList().forEach(f -> {
                    if(f instanceof DisplayEntity.BlockDisplayEntity) f.discard();
                });
                e.discard();
            }
        });
        super.updatePostDeath();
    }
    @Override public void onRemoved() {
        super.onRemoved();
        getPassengerList().forEach(e -> {
            if(e instanceof DisplayEntity.BlockDisplayEntity && e.getCommandTags().contains("door_zombie")) {
                e.getPassengerList().forEach(f -> {
                    if(f instanceof DisplayEntity.BlockDisplayEntity) f.discard();
                });
                e.discard();
            }
        });
    }
    @Override public boolean isBlocking() {
        if(getCommandTags().contains("door_zombie") && getFirstPassenger() instanceof DisplayEntity.BlockDisplayEntity e && e.getCommandTags().contains("door_zombie")) return true;
        return super.isBlocking();
    }
}

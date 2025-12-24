package com.notunanancyowen.mait;

import com.notunanancyowen.mait.commands.*;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.event.GameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("all")
public class MobAITweaks implements ModInitializer {
	public static final String MOD_ID = "mob-ai-tweaks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final GameRules.Key<GameRules.BooleanRule> MOBS_ARE_OP = GameRuleRegistry.register("crazyMobs", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(false));
	public static final TagKey<Item> SHULKER_BREEDING_ITEMS = TagKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "shulker_breeding_items"));
	public static final TagKey<Item> BANNED_BOWS_AND_CROSSBOWS = TagKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "banned_bows_and_crossbows"));
	public static final TagKey<EntityType<?>> ALWAYS_SHOOTS_CRIT_ARROWS = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "always_shoots_crit_arrows"));
	public static final TagKey<EntityType<?>> BOSS_THAT_ANNOUNCES_SUMMON = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "bosses_that_announce_spawn"));
	public static final TagKey<EntityType<?>> BOSS_THAT_ANNOUNCES_DEFEAT = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "bosses_that_announce_death"));
	public static final TagKey<EntityType<?>> HEALABLE_BY_CLERIC = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "healable_by_cleric_villager"));
	public static final TagKey<EntityType<?>> ZOMBIES_NOT_TARGETABLE_BY_ILLAGERS = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "zombies_not_attackable_by_illagers"));
	public static final TagKey<EntityType<?>> ZOMBIES_WITH_DOORS = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "zombies_that_can_have_doors"));
	public static final TagKey<EntityType<?>> ZOMBIES_WITH_SLIMES = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "zombies_that_can_have_slimes"));
	public static final TagKey<EntityType<?>> SKELETONS_THAT_DONT_SWING_HANDS_ON_PULL = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "skeletons_that_dont_swing_hands_on_pull"));
	public static final TagKey<EntityType<?>> SKELETONS_THAT_USE_VANILLA_ATTACK_CHECKS = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "skeletons_that_use_vanilla_attack_checks"));
	public static final TagKey<EntityType<?>> GOLEMS_NEVER_TARGET_AT_ALL_COSTS = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "golems_never_target_at_all_cost"));
	public static final TagKey<EntityType<?>> NO_NATURAL_HEALTH_REGENERATION = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "tameable_with_no_regeneration"));
	public static final TagKey<EntityType<?>> ESCAPABLE_SEATS = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "escapable_seats"));
	public static final TagKey<GameEvent> IGNORED_BY_SLEEPING_MOBS = TagKey.of(RegistryKeys.GAME_EVENT, Identifier.of(MOD_ID, "ignored_by_sleeping_mobs"));
	public static final TagKey<GameEvent> WAKES_UP_SLEEPING_MOBS = TagKey.of(RegistryKeys.GAME_EVENT, Identifier.of(MOD_ID, "wakes_up_sleeping_mobs"));
	public static final RegistryEntry<Potion> BLINDNESS = Registry.registerReference(Registries.POTION, Identifier.of(MOD_ID, "blindness"), new Potion("blindness", new StatusEffectInstance(StatusEffects.BLINDNESS, 500)));
	public static final RegistryEntry<Potion> LONG_BLINDNESS = Registry.registerReference(Registries.POTION, Identifier.of(MOD_ID, "long_blindness"), new Potion("blindness", new StatusEffectInstance(StatusEffects.BLINDNESS, 1000)));
	@Override public void onInitialize() {
		FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
			builder.registerPotionRecipe(BLINDNESS, Items.REDSTONE, LONG_BLINDNESS);
			builder.registerPotionRecipe(BLINDNESS, Items.FERMENTED_SPIDER_EYE, Potions.INVISIBILITY);
			builder.registerPotionRecipe(LONG_BLINDNESS, Items.FERMENTED_SPIDER_EYE, Potions.LONG_INVISIBILITY);
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			AttackCommand.register(dispatcher, registryAccess);
			MotionCommand.register(dispatcher);
			PathToCommand.register(dispatcher);
			RotateCommand.register(dispatcher);
			TargetCommand.register(dispatcher);
		});
		try {
			if(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).toFile().mkdir()) LOGGER.info("Added general directory!");
			if(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/blacklists").toFile().mkdir()) LOGGER.info("Added blacklists directory!");
			if(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/sniffer").toFile().mkdir()) LOGGER.info("Added sniffer directory!");
			if(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/ranged_mobs").toFile().mkdir()) LOGGER.info("Added ranged attacking mobs directory!");
			if(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/whitelists").toFile().mkdir()) LOGGER.info("Added whitelists directory!");
		}
		catch (SecurityException e) {
			LOGGER.info("Error making config directories: %n" + e.getLocalizedMessage());
		}
		try {
			File config = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/blacklists/modded_bows_and_crossbows.txt").toFile();
			if(config.createNewFile()) try(FileWriter configWriter = new FileWriter(config)) {
				configWriter.write("#Dual blacklist for modded bows and crossbows, takes their in-game ID (e.g. " + MOD_ID + ":modded_bow_or_crossbow_item)\nbows:stone_bow\nbows:iron_bow\nbows:golden_bow\nbows:diamond_bow\nbows:netherite_bow\nbows:stone_crossbow\nbows:iron_crossbow\nbows:golden_crossbow\nbows:diamond_crossbow\nbows:netherite_crossbow");
			}
		}
		catch (IOException | SecurityException e) {
			LOGGER.info("Error making config: %n" + e.getLocalizedMessage());
		}
		try {
			File config = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/blacklists/allay_weapons.txt").toFile();
			if(config.createNewFile()) try(FileWriter configWriter = new FileWriter(config)) {
				configWriter.write("#Blacklist for items allays can use in combat, takes their in-game ID (e.g. " + MOD_ID + ":item_to_blacklist)");
			}
			try(FileReader configReader = new FileReader(config)) {
				BufferedReader reader = new BufferedReader(configReader);
				StringBuilder bob = new StringBuilder();
				String configText = null;
				while((configText = reader.readLine()) != null) {
					bob.append(configText);
					bob.append(System.getProperty("line.separator"));
				}
				bob.deleteCharAt(bob.length() - 1);
				reader.close();
				bob.toString().lines().forEach(configContent -> {
					if(!configContent.startsWith("#Blacklist")) allayItemBlacklist.add(configContent);
				});
			}
		}
		catch (IOException | SecurityException e) {
			LOGGER.info("Error making config: %n" + e.getLocalizedMessage());
		}
		try {
			File config = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/blacklists/hostiles_that_can_sit.txt").toFile();
			if(config.createNewFile()) try(FileWriter configWriter = new FileWriter(config)) {
				configWriter.write("#Blacklist for hostile mobs that can sit down randomly, takes their in-game ID (e.g. " + MOD_ID + ":mob_to_blacklist)");
			}
			try(FileReader configReader = new FileReader(config)) {
				BufferedReader reader = new BufferedReader(configReader);
				StringBuilder bob = new StringBuilder();
				String configText = null;
				while((configText = reader.readLine()) != null) {
					bob.append(configText);
					bob.append(System.getProperty("line.separator"));
				}
				bob.deleteCharAt(bob.length() - 1);
				reader.close();
				bob.toString().lines().forEach(configContent -> {
					if(!configContent.startsWith("#Blacklist")) hostilesThatCannotSit.add(configContent);
				});
			}
		}
		catch (IOException | SecurityException e) {
			LOGGER.info("Error making config: %n" + e.getLocalizedMessage());
		}
		String escapableSeats = "#Serves as whitelist for mounts hostiles can escape from if they ever get trapped in them, accepts entity or block (for modded chairs) IDs but tags are not accepted\n" +
			"minecraft:boat\n" +
			"minecraft:chest_boat";
		try {
			File config = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/whitelists/seats_hostiles_can_escape.txt").toFile();
			if(config.createNewFile()) try(FileWriter configWriter = new FileWriter(config)) {
				configWriter.write(escapableSeats);
				configWriter.close();
			}
			try(FileReader configReader = new FileReader(config)) {
				BufferedReader reader = new BufferedReader(configReader);
				StringBuilder bob = new StringBuilder();
				String configText = null;
				while((configText = reader.readLine()) != null) {
					bob.append(configText);
					bob.append(System.getProperty("line.separator"));
				}
				bob.deleteCharAt(bob.length() - 1);
				reader.close();
				bob.toString().lines().forEach(configContent -> {
					if(!configContent.startsWith("#Serves")) seatsHostilesCanEscape.add(configContent);
				});
				configReader.close();
			}
		}
		catch (IOException | SecurityException e) {
			LOGGER.info("Error making config: %n" + e.getLocalizedMessage());
		}
		String snifferConfig = "#Example is as goes, mob-ai-tweaks:item_to_sniff=mob-ai-tweaks:biome_to_find, do note that it supports biome tags but not item tags\n" +
			"minecraft:acacia_sapling=#minecraft:is_savanna\n" +
			"minecraft:bamboo=minecraft:bamboo_jungle\n" +
			"minecraft:birch_sapling=minecraft:birch_forest\n" +
			"minecraft:brown_mushroom=minecraft:swamp\n" +
			"minecraft:cherry_sapling=minecraft:cherry_grove\n" +
			"minecraft:dark_oak_sapling=minecraft:dark_forest\n" +
			"minecraft:mangrove_roots=minecraft:mangrove_swamp\n" +
			"minecraft:oak_sapling=minecraft:forest\n" +
			"minecraft:red_mushroom=minecraft:mushroom_fields\n" +
			"minecraft:red_sand=#minecraft:is_badlands\n" +
			"minecraft:sand=minecraft:desert\n" +
			"minecraft:spruce_sapling=#minecraft:is_taiga\n" +
			"minecraft:sunflower=minecraft:plains";
		try {
			File config = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/sniffer/item_to_biome.txt").toFile();
			if(config.createNewFile()) try(FileWriter configWriter = new FileWriter(config)) {
				configWriter.write(snifferConfig);
			}
			try(FileReader configReader = new FileReader(config)) {
				BufferedReader reader = new BufferedReader(configReader);
				StringBuilder bob = new StringBuilder();
				String configText = null;
				while((configText = reader.readLine()) != null) {
					bob.append(configText);
					bob.append(System.getProperty("line.separator"));
				}
				bob.deleteCharAt(bob.length() - 1);
				reader.close();
				bob.toString().lines().forEach(configContent -> {
					if(!configContent.startsWith("#Example")) {
						String[] configValues = configContent.split("=", 2);
						for(int i = 0; i < configValues.length; i++) if(!configValues[i].contains(":")) configValues[i] = configValues[i].contains("#") ? configValues[i].replace("#", "#minecraft:") : "minecraft:" + configValues[i];
						snifferItemToBiome.put(configValues[0], configValues[1]);
					}
				});
			}
		}
		catch (IOException | SecurityException e) {
			LOGGER.info("Error making config: %n" + e.getLocalizedMessage());
		}
		String secondariesConfig = "#Example is as goes, mob-ai-tweaks:entity_id=mob-ai-tweaks:item_id, do note that this does not support tags\n" +
			"minecraft:pillager=minecraft:stone_axe\n" +
			"minecraft:skeleton=minecraft:wooden_sword\n" +
			"minecraft:stray=minecraft:stone_sword\n" +
			"minecraft:bogged=minecraft:wooden_sword\n" +
			"minecraft:wither_skeleton=minecraft:bow";
		try {
			File config = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/ranged_mobs/mob_secondary_weapons.txt").toFile();
			if(config.createNewFile()) try(FileWriter configWriter = new FileWriter(config)) {
				configWriter.write(secondariesConfig);
			}
			try(FileReader configReader = new FileReader(config)) {
				BufferedReader reader = new BufferedReader(configReader);
				StringBuilder bob = new StringBuilder();
				String configText;
				while((configText = reader.readLine()) != null) {
					bob.append(configText);
					bob.append(System.lineSeparator());
				}
				bob.deleteCharAt(bob.length() - 1);
				reader.close();
				bob.toString().lines().forEach(configContent -> {
					if(!configContent.startsWith("#Example")) {
						String[] configValues = configContent.split("=", 2);
						for(int i = 0; i < configValues.length; i++) if(!configValues[i].contains(":")) configValues[i] = "minecraft:" + configValues[i];
						mobSecondaryWeapons.put(configValues[0], configValues[1]);
					}
				});
			}
		}
		catch (IOException | SecurityException e) {
			LOGGER.info("Error making config: %n" + e.getLocalizedMessage());
		}
		String rangedAttackerCooldowns = "#Example is as goes, mob-ai-tweaks:entity_id=[easy_value,normal_value,hard_value](values must be in ticks), do note that this does not support tags and that crazy mobs mode overrides any of these\n" +
			"minecraft:piglin=[20,15,10]\n" +
			"minecraft:pillager=[20,15,10]\n" +
			"minecraft:wither_skeleton=[60,50,40]";
		try {
			File config = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/ranged_mobs/override_attack_intervals.txt").toFile();
			if(config.createNewFile()) try(FileWriter configWriter = new FileWriter(config)) {
				configWriter.write(rangedAttackerCooldowns);
			}
			try(FileReader configReader = new FileReader(config)) {
				BufferedReader reader = new BufferedReader(configReader);
				StringBuilder bob = new StringBuilder();
				String configText = null;
				while((configText = reader.readLine()) != null) {
					bob.append(configText);
					bob.append(System.lineSeparator());
				}
				bob.deleteCharAt(bob.length() - 1);
				reader.close();
				bob.toString().lines().forEach(configContent -> {
					if(!configContent.startsWith("#Example")) {
						String[] configValues = configContent.split("=", 2);
						if(!configValues[0].contains(":")) configValues[0] = "minecraft:" + configValues[0];
						String[] cooldownsProlly = configValues[1].replace("[", "").replace("]", "").replace(" ", "").split(",", 3);
						int[] cooldownOptions = new int[cooldownsProlly.length];
						for(int i = 0; i < cooldownOptions.length; i++) try {
							cooldownOptions[i] = Integer.parseInt(cooldownsProlly[i]);
						}
						catch (Throwable t) {
							cooldownOptions[i] = 0;
							LOGGER.info("Error making config: %n" + t.getLocalizedMessage());
						}
						mobRangedAttackIntervals.put(configValues[0], cooldownOptions);
					}
				});
			}
		}
		catch (IOException | SecurityException e) {
			LOGGER.info("Error making config: %n" + e.getLocalizedMessage());
		}
		String originalConfig = "#All cooldowns and durations are in ticks (20 ticks is 1 second), chances are in multiplied percentage (50 * 15% is 7.5), boosts are in added percentage (200% means 3x more, -100% means zero), and local difficulty is in percentage (250% means 2.5 local difficulty)\n" +
			"allay_rework=true\n" +
			"animals_can_be_fed_chorus_fruit=true\n" +
			"blaze_attack_rework=true\n" +
			"blaze_fireball_count_easy=2\n" +
			"blaze_fireball_count_normal=3\n" +
			"blaze_fireball_count_hard=4\n" +
			"blazes_strafe_when_shooting=true\n" +
			"bogged_special_attacks=true\n" +
			"bogged_special_attack_cooldown=120\n" +
			"bosses_enrage=true\n" +
			"bosses_announce_spawn_and_death=true\n" +
			"burn_and_freeze_visual_effects=true\n" +
			"burning_projectile_visuals=true\n" +
			"cats_avoid_projectiles=true\n" +
			"chicken_jockey_special_attacks=true\n" +
			"chicken_jockey_special_attack_cooldown=100\n" +
			"chickens_flee_from_mobs=true\n" +
			"chickens_shed_feathers=true\n" +
			"chickens_shed_feather_chance=50\n" +
			"creepers_explode_stuck_arrows=true\n" +
			"creepers_delay_explosion_when_hit=true\n" +
			"disable_golem_infighting=true\n" +
			"drowned_rework=true\n" +
			"drowned_swimming_animation=true\n" +
			"enchantments_from_mod=true\n" +
			"ender_dragon_crystal_threshold_peaceful=1\n" +
			"ender_dragon_crystal_threshold_easy=2\n" +
			"ender_dragon_crystal_threshold_normal=3\n" +
			"ender_dragon_crystal_threshold_hard=4\n" +
			"ender_dragon_healing_rework=true\n" +
			"ender_dragon_rework=true\n" +
			"enderman_rework=true\n" +
			"enderman_rework_attack_animation=true\n" +
			"elder_guardians_are_bosses=true\n" +
			"elder_guardians_boss_armor=8\n" +
			"elder_guardians_boss_health=150\n" +
			"evoker_summon_vex_cooldown=600\n" +
			"evoker_summon_vex_count_easy=2\n" +
			"evoker_summon_vex_count_normal=3\n" +
			"evoker_summon_vex_count_hard=4\n" +
			"evokers_cast_fireball=true\n" +
			"evokers_cast_fireball_cooldown=20\n" +
			"evokers_cast_fireball_count=3\n" +
			"evoker_totem_clutch_chance=20\n" +
			"fireworks_boost_mounts=true\n" +
			"ghasts_cry_tears=true\n" +
			"ghasts_cry_tears_cooldown=60\n" +
			"goat_horns_draw_aggro=true\n" +
			"hostile_mobs_can_escape_boats=true\n" +
			"hostile_mobs_can_sit=true\n" +
			"hostile_mobs_sitting_can_be_startled=true\n" +
			"hostile_mobs_spawn_with_effects_chance=10\n" +
			"husk_burrow_cooldown=200\n" +
			"husk_burrow_min_distance=12\n" +
			"husks_can_burrow=true\n" +
			"illagers_and_normal_zombies_fight=true\n" +
			"illagers_and_zombie_villagers_fight=true\n" +
			"illagers_and_zombies_prioritize_player=true\n" +
			"illagers_use_boats=true\n" +
			"illusioner_rework=true\n" +
			"illusioner_bad_omen_spawn_chance=5\n" +
			"iron_golem_rework=true\n" +
			"iron_golem_pickaxe_damage_boost=100\n" +
			"line_of_sight_rework=true\n" +
			"melee_mobs_jump_to_reach=true\n" +
			"ominous_mobs=true\n" +
			"phantom_rework=true\n" +
			"phantom_projectile_damage_boost=200\n" +
			"phantom_reset_insomnia_size=3\n" +
			"phantoms_become_hostile_size=3\n" +
			"phantom_boss_follow_range=160\n" +
			"phantom_boss_health=100\n" +
			"phantom_boss_size=20\n" +
			"phantom_boss_spawn_chance=1\n" +
			"phantoms_from_spawner_always_attack=true\n" +
			"pillager_crossbow_range=15\n" +
			"pillager_melee_mode_speed_boost=0\n" +
			"pillager_shield_break_cooldown=60\n" +
			"pillager_switch_to_melee_range=3\n" +
			"pillagers_eat_food=true\n" +
			"pillagers_use_modded_crossbows=true\n" +
			"ranged_mobs_reposition=true\n" +
			"ranged_mobs_use_guns=true\n" +
			"ravagers_are_minibosses=true\n" +
			"ravager_special_attacks=true\n" +
			"ravager_special_attack_cooldown=180\n" +
			"shulker_breed_heal_amount=10\n" +
			"shulker_pickaxe_damage_boost=150\n" +
			"shulker_duplicate_dye_amount=3\n" +
			"shulkers_are_breedable=true\n" +
			"shulkers_are_dyeable=true\n" +
			"shulkers_can_be_silk_touched=true\n" +
			"silverfish_can_hide=true\n" +
			"silverfish_hide_cooldown=100\n" +
			"silverfish_hide_duration=100\n" +
			"silverfish_shovel_damage_boost=200\n" +
			"silverfish_stop_to_call_help=true\n" +
			"skeleton_babies=true\n" +
			"skeleton_horsemen=true\n" +
			"skeleton_melee_mode_speed_boost=10\n" +
			"skeleton_sniper_AI=true\n" +
			"skeleton_special_attacks=true\n" +
			"skeleton_special_attack_cooldown=200\n" +
			"skeleton_strafe_speed_buff=true\n" +
			"skeleton_switch_to_melee_range=0\n" +
			"skeletons_can_convert_to_wither=true\n" +
			"skeletons_swing_off_hand_on_draw=true\n" +
			"skeletons_use_modded_bows=true\n" +
			"sneak_to_approach_animals=true\n" +
			"sniffer_announces_biome_find=true\n" +
			"sniffer_announces_block_find=true\n" +
			"sniffer_rework=true\n" +
			"snow_golem_rework=true\n" +
			"snow_golem_attack_rework=true\n" +
			"snow_golem_pumpkin_can_be_returned=true\n" +
			"snow_golem_armor_if_it_has_pumpkin=10\n" +
			"snow_golem_accuracy_when_no_pumpkin=2\n" +
			"spiders_attack_smaller_arthropods=true\n" +
			"squid_ink_inflicts_debuffs=true\n" +
			"squid_ink_debuff_radius=8\n" +
			"squid_ink_debuff_duration=100\n" +
			"glow_squid_night_vision_on_stare_duration=600\n" +
			"stray_special_attacks=true\n" +
			"stray_special_attack_cooldown=120\n" +
			"tamed_mobs_regen_amount=1\n" +
			"tamed_mobs_regen_cooldown=20\n" +
			"throwable_fire_charges=true\n" +
			"undead_horses_burn_in_day=true\n" +
			"vex_rework=true\n" +
			"vex_charge_attack_min_distance=4\n" +
			"vex_projectile_damage_boost=200\n" +
			"villagers_eat_food=true\n" +
			"villagers_have_special_roles=true\n" +
			"villagers_max_special_services=12\n" +
			"villagers_announce_special_services=true\n" +
			"wandering_traders_blind_attackers=true\n" +
			"wardens_get_stunned_by_bells=true\n" +
			"witches_drink_jump_boost=true\n" +
			"witches_show_potion_before_throwing_it=true\n" +
			"wither_death_animation=true\n" +
			"wither_rework=true\n" +
			"wither_boss_armor=12\n" +
			"wither_boss_follow_range=120\n" +
			"wither_boss_health=300\n" +
			"wither_boss_drops_netherite=true\n" +
			"wither_skeleton_special_attacks=true\n" +
			"wither_skeleton_special_attack_cooldown=200\n" +
			"wither_skeletons_with_bows_chance=10\n" +
			"wither_skeletons_with_bows_local_difficulty=250\n" +
			"zombie_dads_from_bedrock=true\n" +
			"zombie_dads_spawn_chance=1\n" +
			"zombie_dads_spawn_local_difficulty=150\n" +
			"zombie_leader_rework=true\n" +
			"zombie_leader_spawn_chance=10\n" +
			"zombie_slimers=true\n" +
			"zombie_slimer_spawn_chance=1\n" +
			"zombie_slimer_spawn_local_difficulty=150\n" +
			"zombie_with_door_chance=1\n" +
			"zombie_with_door_local_difficulty=250\n" +
			"zombie_with_door_speed_boost=-5\n" +
			"zombie_with_door_knockback_resist_boost=50\n" +
			"zombie_with_door_health=40\n" +
			"zombie_pigmen_can_use_crossbows=true\n" +
			"zombie_pigmen_with_crossbows_chance=20\n" +
			"zombie_pigmen_with_crossbows_local_difficulty=250\n" +
			"zombie_villager_special_attacks=true";
		long originalConfigSize = originalConfig.lines().count();
		try {
			File config = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/general_config.txt").toFile();
			if(config.createNewFile()) try(FileWriter configWriter = new FileWriter(config)) {
				configWriter.write(originalConfig);
			}
			try(FileReader configReader = new FileReader(config)) {
				BufferedReader reader = new BufferedReader(configReader);
				StringBuilder bob = new StringBuilder();
				String configText = null;
				int tries = -1;
				while(++tries <= originalConfigSize && (configText = reader.readLine()) != null) {
					bob.append(configText);
					bob.append(System.lineSeparator());
				}
				bob.deleteCharAt(bob.length() - 1);
				reader.close();
				bob.toString().lines().forEach(configContent -> {
					if(!configContent.startsWith("#") && configContent.contains("=")) {
						String[] configValues = configContent.split("=", 2);
						if(configValues[1].contains("false")) disabledFeatures.add(configValues[0]);
						else if(!configValues[1].contains("true")) {
							boolean putItIn = true;
							for(int i = 0; i < configValues[1].length(); i++) if(!Character.isDigit(configValues[1].charAt(i))) {
								putItIn = false;
								break;
							}
							if(putItIn) configIntegers.put(configValues[0], Integer.valueOf(configValues[1]));
						}
					}
				});
				configReader.close();
				if(bob.toString().lines().count() == originalConfigSize) return;
			}
			config.delete();
			if(config.createNewFile()) try(FileWriter configWriter = new FileWriter(config)) {
				originalConfig.lines().forEach(configContent -> {
					if(configContent.startsWith("#") || !configContent.contains("=")) {
						try {
							configWriter.append(configContent);
							if(originalConfig.lines().toList().indexOf(configContent) < originalConfigSize - 1) configWriter.append(System.getProperty("line.separator"));
						}
						catch (IOException e) {
							return;
						}
						return;
					}
					String[] configValues = configContent.split("=", 2);
					if(configIntegers.containsKey(configValues[0])) {
						try {
							String number = "";
							for(int i = 0; i < configValues[1].length(); i++) if(Character.isDigit(configValues[1].charAt(i))) number += configValues[1].charAt(i);
							configContent = configValues[0] + "=" + getModConfigValue(configValues[0], Integer.valueOf(number));
							configWriter.append(configContent);
							if(originalConfig.lines().toList().indexOf(configContent) < originalConfigSize - 1) configWriter.append(System.getProperty("line.separator"));
						}
						catch (IOException e) {
							return;
						}
						return;
					}
					if(disabledFeatures.contains(configValues[0]) || configValues[1].contains("false")) configContent = configContent.replace("true", "false");
					try {
						configWriter.append(configContent);
						if(originalConfig.lines().toList().indexOf(configContent) < originalConfigSize - 1) configWriter.append(System.getProperty("line.separator"));
					}
					catch (IOException e) {
					}
				});
			}
			LOGGER.info("Config updated!");
		}
		catch (IOException | SecurityException e) {
			LOGGER.info("Error making config: %n" + e.getLocalizedMessage());
		}
		LOGGER.info("Mob AIs have been tweaked!");
	}
	public static int getRangedAttackCooldown(Entity entity, int[] fallback) {
		int worldDifficulty = entity.getWorld().getDifficulty().getId() - 1;
		String typeName = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
		if(!mobRangedAttackIntervals.containsKey(typeName)) return fallback[worldDifficulty];
		try {
			int[] newValue = mobRangedAttackIntervals.getOrDefault(typeName, fallback);
			return newValue[worldDifficulty];
		}
		catch (Throwable t) {
			LOGGER.info("Error getting ranged attack cooldown for " + typeName + ", resorting to default vanilla values");
			return fallback[worldDifficulty];
		}
	}
	public static ItemStack getSecondaryWeapon(EntityType type) {
		String typeName = Registries.ENTITY_TYPE.getId(type).toString();
		if(!mobSecondaryWeapons.containsKey(typeName)) return ItemStack.EMPTY;
		String itemName = mobSecondaryWeapons.get(typeName);
		try {
			return Registries.ITEM.get(Identifier.of(itemName)).getDefaultStack();
		}
		catch (Throwable t) {
			LOGGER.info("Error getting secondary weapon for " + typeName + ", " + itemName + " does not exist or the mod needed for it is unloaded");
			return ItemStack.EMPTY;
		}
	}
	public static boolean canBeDismounted(Entity entity) {
		if(seatsHostilesCanEscape == null || entity == null) return entity != null && entity.getType().isIn(ESCAPABLE_SEATS);
		String typeName = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
		String className = entity.getClass().getSimpleName();
		if(className.isBlank() && entity.getClass().getSuperclass() != null) className = entity.getClass().getSuperclass().getName();
		if(entity.getEntityWorld().getBlockState(entity.getBlockPos()).getBlock().getClass().getSimpleName().contains(className) || className.contains("Seat") || className.contains("seat") || className.contains("Chair") || className.contains("chair") || className.contains("Stool") || className.contains("stool")) return seatsHostilesCanEscape.contains(Registries.BLOCK.getId(entity.getEntityWorld().getBlockState(entity.getBlockPos()).getBlock()).toString()) || seatsHostilesCanEscape.contains(typeName) || entity.getType().isIn(ESCAPABLE_SEATS);
		return (seatsHostilesCanEscape.contains(typeName) && entity.isOnGround()) || entity.getType().isIn(ESCAPABLE_SEATS);
	}
	public static boolean isOminous(LivingEntity entity) {
		return entity != null && getModConfigValue("ominous_mobs") && (entity.hasStatusEffect(StatusEffects.BAD_OMEN) || entity.hasStatusEffect(StatusEffects.TRIAL_OMEN));
	}
	private final static HashMap<String, Integer> configIntegers = new HashMap<>();
	private final static ArrayList<String> disabledFeatures = new ArrayList<>();
	private final static ArrayList<Item> possibleBows = new ArrayList<>();
	private final static ArrayList<Item> possibleCrossbows = new ArrayList<>();
	public static Item getRandomBow(Random random) {
		if(!getModConfigValue("skeletons_use_modded_bows")) return Items.BOW;
		if(possibleBows.isEmpty()) {
			LOGGER.info("List of possible modded bow items are empty, preparing to fill up...");
			Registries.ITEM.stream().filter(item -> item instanceof BowItem && !item.getDefaultStack().isIn(BANNED_BOWS_AND_CROSSBOWS)).forEach(item -> possibleBows.add(item));
			if(possibleBows.size() > 1) {
				try {
					File config = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/blacklists/modded_bows_and_crossbows.txt").toFile();
					if(config.exists()) try(FileReader configReader = new FileReader(config)) {
						BufferedReader reader = new BufferedReader(configReader);
						StringBuilder bob = new StringBuilder();
						String configText = null;
						int tries = -1;
						while(++tries < Integer.MAX_VALUE && (configText = reader.readLine()) != null) {
							bob.append(configText);
							bob.append(System.lineSeparator());
						}
						bob.deleteCharAt(bob.length() - 1);
						reader.close();
						String b = bob.toString();
						String blacklist = b.substring(b.indexOf(")") + 1);
						if(possibleBows.removeIf(bow -> blacklist.contains(bow.toString()))) LOGGER.info(String.format("Some modded bow items have been blacklisted..."));
						configReader.close();
					}
				}
				catch (IOException | SecurityException e) {
					LOGGER.info("Error reading config: %n" + e.getLocalizedMessage());
				}
				LOGGER.info(String.format("Found %d modded bow items, all have been registered. Expect skeletons to spawn with them!", possibleBows.size()));
			}
			else LOGGER.info("No modded bow items have been found!");
		}
		return possibleBows.size() > 1 ? possibleBows.get(random.nextInt(possibleBows.size() - 1) + 1) : Items.BOW;
	}
	public static Item getRandomCrossbow(Random random) {
		if(!getModConfigValue("pillagers_use_modded_crossbows")) return Items.CROSSBOW;
		if(possibleCrossbows.isEmpty()) {
			LOGGER.info("List of possible modded crossbow items are empty, preparing to fill up...");
			Registries.ITEM.stream().filter(item -> item instanceof CrossbowItem && !item.getDefaultStack().isIn(BANNED_BOWS_AND_CROSSBOWS)).forEach(item -> possibleCrossbows.add(item));
			if(possibleCrossbows.size() > 1) {
				try {
					File config = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "/blacklists/modded_bows_and_crossbows.txt").toFile();
					if(config.exists()) try(FileReader configReader = new FileReader(config)) {
						BufferedReader reader = new BufferedReader(configReader);
						StringBuilder bob = new StringBuilder();
						String configText = null;
						int tries = -1;
						while(++tries < Integer.MAX_VALUE && (configText = reader.readLine()) != null) {
							bob.append(configText);
							bob.append(System.lineSeparator());
						}
						bob.deleteCharAt(bob.length() - 1);
						reader.close();
						String b = bob.toString();
						String blacklist = b.substring(b.indexOf(")") + 1);
						if(possibleCrossbows.removeIf(crossbow -> blacklist.contains(crossbow.toString()))) LOGGER.info(String.format("Some modded bow items have been blacklisted..."));
						configReader.close();
					}
				}
				catch (IOException | SecurityException e) {
					LOGGER.info("Error reading config: %n" + e.getLocalizedMessage());
				}
				LOGGER.info(String.format("Found %d modded crossbow items, all have been registered. Expect pillagers to spawn with them!", possibleCrossbows.size()));
			}
			else LOGGER.info("No modded crossbow items have been found!");
		}
		return possibleCrossbows.size() > 1 ? possibleCrossbows.get(random.nextInt(possibleCrossbows.size() - 1) + 1) : Items.CROSSBOW;
	}
	public static boolean getModConfigValue(String whatToGet) {
		if(disabledFeatures.isEmpty()) {
			return true;
		}
		try {
			return !disabledFeatures.contains(whatToGet);
		}
		catch(Throwable ignore) {
			return true;
		}
	}
	public static int getModConfigValue(String whatToGet, int fallback) {
		if(configIntegers.isEmpty()) {
			return fallback;
		}
		try {
			return configIntegers.getOrDefault(whatToGet, fallback);
		}
		catch(Throwable ignore) {
			return fallback;
		}
	}
	public static final ArrayList<String> hostilesThatCannotSit = new ArrayList<>();
	public static final ArrayList<String> seatsHostilesCanEscape = new ArrayList<>();
	public static final ArrayList<String> allayItemBlacklist = new ArrayList<>();
	public static final HashMap<String, String> snifferItemToBiome = new HashMap<>();
	private static final HashMap<String, String> mobSecondaryWeapons = new HashMap<>();
	private static final HashMap<String, int[]> mobRangedAttackIntervals = new HashMap<>();
}
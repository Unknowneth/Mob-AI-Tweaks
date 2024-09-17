package com.notunanancyowen;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;

@SuppressWarnings("all")
public class MobAITweaks implements ModInitializer {
	public static final String MOD_ID = "mob-ai-tweaks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final GameRules.Key<GameRules.BooleanRule> MOBS_ARE_OP = GameRuleRegistry.register("crazyMobs", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(false));
	@Override public void onInitialize() {
		String originalConfig = "blazes_strafe_when_shooting=true\n" +
			"bosses_enrage=true\n" +
			"chickens_flee_from_mobs=true\n" +
			"chickens_shed_feathers=true\n" +
			"creeper_explosions_rework=true\n" +
			"ender_dragon_rework=true\n" +
			"elder_guardians_are_bosses=true\n" +
			"evokers_cast_fireball=true\n" +
			"hostile_mobs_can_sit=true\n" +
			"ghasts_cry_tears=true\n" +
			"illagers_use_boats=true\n" +
			"illagers_and_zombie_villagers_fight=true\n" +
			"line_of_sight_rework=true\n" +
			"phantom_rework=true\n" +
			"pillagers_eat_food=true\n" +
			"pillagers_can_melee_attack=true\n" +
			"pillagers_use_modded_crossbows=true\n" +
			"ravagers_are_minibosses=true\n" +
			"ranged_mobs_reposition=true\n" +
			"skeleton_babies=true\n" +
			"skeleton_horsemen=true\n" +
			"skeleton_sniper_AI=true\n" +
			"skeleton_special_attacks=true\n" +
			"skeletons_use_modded_bows=true\n" +
			"sneak_to_approach_animals=true\n" +
			"stray_special_attacks=true\n" +
			"throwable_fire_charges=true\n" +
			"vex_rework=true\n" +
			"villagers_eat_food=true\n" +
			"villagers_have_special_roles=true\n" +
			"wardens_get_stunned_by_bells=true\n" +
			"wither_death_animation=true\n" +
			"wither_rework=true\n" +
			"wither_skeleton_special_attacks=true\n" +
			"zombie_dads_from_bedrock=true\n" +
			"zombie_pigmen_can_use_crossbows=true\n" +
			"zombie_villager_special_attacks=true";
		long originalConfigSize = originalConfig.lines().count();
		try {
			File config = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "_config.txt").toFile();
			if(config.createNewFile()) try(FileWriter configWriter = new FileWriter(config)) {
				configWriter.write(originalConfig);
				configWriter.close();
			}
			try(FileReader configReader = new FileReader(config)) {
				BufferedReader reader = new BufferedReader(configReader);
				StringBuilder bob = new StringBuilder();
				String configText = null;
				int tries = -1;
				while(++tries <= originalConfigSize && (configText = reader.readLine()) != null) {
					bob.append(configText);
					bob.append(System.getProperty("line.separator"));
				}
				bob.deleteCharAt(bob.length() - 1);
				reader.close();
				bob.toString().lines().forEach(configContent -> {
					String[] configValues = configContent.split("=", 2);
					if(configValues[1].contains("false")) disabledFeatures.add(configValues[0]);
				});
				configReader.close();
				if(bob.toString().lines().count() == originalConfigSize) return;
			}
			config.delete();
			if(config.createNewFile()) try(FileWriter configWriter = new FileWriter(config)) {
				originalConfig.lines().forEach(configContent -> {
					String[] configValues = configContent.split("=", 2);
					if(disabledFeatures.contains(configValues[0]) || configValues[1].contains("false")) configContent = configContent.replace("true", "false");
					try {
						configWriter.append(configContent);
						if(originalConfig.lines().toList().indexOf(configContent) < originalConfigSize - 1) configWriter.append(System.getProperty("line.separator"));
					}
					catch (IOException e) {
					}
				});
				configWriter.close();
			}
			LOGGER.info("Config updated!");
		}
		catch (IOException | SecurityException e) {
			LOGGER.info("Error making config: %n" + e.getLocalizedMessage());
		}
		LOGGER.info("Mob AIs have been tweaked!");
	}
	private final static ArrayList<String> disabledFeatures = new ArrayList<>();
	private final static ArrayList<Item> possibleBows = new ArrayList<>();
	private final static ArrayList<Item> possibleCrossbows = new ArrayList<>();
	public static Item getRandomBow(Random random) {
		if(!getModConfigValue("skeletons_use_modded_bows")) return Items.BOW;
		if(possibleBows.isEmpty()) {
			LOGGER.info("List of possible modded bow items are empty, preparing to fill up...");
			Registries.ITEM.stream().filter(item -> item instanceof BowItem).forEach(item -> possibleBows.add(item));
			if(possibleBows.size() > 1) LOGGER.info(String.format("Found %d modded bow items, all have been registered. Expect skeletons to spawn with them!", possibleBows.size()));
			else LOGGER.info("No modded bow items have been found!");
		}
		return possibleBows.size() > 1 ? possibleBows.get(random.nextInt(possibleBows.size() - 1) + 1) : Items.BOW;
	}
	public static Item getRandomCrossbow(Random random) {
		if(!getModConfigValue("pillagers_use_modded_crossbows")) return Items.CROSSBOW;
		if(possibleCrossbows.isEmpty()) {
			LOGGER.info("List of possible modded crossbow items are empty, preparing to fill up...");
			Registries.ITEM.stream().filter(item -> item instanceof CrossbowItem).forEach(item -> possibleCrossbows.add(item));
			if(possibleCrossbows.size() > 1) LOGGER.info(String.format("Found %d modded crossbow items, all have been registered. Expect pillagers to spawn with them!", possibleCrossbows.size()));
			else LOGGER.info("No modded crossbow items have been found!");
		}
		return possibleCrossbows.size() > 1 ? possibleCrossbows.get(random.nextInt(possibleCrossbows.size() - 1) + 1) : Items.CROSSBOW;
	}
	public static boolean getModConfigValue(String whatToGet) {
		if(disabledFeatures == null || disabledFeatures.isEmpty()) {
			return true;
		}
		try {
			return !disabledFeatures.contains(whatToGet);
		}
		catch(Exception e) {
			return true;
		}
	}
}
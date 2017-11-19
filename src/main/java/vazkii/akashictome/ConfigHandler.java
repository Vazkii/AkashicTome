package vazkii.akashictome;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ConfigHandler {

	public static Configuration config;

	public static boolean allItems;
	public static List<String> whitelistedItems, whitelistedNames, blacklistedMods;

	public static Map<String, String> aliases = new HashMap();

	public static void init(File configFile) {
		config = new Configuration(configFile);

		config.load();
		load();

		MinecraftForge.EVENT_BUS.register(new ChangeListener());
	}

	public static void load() {
		allItems = loadPropBool("Allow all items to be added", false);
		
		whitelistedItems = loadPropStringList("Whitelisted Items", "roots:runedTablet", "opencomputers:tool:4", "immersiveengineering:tool:3", "integrateddynamics:on_the_dynamics_of_integration");
		whitelistedNames = loadPropStringList("Whitelisted Names", "book", "tome", "lexicon", "nomicon", "manual", "knowledge", "pedia", "compendium", "guide", "codex", "journal");
		blacklistedMods = loadPropStringList("Blacklisted Mods");

		aliases.clear();
		List<String> aliasesList = loadPropStringList("Mod Aliases", 
				"nautralpledge=botania", 
				"thermalexpansion=thermalfoundation",
				"thermaldynamics=thermalfoundation",
				"thermalcultivation=thermalfoundation", 
				"rftoolsdim=rftools");		for(String s : aliasesList)
			if(s.matches(".+?=.+")) {
				String[] tokens = s.toLowerCase().split("=");
				aliases.put(tokens[0], tokens[1]);
			}

		if(config.hasChanged())
			config.save();
	}

	public static List<String> loadPropStringList(String propName, String... default_) {
		Property prop = config.get(Configuration.CATEGORY_GENERAL, propName, default_);
		return Arrays.asList(prop.getStringList());
	}

	public static boolean loadPropBool(String propName, boolean default_) {
		Property prop = config.get(Configuration.CATEGORY_GENERAL, propName, default_);
		return prop.getBoolean(default_);
	}

	public static class ChangeListener {

		@SubscribeEvent
		public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
			if(eventArgs.getModID().equals(AkashicTome.MOD_ID))
				load();
		}

	}
}

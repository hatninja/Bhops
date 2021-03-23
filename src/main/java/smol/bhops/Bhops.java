package smol.bhops;

import net.fabricmc.api.ModInitializer;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.ConfigManager;
import me.sargunvohra.mcmods.autoconfig1u.gui.registry.GuiRegistry;
import me.sargunvohra.mcmods.autoconfig1u.serializer.GsonConfigSerializer;
import me.sargunvohra.mcmods.autoconfig1u.util.Utils;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.ClientModInitializer;
import smol.bhops.config.BhopsConfig;

public class Bhops implements ModInitializer {
	public static final String MOD_ID = "bhops";
	public static ConfigManager configManager;

	@Override
	public void onInitialize() {
		AutoConfig.register(BhopsConfig.class, GsonConfigSerializer::new);

		System.out.println("Bhops initialized.");
	}
}

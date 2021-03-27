package smol.bhops;

import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.ConfigManager;
import me.sargunvohra.mcmods.autoconfig1u.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
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

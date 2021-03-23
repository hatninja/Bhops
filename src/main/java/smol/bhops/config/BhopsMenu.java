package smol.bhops.config;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import smol.bhops.Bhops;

public class BhopsMenu implements ModMenuApi {
    @Override
    public String getModId() {
        return Bhops.MOD_ID;
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(BhopsConfig.class, parent).get();
    }
}
package smol.bhops.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.ConfigHolder;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class BhopsMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BhopsMenu::createConfigScreen;
    }

    private static Screen createConfigScreen(Screen parent) {
        ConfigHolder<BhopsConfig> holder = AutoConfig.getConfigHolder(BhopsConfig.class);
        BhopsConfig config = holder.getConfig();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("text.autoconfig.bhops.title"))
                .setSavingRunnable(holder::save);
        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(
                Text.translatable("text.autoconfig.bhops.category.general")
        );

        general.addEntry(entries.startBooleanToggle(
                        Text.translatable("text.autoconfig.bhops.option.showSpeed"),
                        config.showSpeed
                )
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.showSpeed = value)
                .build());
        general.addEntry(entries.startBooleanToggle(
                        Text.translatable("text.autoconfig.bhops.option.enableBhops"),
                        config.enableBhops
                )
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.enableBhops = value)
                .build());
        general.addEntry(entries.startBooleanToggle(
                        Text.translatable("text.autoconfig.bhops.option.exclusiveToPlayers"),
                        config.exclusiveToPlayers
                )
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.exclusiveToPlayers = value)
                .build());

        general.addEntry(entries.startFloatField(
                        Text.translatable("text.autoconfig.bhops.option.sv_friction"),
                        config.sv_friction
                )
                .setDefaultValue(0.5F)
                .setMin(0.0F)
                .setMax(1.0F)
                .setSaveConsumer(value -> config.sv_friction = value)
                .build());
        general.addEntry(entries.startFloatField(
                        Text.translatable("text.autoconfig.bhops.option.sv_accelerate"),
                        config.sv_accelerate
                )
                .setDefaultValue(0.1F)
                .setMin(0.0F)
                .setMax(10.0F)
                .setSaveConsumer(value -> config.sv_accelerate = value)
                .build());
        general.addEntry(entries.startFloatField(
                        Text.translatable("text.autoconfig.bhops.option.sv_airaccelerate"),
                        config.sv_airaccelerate
                )
                .setDefaultValue(0.2F)
                .setMin(0.0F)
                .setMax(10.0F)
                .setSaveConsumer(value -> config.sv_airaccelerate = value)
                .build());
        general.addEntry(entries.startFloatField(
                        Text.translatable("text.autoconfig.bhops.option.sv_maxairspeed"),
                        config.sv_maxairspeed
                )
                .setDefaultValue(0.08F)
                .setMin(0.0F)
                .setMax(10.0F)
                .setSaveConsumer(value -> config.sv_maxairspeed = value)
                .build());
        general.addEntry(entries.startFloatField(
                        Text.translatable("text.autoconfig.bhops.option.maxSpeedMul"),
                        config.maxSpeedMul
                )
                .setDefaultValue(2.2F)
                .setMin(0.0F)
                .setMax(100.0F)
                .setSaveConsumer(value -> config.maxSpeedMul = value)
                .build());

        return builder.build();
    }
}

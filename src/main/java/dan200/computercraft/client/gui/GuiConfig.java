package dan200.computercraft.client.gui;

import java.util.Arrays;

import dan200.computercraft.core.apis.http.websocket.Websocket;
import dan200.computercraft.shared.util.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public final class GuiConfig {
    private GuiConfig() {}

    @SuppressWarnings ({
        "LocalVariableDeclarationSideOnly",
        "MethodCallSideOnly"
    })
    public static Screen getScreen(Screen parentScreen) {
        Config config = Config.get();
        ConfigBuilder builder = ConfigBuilder.create()
                                             .setParentScreen(parentScreen)
                                             .setTitle(new TranslatableText("gui.computercraft.config.title"))
                                             .setSavingRunnable(() -> {
                                                 Config.save();
                                                 Config.sync();
                                             });

        ConfigEntryBuilder entryBuilder = ConfigEntryBuilder.create();

        builder.getOrCreateCategory(key("general"))

               .addEntry(entryBuilder.startIntField(key("computer_space_limit"), config.general.computer_space_limit)
                                     .setSaveConsumer(v -> config.general.computer_space_limit = v)
                                     .setDefaultValue(Config.defaultConfig.general.computer_space_limit)
                                     .build())

               .addEntry(entryBuilder.startIntField(key("floppy_space_limit"), config.general.floppy_space_limit)
                                     .setSaveConsumer(v -> config.general.floppy_space_limit = v)
                                     .setDefaultValue(Config.defaultConfig.general.floppy_space_limit)
                                     .build())

               .addEntry(entryBuilder.startIntField(key("maximum_open_files"), config.general.maximum_open_files)
                                     .setSaveConsumer(v -> config.general.maximum_open_files = v)
                                     .setDefaultValue(Config.defaultConfig.general.maximum_open_files)
                                     .setMin(0)
                                     .build())

               .addEntry(entryBuilder.startBooleanToggle(key("disable_lua51_features"), config.general.disable_lua51_features)
                                     .setSaveConsumer(v -> config.general.disable_lua51_features = v)
                                     .setDefaultValue(Config.defaultConfig.general.disable_lua51_features)
                                     .build())

               .addEntry(entryBuilder.startStrField(key("default_computer_settings"), config.general.default_computer_settings)
                                     .setSaveConsumer(v -> config.general.default_computer_settings = v)
                                     .setDefaultValue(Config.defaultConfig.general.default_computer_settings)
                                     .build())

               .addEntry(entryBuilder.startBooleanToggle(key("debug_enabled"), config.general.debug_enabled)
                                     .setSaveConsumer(v -> config.general.debug_enabled = v)
                                     .setDefaultValue(Config.defaultConfig.general.debug_enabled)
                                     .build())

               .addEntry(entryBuilder.startBooleanToggle(key("log_computer_errors"), config.general.log_computer_errors)
                                     .setSaveConsumer(v -> config.general.log_computer_errors = v)
                                     .setDefaultValue(Config.defaultConfig.general.log_computer_errors)
                                     .build());

        builder.getOrCreateCategory(key("execution"))

               .addEntry(entryBuilder.startIntField(key("execution.computer_threads"), config.execution.computer_threads)
                                     .setSaveConsumer(v -> config.execution.computer_threads = v)
                                     .setDefaultValue(Config.defaultConfig.execution.computer_threads)
                                     .setMin(1)
                                     .requireRestart()
                                     .build())

               .addEntry(entryBuilder.startLongField(key("execution.max_main_global_time"), config.execution.max_main_global_time)
                                     .setSaveConsumer(v -> config.execution.max_main_global_time = v)
                                     .setDefaultValue(Config.defaultConfig.execution.max_main_global_time)
                                     .setMin(1)
                                     .build())

               .addEntry(entryBuilder.startLongField(key("execution.max_main_computer_time"), config.execution.max_main_computer_time)
                                     .setSaveConsumer(v -> config.execution.max_main_computer_time = v)
                                     .setDefaultValue(Config.defaultConfig.execution.max_main_computer_time)
                                     .setMin(1)
                                     .build());

        builder.getOrCreateCategory(key("http"))

               .addEntry(entryBuilder.startBooleanToggle(key("http.enabled"), config.http.enabled)
                                     .setSaveConsumer(v -> config.http.enabled = v)
                                     .setDefaultValue(Config.defaultConfig.http.enabled)
                                     .build())

               .addEntry(entryBuilder.startBooleanToggle(key("http.websocket_enabled"), config.http.websocket_enabled)
                                     .setSaveConsumer(v -> config.http.websocket_enabled = v)
                                     .setDefaultValue(Config.defaultConfig.http.websocket_enabled)
                                     .build())

               .addEntry(entryBuilder.startStrList(key("http.whitelist"), Arrays.asList(config.http.whitelist))
                                     .setSaveConsumer(v -> config.http.whitelist = v.toArray(new String[0]))
                                     .setDefaultValue(Arrays.asList(Config.defaultConfig.http.whitelist))
                                     .build())

               .addEntry(entryBuilder.startStrList(key("http.blacklist"), Arrays.asList(config.http.blacklist))
                                     .setSaveConsumer(v -> config.http.blacklist = v.toArray(new String[0]))
                                     .setDefaultValue(Arrays.asList(Config.defaultConfig.http.blacklist))
                                     .build())

               .addEntry(entryBuilder.startIntField(key("http.timeout"), config.http.timeout)
                                     .setSaveConsumer(v -> config.http.timeout = v)
                                     .setDefaultValue(Config.defaultConfig.http.timeout)
                                     .setMin(0)
                                     .build())

               .addEntry(entryBuilder.startIntField(key("http.max_requests"), config.http.max_requests)
                                     .setSaveConsumer(v -> config.http.max_requests = v)
                                     .setDefaultValue(Config.defaultConfig.http.max_requests)
                                     .setMin(0)
                                     .build())

               .addEntry(entryBuilder.startLongField(key("http.max_download"), config.http.max_download)
                                     .setSaveConsumer(v -> config.http.max_download = v)
                                     .setDefaultValue(Config.defaultConfig.http.max_download)
                                     .setMin(0)
                                     .build())

               .addEntry(entryBuilder.startLongField(key("http.max_upload"), config.http.max_upload)
                                     .setSaveConsumer(v -> config.http.max_upload = v)
                                     .setDefaultValue(Config.defaultConfig.http.max_upload)
                                     .setMin(0)
                                     .build())

               .addEntry(entryBuilder.startIntField(key("http.max_websockets"), config.http.max_websockets)
                                     .setSaveConsumer(v -> config.http.max_websockets = v)
                                     .setDefaultValue(Config.defaultConfig.http.max_websockets)
                                     .setMin(1)
                                     .build())

               .addEntry(entryBuilder.startIntField(key("http.max_websocket_message"), config.http.max_websocket_message)
                                     .setSaveConsumer(v -> config.http.max_websocket_message = v)
                                     .setDefaultValue(Config.defaultConfig.http.max_websocket_message)
                                     .setMin(0)
                                     .setMax(Websocket.MAX_MESSAGE_SIZE)
                                     .build());

        builder.getOrCreateCategory(key("peripheral"))

               .addEntry(entryBuilder.startBooleanToggle(key("peripheral.command_block_enabled"), config.peripheral.command_block_enabled)
                                     .setSaveConsumer(v -> config.peripheral.command_block_enabled = v)
                                     .setDefaultValue(Config.defaultConfig.peripheral.command_block_enabled)
                                     .build())

               .addEntry(entryBuilder.startIntField(key("peripheral.modem_range"), config.peripheral.modem_range)
                                     .setSaveConsumer(v -> config.peripheral.modem_range = v)
                                     .setDefaultValue(Config.defaultConfig.peripheral.modem_range)
                                     .setMin(0)
                                     .setMax(Config.MODEM_MAX_RANGE)
                                     .build())

               .addEntry(entryBuilder.startIntField(key("peripheral.modem_high_altitude_range"), config.peripheral.modem_high_altitude_range)
                                     .setSaveConsumer(v -> config.peripheral.modem_high_altitude_range = v)
                                     .setDefaultValue(Config.defaultConfig.peripheral.modem_high_altitude_range)
                                     .setMin(0)
                                     .setMax(Config.MODEM_MAX_RANGE)
                                     .build())

               .addEntry(entryBuilder.startIntField(key("peripheral.modem_range_during_storm"), config.peripheral.modem_range_during_storm)
                                     .setSaveConsumer(v -> config.peripheral.modem_range_during_storm = v)
                                     .setDefaultValue(Config.defaultConfig.peripheral.modem_range_during_storm)
                                     .setMin(0)
                                     .setMax(Config.MODEM_MAX_RANGE)
                                     .build())

               .addEntry(entryBuilder.startIntField(key("peripheral.modem_high_altitude_range_during_storm"),
                                                    config.peripheral.modem_high_altitude_range_during_storm)
                                     .setSaveConsumer(v -> config.peripheral.modem_high_altitude_range_during_storm = v)
                                     .setDefaultValue(Config.defaultConfig.peripheral.modem_high_altitude_range_during_storm)
                                     .setMin(0)
                                     .setMax(Config.MODEM_MAX_RANGE)
                                     .build())

               .addEntry(entryBuilder.startIntField(key("peripheral.max_notes_per_tick"), config.peripheral.max_notes_per_tick)
                                     .setSaveConsumer(v -> config.peripheral.max_notes_per_tick = v)
                                     .setDefaultValue(Config.defaultConfig.peripheral.max_notes_per_tick)
                                     .setMin(1)
                                     .build());

        builder.getOrCreateCategory(key("turtle"))

               .addEntry(entryBuilder.startBooleanToggle(key("turtle.need_fuel"), config.turtle.need_fuel)
                                     .setSaveConsumer(v -> config.turtle.need_fuel = v)
                                     .setDefaultValue(Config.defaultConfig.turtle.need_fuel)
                                     .build())

               .addEntry(entryBuilder.startIntField(key("turtle.normal_fuel_limit"), config.turtle.normal_fuel_limit)
                                     .setSaveConsumer(v -> config.turtle.normal_fuel_limit = v)
                                     .setDefaultValue(Config.defaultConfig.turtle.normal_fuel_limit)
                                     .setMin(0)
                                     .build())

               .addEntry(entryBuilder.startIntField(key("turtle.advanced_fuel_limit"), config.turtle.advanced_fuel_limit)
                                     .setSaveConsumer(v -> config.turtle.advanced_fuel_limit = v)
                                     .setDefaultValue(Config.defaultConfig.turtle.advanced_fuel_limit)
                                     .setMin(0)
                                     .build())

               .addEntry(entryBuilder.startBooleanToggle(key("turtle.obey_block_protection"), config.turtle.obey_block_protection)
                                     .setSaveConsumer(v -> config.turtle.obey_block_protection = v)
                                     .setDefaultValue(Config.defaultConfig.turtle.obey_block_protection)
                                     .build())

               .addEntry(entryBuilder.startStrList(key("turtle.disabled_actions"), Arrays.asList(config.turtle.disabled_actions))
                                     .setSaveConsumer(v -> config.turtle.disabled_actions = v.toArray(new String[0]))
                                     .setDefaultValue(Arrays.asList(Config.defaultConfig.turtle.disabled_actions))
                                     .build());

        return builder.build();
    }

    private static Text key(String name) {
        return new TranslatableText("gui.computercraft.config." + name);
    }
}

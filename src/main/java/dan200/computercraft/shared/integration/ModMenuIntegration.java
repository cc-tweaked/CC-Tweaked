/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import dan200.computercraft.shared.util.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

// A poor mod menu integration just for testing the monitor rendering changes we've been making :)

@Environment( EnvType.CLIENT )
public class ModMenuIntegration implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create().setParentScreen( parent )
                .setTitle( new TextComponent( "Computer Craft" ) )
                .setSavingRunnable( () -> {
                    Config.clientSpec.correct( Config.clientConfig );
                    Config.sync();
                    Config.save();
                    ComputerCraft.log.info( "Monitor renderer: {}", ComputerCraft.monitorRenderer );
                } );

            ConfigCategory client = builder.getOrCreateCategory( new TextComponent( "Client" ) );

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            client.addEntry( entryBuilder.startEnumSelector( new TextComponent( "Monitor Renderer" ), MonitorRenderer.class, ComputerCraft.monitorRenderer )
                .setDefaultValue( MonitorRenderer.BEST )
                .setSaveConsumer( renderer -> { Config.clientConfig.set( "monitor_renderer", renderer ); } )
                .setTooltip( Component.nullToEmpty( Config.clientConfig.getComment( "monitor_renderer" ) ) )
                .build() );

            return builder.build();
        };
    }
}

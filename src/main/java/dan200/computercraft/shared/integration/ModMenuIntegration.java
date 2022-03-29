/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
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
                } );

            ConfigCategory client = builder.getOrCreateCategory( new TextComponent( "Client" ) );

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            client.addEntry( entryBuilder
                .startEnumSelector(
                    new TextComponent( "Monitor Renderer" ),
                    MonitorRenderer.class,
                    Config.clientConfig.getEnum( "monitor_renderer", MonitorRenderer.class )
                )
                .setDefaultValue( MonitorRenderer.BEST )
                .setSaveConsumer( renderer -> Config.clientConfig.set( "monitor_renderer", renderer ) )
                .setTooltip( Component.nullToEmpty( rewrapComment( Config.clientConfig.getComment( "monitor_renderer" ) ) ) )
                .build() );

            return builder.build();
        };
    }

    private static String rewrapComment( String comment )
    {
        String[] words = comment.strip().replaceAll( "[\r\n]", "" ).split( " " );

        StringBuilder builder = new StringBuilder();
        int lineLength = 0;
        for( String word : words )
        {
            int wordLength = word.length();

            if( lineLength + wordLength + 1 > 50 )
            {
                builder.append( "\n" );
                lineLength = 0;
                builder.append( word );
                lineLength += wordLength;
            }
            else
            {
                if( builder.length() == 0 )
                {
                    builder.append( word );
                    lineLength += wordLength;
                }
                else
                {
                    builder.append( " " );
                    builder.append( word );
                    lineLength += wordLength + 1;
                }
            }
        }

        return new String( builder );
    }
}

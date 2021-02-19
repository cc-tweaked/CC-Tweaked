/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import com.mojang.brigadier.CommandDispatcher;
import dan200.computercraft.utils.Copier;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.UncheckedIOException;

import static dan200.computercraft.shared.command.builder.HelpingArgumentBuilder.choice;
import static net.minecraft.command.Commands.literal;

/**
 * Helper commands for importing/exporting the computer directory.
 */
class CCTestCommand
{
    public static void register( CommandDispatcher<CommandSource> dispatcher )
    {
        dispatcher.register( choice( "cctest" )
            .then( literal( "import" ).executes( context -> {
                importFiles( context.getSource().getServer() );
                return 0;
            } ) )
            .then( literal( "export" ).executes( context -> {
                exportFiles( context.getSource().getServer() );
                return 0;
            } ) )
        );
    }

    public static void importFiles( MinecraftServer server )
    {
        try
        {
            Copier.replicate( TestMod.sourceDir.resolve( "computers" ), server.getServerDirectory().toPath().resolve( "world/computercraft" ) );
        }
        catch( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }

    public static void exportFiles( MinecraftServer server )
    {
        try
        {
            Copier.replicate( server.getServerDirectory().toPath().resolve( "world/computercraft" ), TestMod.sourceDir.resolve( "computers" ) );
        }
        catch( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }
}

/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.data.NBTToSNBTConverter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.test.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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

                Path path = Paths.get( StructureHelper.testStructuresDir );
                int total = 0;
                for( TestFunctionInfo function : TestRegistry.getAllTestFunctions() )
                {
                    ResourceLocation resourcelocation = new ResourceLocation( "minecraft", function.getStructureName() );
                    Path input = context.getSource().getLevel().getStructureManager().createPathToStructure( resourcelocation, ".nbt" );
                    Path output = NBTToSNBTConverter.convertStructure( input, function.getStructureName(), path );
                    if( output != null ) total++;
                }
                return total;
            } ) )
            .then( literal( "runall" ).executes( context -> {
                TestRegistry.forgetFailedTests();
                TestResultList result = TestHooks.runTests();
                result.addListener( new Callback( context.getSource(), result ) );
                result.addFailureListener( x -> TestRegistry.rememberFailedTest( x.getTestFunction() ) );
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

    private static class Callback implements ITestCallback
    {
        private final CommandSource source;
        private final TestResultList result;

        public Callback( CommandSource source, TestResultList result )
        {
            this.source = source;
            this.result = result;
        }

        @Override
        public void testStructureLoaded( @Nonnull TestTracker tracker )
        {
        }

        @Override
        public void testFailed( @Nonnull TestTracker tracker )
        {
            if( !tracker.isDone() ) return;

            source.sendFailure( new StringTextComponent( result.getFailedRequiredCount() + " required tests failed" ).withStyle( TextFormatting.RED ) );
        }
    }
}

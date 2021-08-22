/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import com.mojang.brigadier.CommandDispatcher;
import dan200.computercraft.ComputerCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.data.NBTToSNBTConverter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.test.*;
import net.minecraft.tileentity.StructureBlockTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.fml.loading.FMLLoader;

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

            .then( literal( "promote" ).executes( context -> {
                if( !FMLLoader.getDist().isClient() ) return error( context.getSource(), "Cannot run on server" );

                promote();
                return 0;
            } ) )
            .then( literal( "marker" ).executes( context -> {
                ServerPlayerEntity player = context.getSource().getPlayerOrException();
                BlockPos pos = StructureHelper.findNearestStructureBlock( player.blockPosition(), 15, player.getLevel() );
                if( pos == null ) return error( context.getSource(), "No nearby test" );

                StructureBlockTileEntity structureBlock = (StructureBlockTileEntity) player.getLevel().getBlockEntity( pos );
                TestFunctionInfo info = TestRegistry.getTestFunction( structureBlock.getStructurePath() );

                // Kill the existing armor stand
                player
                    .getLevel().getEntities()
                    .filter( x -> x.isAlive() && x instanceof ArmorStandEntity && x.getName().getString().equals( info.getTestName() ) )
                    .forEach( Entity::remove );

                // And create a new one
                CompoundNBT nbt = new CompoundNBT();
                nbt.putBoolean( "Marker", true );
                nbt.putBoolean( "Invisible", true );
                ArmorStandEntity armorStand = EntityType.ARMOR_STAND.create( player.getLevel() );
                armorStand.readAdditionalSaveData( nbt );
                armorStand.copyPosition( player );
                armorStand.setCustomName( new StringTextComponent( info.getTestName() ) );
                return 0;
            } ) )
        );
    }

    public static void importFiles( MinecraftServer server )
    {
        try
        {
            Copier.replicate( TestMod.sourceDir.resolve( "computers" ), server.getWorldPath( new FolderName( ComputerCraft.MOD_ID ) ) );
        }
        catch( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }

    static void exportFiles( MinecraftServer server )
    {
        try
        {
            Copier.replicate( server.getWorldPath( new FolderName( ComputerCraft.MOD_ID ) ), TestMod.sourceDir.resolve( "computers" ) );
        }
        catch( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }

    private static void promote()
    {
        try
        {
            Copier.replicate(
                Minecraft.getInstance().gameDirectory.toPath().resolve( "screenshots" ),
                TestMod.sourceDir.resolve( "screenshots" ),
                x -> !x.toFile().getName().endsWith( ".diff.png" )
            );
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

        Callback( CommandSource source, TestResultList result )
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

            error( source, result.getFailedRequiredCount() + " required tests failed" );
        }
    }

    private static int error( CommandSource source, String message )
    {
        source.sendFailure( new StringTextComponent( message ).withStyle( TextFormatting.RED ) );
        return 0;
    }
}

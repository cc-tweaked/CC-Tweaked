/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest.core;

import com.mojang.brigadier.CommandDispatcher;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.mixin.gametest.TestCommandAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import static dan200.computercraft.shared.command.builder.HelpingArgumentBuilder.choice;
import static net.minecraft.commands.Commands.literal;

/**
 * Helper commands for importing/exporting the computer directory.
 */
class CCTestCommand
{
    public static final LevelResource LOCATION = new LevelResource( ComputerCraft.MOD_ID );

    public static void register( CommandDispatcher<CommandSourceStack> dispatcher )
    {
        dispatcher.register( choice( "cctest" )
            .then( literal( "import" ).executes( context -> {
                importFiles( context.getSource().getServer() );
                return 0;
            } ) )
            .then( literal( "export" ).executes( context -> {
                exportFiles( context.getSource().getServer() );

                for( TestFunction function : GameTestRegistry.getAllTestFunctions() )
                {
                    TestCommandAccessor.callExportTestStructure( context.getSource(), function.getStructureName() );
                }
                return 0;
            } ) )
            .then( literal( "regen-structures" ).executes( context -> {
                for( TestFunction function : GameTestRegistry.getAllTestFunctions() )
                {
                    dispatcher.execute( "test import " + function.getTestName(), context.getSource() );
                    TestCommandAccessor.callExportTestStructure( context.getSource(), function.getStructureName() );
                }
                return 0;
            } ) )

            .then( literal( "marker" ).executes( context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                BlockPos pos = StructureUtils.findNearestStructureBlock( player.blockPosition(), 15, player.getLevel() );
                if( pos == null ) return error( context.getSource(), "No nearby test" );

                StructureBlockEntity structureBlock = (StructureBlockEntity) player.getLevel().getBlockEntity( pos );
                TestFunction info = GameTestRegistry.getTestFunction( structureBlock.getStructurePath() );

                // Kill the existing armor stand
                player
                    .getLevel().getEntities( EntityType.ARMOR_STAND, x -> x.isAlive() && x.getName().getString().equals( info.getTestName() ) )
                    .forEach( Entity::kill );

                // And create a new one
                CompoundTag nbt = new CompoundTag();
                nbt.putBoolean( "Marker", true );
                nbt.putBoolean( "Invisible", true );
                ArmorStand armorStand = EntityType.ARMOR_STAND.create( player.getLevel() );
                armorStand.readAdditionalSaveData( nbt );
                armorStand.copyPosition( player );
                armorStand.setCustomName( new TextComponent( info.getTestName() ) );
                player.getLevel().addFreshEntity( armorStand );
                return 0;
            } ) )
        );
    }

    public static void importFiles( MinecraftServer server )
    {
        try
        {
            Copier.replicate( getSourceComputerPath(), getWorldComputerPath( server ) );
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
            Copier.replicate( getWorldComputerPath( server ), getSourceComputerPath() );
        }
        catch( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }

    private static Path getWorldComputerPath( MinecraftServer server )
    {
        return server.getWorldPath( LOCATION ).resolve( "computer" ).resolve( "0" );
    }

    private static Path getSourceComputerPath()
    {
        return TestHooks.sourceDir.resolve( "computer" );
    }

    private static int error( CommandSourceStack source, String message )
    {
        source.sendFailure( new TextComponent( message ).withStyle( ChatFormatting.RED ) );
        return 0;
    }
}

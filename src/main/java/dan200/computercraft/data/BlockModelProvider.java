/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.peripheral.modem.wired.BlockWiredModemFull;
import dan200.computercraft.shared.peripheral.modem.wireless.BlockWirelessModem;
import dan200.computercraft.shared.peripheral.monitor.BlockMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;
import java.util.Objects;

class BlockModelProvider extends BlockStateProvider
{
    private ModelFile monitorBase;
    private ModelFile turtleBase;
    private ModelFile modemBase;

    BlockModelProvider( DataGenerator generator, ExistingFileHelper existingFileHelper )
    {
        super( generator, ComputerCraft.MOD_ID, existingFileHelper );
    }

    @Nonnull
    @Override
    public String getName()
    {
        return "Block states and models";
    }

    @Override
    protected void registerStatesAndModels()
    {
        monitorBase = models().getExistingFile( new ResourceLocation( ComputerCraft.MOD_ID, "block/monitor_base" ) );
        turtleBase = models().getExistingFile( new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_base" ) );
        modemBase = models().getExistingFile( new ResourceLocation( ComputerCraft.MOD_ID, "block/modem" ) );

        registerComputer( Registry.ModBlocks.COMPUTER_NORMAL.get() );
        registerComputer( Registry.ModBlocks.COMPUTER_ADVANCED.get() );
        registerComputer( Registry.ModBlocks.COMPUTER_COMMAND.get() );

        registerTurtle( Registry.ModBlocks.TURTLE_NORMAL.get() );
        registerTurtle( Registry.ModBlocks.TURTLE_ADVANCED.get() );

        registerWirelessModem( Registry.ModBlocks.WIRELESS_MODEM_NORMAL.get() );
        registerWirelessModem( Registry.ModBlocks.WIRELESS_MODEM_ADVANCED.get() );

        registerWiredModems();

        registerMonitors( Registry.ModBlocks.MONITOR_NORMAL.get() );
        registerMonitors( Registry.ModBlocks.MONITOR_ADVANCED.get() );

        // Register the simple things.
        ModelFile speaker = models().orientable(
            name( Registry.ModBlocks.SPEAKER.get() ),
            blockTexture( Registry.ModBlocks.SPEAKER.get(), "_side" ),
            blockTexture( Registry.ModBlocks.SPEAKER.get(), "_front" ),
            blockTexture( Registry.ModBlocks.SPEAKER.get(), "_top" )
        );
        horizontalBlock( Registry.ModBlocks.SPEAKER.get(), speaker );
        simpleBlockItem( Registry.ModBlocks.SPEAKER.get(), speaker );
    }

    private void registerComputer( BlockComputer<?> block )
    {
        VariantBlockStateBuilder builder = getVariantBuilder( block );
        for( ComputerState state : BlockComputer.STATE.getPossibleValues() )
        {
            BlockModelBuilder model = models().orientable(
                extendedName( block, "_" + state ),
                blockTexture( block, "_side" ),
                blockTexture( block, "_front" + state.getTexture() ),
                blockTexture( block, "_top" )
            );

            for( Direction facing : BlockComputer.FACING.getPossibleValues() )
            {
                builder.partialState()
                    .with( BlockComputer.STATE, state )
                    .with( BlockComputer.FACING, facing )
                    .addModels( new ConfiguredModel( model, 0, toYAngle( facing ), false ) );
            }
        }

        simpleBlockItem( block, models().getBuilder( extendedName( block, "_blinking" ) ) );
    }

    private void registerTurtle( BlockTurtle block )
    {
        VariantBlockStateBuilder builder = getVariantBuilder( block );
        BlockModelBuilder base = models()
            .getBuilder( extendedName( block, "_base" ) )
            .parent( turtleBase )
            .texture( "texture", blockTexture( block ) );

        BlockModelBuilder model = models()
            .getBuilder( name( block ) )
            .customLoader( BasicCustomLoader.makeFactory( new ResourceLocation( ComputerCraft.MOD_ID, "turtle" ),
                x -> x.addProperty( "model", base.getLocation().toString() ) ) )
            .end();

        for( Direction facing : BlockTurtle.FACING.getPossibleValues() )
        {
            builder.partialState()
                .with( BlockTurtle.FACING, facing )
                .addModels( new ConfiguredModel( model, 0, toYAngle( facing ), false ) );
        }

        simpleBlockItem( block, models().getBuilder( name( block ) ) );
    }

    private void registerWirelessModem( BlockWirelessModem block )
    {
        VariantBlockStateBuilder builder = getVariantBuilder( block );

        for( boolean on : BlockWirelessModem.ON.getPossibleValues() )
        {
            ModelFile model = modemModel( extendedName( block, on ? "_on" : "_off" ), blockTexture( block, "_face" + (on ? "_on" : "") ) );

            for( Direction facing : BlockWirelessModem.FACING.getPossibleValues() )
            {
                builder.partialState()
                    .with( BlockWirelessModem.FACING, facing )
                    .with( BlockWirelessModem.ON, on )
                    .addModels( new ConfiguredModel( model, toXAngle( facing ), toYAngle( facing ), false ) );
            }
        }

        simpleBlockItem( block, models().getBuilder( extendedName( block, "_off" ) ) );
    }

    private void registerWiredModems()
    {
        Block fullBlock = Registry.ModBlocks.WIRED_MODEM_FULL.get();
        VariantBlockStateBuilder fullBlockState = getVariantBuilder( fullBlock );
        for( boolean on : BlockWiredModemFull.MODEM_ON.getPossibleValues() )
        {
            for( boolean peripheral : BlockWiredModemFull.PERIPHERAL_ON.getPossibleValues() )
            {
                String suffix = (on ? "_on" : "_off") + (peripheral ? "_peripheral" : "");
                ResourceLocation faceTexture = new ResourceLocation(
                    ComputerCraft.MOD_ID,
                    "block/wired_modem_face" + (peripheral ? "_peripheral" : "") + (on ? "_on" : "")
                );
                ModelFile fullBlockModel = models().cubeAll( blockTexture( fullBlock, suffix ).toString(), faceTexture );
                fullBlockState.partialState()
                    .with( BlockWiredModemFull.MODEM_ON, on )
                    .with( BlockWiredModemFull.PERIPHERAL_ON, peripheral )
                    .addModels( new ConfiguredModel( fullBlockModel ) );

                modemModel( "wired_modem" + suffix, faceTexture );
            }
        }

        simpleBlockItem( fullBlock, models().getBuilder( extendedName( fullBlock, "_off" ) ) );
        itemModels()
            .getBuilder( name( Registry.ModItems.WIRED_MODEM.get() ) )
            .parent( models().getBuilder( "wired_modem_off" ) );
    }

    private ModelFile modemModel( String name, ResourceLocation texture )
    {
        return models()
            .getBuilder( name )
            .parent( modemBase )
            .texture( "front", texture )
            .texture( "back", new ResourceLocation( ComputerCraft.MOD_ID, "block/modem_back" ) );
    }

    private void registerMonitors( BlockMonitor block )
    {
        String name = blockTexture( block ).toString();
        monitorModel( name, "", 16, 4, 0, 32 );
        monitorModel( name, "_d", 20, 7, 0, 36 );
        monitorModel( name, "_l", 19, 4, 1, 33 );
        monitorModel( name, "_ld", 31, 7, 1, 45 );
        monitorModel( name, "_lr", 18, 4, 2, 34 );
        monitorModel( name, "_lrd", 30, 7, 2, 46 );
        monitorModel( name, "_lru", 24, 5, 2, 40 );
        monitorModel( name, "_lrud", 27, 6, 2, 43 );
        monitorModel( name, "_lu", 25, 5, 1, 39 );
        monitorModel( name, "_lud", 28, 6, 1, 42 );
        monitorModel( name, "_r", 17, 4, 3, 35 );
        monitorModel( name, "_rd", 29, 7, 3, 47 );
        monitorModel( name, "_ru", 23, 5, 3, 41 );
        monitorModel( name, "_rud", 26, 6, 3, 44 );
        monitorModel( name, "_u", 22, 5, 0, 38 );
        monitorModel( name, "_ud", 21, 6, 0, 37 );

        VariantBlockStateBuilder builder = getVariantBuilder( block );
        for( MonitorEdgeState edge : BlockMonitor.STATE.getPossibleValues() )
        {
            String suffix = edge == MonitorEdgeState.NONE ? "" : "_" + edge.getSerializedName();
            ModelFile model = models().getBuilder( extend( block.getRegistryName(), suffix ) );

            for( Direction facing : BlockMonitor.FACING.getPossibleValues() )
            {
                for( Direction orientation : BlockMonitor.ORIENTATION.getPossibleValues() )
                {
                    builder.partialState()
                        .with( BlockMonitor.STATE, edge )
                        .with( BlockMonitor.FACING, facing )
                        .with( BlockMonitor.ORIENTATION, orientation )
                        .addModels( new ConfiguredModel( model, toXAngle( orientation ), toYAngle( facing ), false ) );
                }
            }
        }

        simpleBlockItem( block, models().orientable(
            extendedName( block, "_item" ),
            blockTexture( block, "_4" ),
            blockTexture( block, "_15" ),
            blockTexture( block, "_0" )
        ) );
    }

    private void monitorModel( String prefix, String corners, int front, int side, int top, int back )
    {
        String texturePrefix = prefix + "_";
        models().getBuilder( prefix + corners )
            .parent( monitorBase )
            .texture( "front", texturePrefix + front )
            .texture( "side", texturePrefix + side )
            .texture( "top", texturePrefix + top )
            .texture( "back", texturePrefix + back );
    }

    private static int toXAngle( Direction direction )
    {
        switch( direction )
        {
            default:
                return 0;
            case UP:
                return 270;
            case DOWN:
                return 90;
        }
    }

    private static int toYAngle( Direction direction )
    {
        return ((int) direction.toYRot() + 180) % 360;
    }

    private static ResourceLocation blockTexture( Block block, String suffix )
    {
        ResourceLocation id = block.getRegistryName();
        return new ResourceLocation( id.getNamespace(), "block/" + id.getPath() + suffix );
    }

    @Nonnull
    private String name( @Nonnull IForgeRegistryEntry<?> term )
    {
        return Objects.requireNonNull( term.getRegistryName() ).toString();
    }

    @Nonnull
    private String extendedName( @Nonnull IForgeRegistryEntry<?> term, @Nonnull String suffix )
    {
        return extend( Objects.requireNonNull( term.getRegistryName() ), suffix );
    }

    @Nonnull
    private String extend( @Nonnull ResourceLocation location, @Nonnull String suffix )
    {
        return new ResourceLocation( location.getNamespace(), location.getPath() + suffix ).toString();
    }
}

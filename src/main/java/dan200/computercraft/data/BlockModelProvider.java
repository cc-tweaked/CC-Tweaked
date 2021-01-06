/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.peripheral.monitor.BlockMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.*;

import javax.annotation.Nonnull;

public class BlockModelProvider extends BlockStateProvider
{
    private final ModelFile monitorBase;
    private final ModelFile orientable;

    public BlockModelProvider( DataGenerator generator, ExistingFileHelper existingFileHelper )
    {
        super( generator, ComputerCraft.MOD_ID, existingFileHelper );
        monitorBase = models().getExistingFile( new ResourceLocation( ComputerCraft.MOD_ID, "block/monitor_base" ) );
        orientable = models().getExistingFile( new ResourceLocation( "block/orientable" ) );
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
        registerMonitors( Registry.ModBlocks.MONITOR_NORMAL.get() );
        registerMonitors( Registry.ModBlocks.MONITOR_ADVANCED.get() );

        registerComputer( Registry.ModBlocks.COMPUTER_NORMAL.get() );
        registerComputer( Registry.ModBlocks.COMPUTER_ADVANCED.get() );
        registerComputer( Registry.ModBlocks.COMPUTER_COMMAND.get() );
    }

    private void registerComputer( BlockComputer block )
    {
        VariantBlockStateBuilder builder = getVariantBuilder( block );
        for( ComputerState state : BlockComputer.STATE.getAllowedValues() )
        {
            BlockModelBuilder model = models()
                .getBuilder( suffix( block, "_" + state ) )
                .parent( orientable )
                .texture( "top", suffix( block, "_top" ) )
                .texture( "side", suffix( block, "_side" ) )
                .texture( "front", suffix( block, "_front" + toSuffix( state ) ) );

            for( Direction facing : BlockComputer.FACING.getAllowedValues() )
            {
                builder.partialState()
                    .with( BlockComputer.STATE, state )
                    .with( BlockComputer.FACING, facing )
                    .addModels( new ConfiguredModel( model, 0, toYAngle( facing ), false ) );
            }
        }
    }

    private void registerMonitors( BlockMonitor block )
    {
        String name = block.getRegistryName().getPath();
        registerMonitorModel( name, "", 16, 4, 0, 32 );
        registerMonitorModel( name, "_d", 20, 7, 0, 36 );
        registerMonitorModel( name, "_l", 19, 4, 1, 33 );
        registerMonitorModel( name, "_ld", 31, 7, 1, 45 );
        registerMonitorModel( name, "_lr", 18, 4, 2, 34 );
        registerMonitorModel( name, "_lrd", 30, 7, 2, 46 );
        registerMonitorModel( name, "_lru", 24, 5, 2, 40 );
        registerMonitorModel( name, "_lrud", 27, 6, 2, 43 );
        registerMonitorModel( name, "_lu", 25, 5, 1, 39 );
        registerMonitorModel( name, "_lud", 28, 6, 1, 42 );
        registerMonitorModel( name, "_r", 17, 4, 3, 35 );
        registerMonitorModel( name, "_rd", 29, 7, 3, 47 );
        registerMonitorModel( name, "_ru", 23, 5, 3, 41 );
        registerMonitorModel( name, "_rud", 26, 6, 3, 44 );
        registerMonitorModel( name, "_u", 22, 5, 0, 38 );
        registerMonitorModel( name, "_ud", 21, 6, 0, 37 );

        VariantBlockStateBuilder builder = getVariantBuilder( block );
        for( MonitorEdgeState edge : BlockMonitor.STATE.getAllowedValues() )
        {
            String suffix = edge == MonitorEdgeState.NONE ? "" : "_" + edge.getName();
            ModelFile model = models().getBuilder( suffix( block, suffix ) );

            for( Direction facing : BlockMonitor.FACING.getAllowedValues() )
            {
                for( Direction orientation : BlockMonitor.ORIENTATION.getAllowedValues() )
                {
                    builder.partialState()
                        .with( BlockMonitor.STATE, edge )
                        .with( BlockMonitor.FACING, facing )
                        .with( BlockMonitor.ORIENTATION, orientation )
                        .addModels( new ConfiguredModel( model, toXAngle( orientation ), toYAngle( facing ), false ) );
                }
            }
        }
    }

    private void registerMonitorModel( String prefix, String corners, int front, int side, int top, int back )
    {
        String texturePrefix = ComputerCraft.MOD_ID + ":block/" + prefix + "_";
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
        return ((int) direction.getHorizontalAngle() + 180) % 360;
    }

    private static String toSuffix( ComputerState state )
    {
        switch( state )
        {
            default:
            case OFF:
                return "";
            case ON:
                return "_on";
            case BLINKING:
                return "_blink";
        }
    }

    private static String suffix( Block block, String suffix )
    {
        ResourceLocation id = block.getRegistryName();
        return new ResourceLocation( id.getNamespace(), "block/" + id.getPath() + suffix ).toString();
    }
}

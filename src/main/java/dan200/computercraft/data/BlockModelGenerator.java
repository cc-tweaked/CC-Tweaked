/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.google.gson.JsonObject;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.peripheral.diskdrive.BlockDiskDrive;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.BlockWiredModemFull;
import dan200.computercraft.shared.peripheral.modem.wired.CableModemVariant;
import dan200.computercraft.shared.peripheral.modem.wireless.BlockWirelessModem;
import dan200.computercraft.shared.peripheral.monitor.BlockMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState;
import dan200.computercraft.shared.peripheral.printer.BlockPrinter;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.data.*;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.Property;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.minecraft.data.ModelTextures.getBlockTexture;
import static net.minecraft.data.ModelsResourceUtil.getModelLocation;

class BlockModelGenerator
{
    private static final ModelsUtil MONITOR_BASE = new ModelsUtil(
        Optional.of( new ResourceLocation( ComputerCraft.MOD_ID, "block/monitor_base" ) ),
        Optional.empty(),
        StockTextureAliases.FRONT, StockTextureAliases.SIDE, StockTextureAliases.TOP, StockTextureAliases.BACK
    );
    private static final ModelsUtil MODEM = new ModelsUtil(
        Optional.of( new ResourceLocation( ComputerCraft.MOD_ID, "block/modem" ) ),
        Optional.empty(),
        StockTextureAliases.FRONT, StockTextureAliases.BACK
    );
    private static final ModelsUtil TURTLE = new ModelsUtil(
        Optional.of( new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_base" ) ),
        Optional.empty(),
        StockTextureAliases.TEXTURE
    );
    private static final ModelsUtil TURTLE_UPGRADE_LEFT = new ModelsUtil(
        Optional.of( new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_upgrade_base_left" ) ),
        Optional.of( "_left" ),
        StockTextureAliases.TEXTURE
    );
    private static final ModelsUtil TURTLE_UPGRADE_RIGHT = new ModelsUtil(
        Optional.of( new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_upgrade_base_right" ) ),
        Optional.of( "_left" ),
        StockTextureAliases.TEXTURE
    );

    public static void addBlockModels( BlockModelProvider generators )
    {
        registerComputer( generators, Registry.ModBlocks.COMPUTER_NORMAL.get() );
        registerComputer( generators, Registry.ModBlocks.COMPUTER_ADVANCED.get() );
        registerComputer( generators, Registry.ModBlocks.COMPUTER_COMMAND.get() );

        registerTurtle( generators, Registry.ModBlocks.TURTLE_NORMAL.get() );
        registerTurtle( generators, Registry.ModBlocks.TURTLE_ADVANCED.get() );

        registerWirelessModem( generators, Registry.ModBlocks.WIRELESS_MODEM_NORMAL.get() );
        registerWirelessModem( generators, Registry.ModBlocks.WIRELESS_MODEM_ADVANCED.get() );

        registerWiredModems( generators );

        registerMonitor( generators, Registry.ModBlocks.MONITOR_NORMAL.get() );
        registerMonitor( generators, Registry.ModBlocks.MONITOR_ADVANCED.get() );

        generators.createHorizontallyRotatedBlock( Registry.ModBlocks.SPEAKER.get(), TexturedModel.ORIENTABLE_ONLY_TOP );
        registerDiskDrive( generators );
        registerPrinter( generators );

        registerCable( generators );

        registerTurtleUpgrade( generators, "block/turtle_crafting_table", "block/turtle_crafty_face" );
        registerTurtleUpgrade( generators, "block/turtle_speaker", "block/turtle_speaker_face" );
        registerTurtleModem( generators, "block/turtle_modem_normal", "block/wireless_modem_normal_face" );
        registerTurtleModem( generators, "block/turtle_modem_advanced", "block/wireless_modem_advanced_face" );
    }

    private static void registerDiskDrive( BlockModelProvider generators )
    {
        BlockDiskDrive diskDrive = Registry.ModBlocks.DISK_DRIVE.get();
        generators.blockStateOutput.accept( FinishedVariantBlockState.multiVariant( diskDrive )
            .with( createHorizontalFacingDispatch() )
            .with( createModelDispatch( BlockDiskDrive.STATE, value -> {
                String textureSuffix;
                switch( value )
                {
                    case EMPTY:
                        textureSuffix = "_front";
                        break;
                    case INVALID:
                        textureSuffix = "_front_rejected";
                        break;
                    case FULL:
                        textureSuffix = "_front_accepted";
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                return StockModelShapes.CUBE_ORIENTABLE.createWithSuffix(
                    diskDrive, "_" + value.getSerializedName(),
                    ModelTextures.orientableCube( diskDrive ).put( StockTextureAliases.FRONT, getBlockTexture( diskDrive, textureSuffix ) ),
                    generators.modelOutput
                );
            } ) )
        );
        generators.delegateItemModel( diskDrive, getModelLocation( diskDrive, "_empty" ) );
    }

    private static void registerPrinter( BlockModelProvider generators )
    {
        BlockPrinter printer = Registry.ModBlocks.PRINTER.get();
        generators.blockStateOutput.accept( FinishedVariantBlockState.multiVariant( printer )
            .with( createHorizontalFacingDispatch() )
            .with( createModelDispatch( BlockPrinter.TOP, BlockPrinter.BOTTOM, ( top, bottom ) -> {
                String model, texture;
                if( top && bottom )
                {
                    model = "_both_full";
                    texture = "_both_trays";
                }
                else if( top )
                {
                    model = "_top_full";
                    texture = "_top_tray";
                }
                else if( bottom )
                {
                    model = "_bottom_full";
                    texture = "_bottom_tray";
                }
                else
                {
                    texture = model = "_empty";
                }

                return StockModelShapes.CUBE_ORIENTABLE.createWithSuffix( printer, model,
                    ModelTextures.orientableCube( printer ).put( StockTextureAliases.FRONT, getBlockTexture( printer, "_front" + texture ) ),
                    generators.modelOutput
                );
            } ) )
        );
        generators.delegateItemModel( printer, getModelLocation( printer, "_empty" ) );
    }

    private static void registerComputer( BlockModelProvider generators, BlockComputer block )
    {
        generators.blockStateOutput.accept( FinishedVariantBlockState.multiVariant( block )
            .with( createHorizontalFacingDispatch() )
            .with( createModelDispatch( BlockComputer.STATE, state -> StockModelShapes.CUBE_ORIENTABLE.createWithSuffix(
                block, "_" + state.getSerializedName(),
                ModelTextures.orientableCube( block ).put( StockTextureAliases.FRONT, getBlockTexture( block, "_front" + state.getTexture() ) ),
                generators.modelOutput
            ) ) )
        );
        generators.delegateItemModel( block, getModelLocation( block, "_blinking" ) );
    }

    private static void registerTurtle( BlockModelProvider generators, BlockTurtle block )
    {
        ResourceLocation model = TURTLE.create( block, ModelTextures.defaultTexture( block ), generators.modelOutput );
        generators.blockStateOutput.accept(
            FinishedVariantBlockState.multiVariant( block, BlockModelDefinition.variant().with( BlockModelFields.MODEL, model ) )
                .with( createHorizontalFacingDispatch() )
        );

        generators.modelOutput.accept( getModelLocation( block.asItem() ), () -> {
            JsonObject out = new JsonObject();
            out.addProperty( "loader", "computercraft:turtle" );
            out.addProperty( "model", model.toString() );
            return out;
        } );
    }

    private static void registerWirelessModem( BlockModelProvider generators, BlockWirelessModem block )
    {
        generators.blockStateOutput.accept( FinishedVariantBlockState.multiVariant( block )
            .with( createFacingDispatch() )
            .with( createModelDispatch( BlockWirelessModem.ON,
                on -> modemModel( generators, getModelLocation( block, on ? "_on" : "_off" ), getBlockTexture( block, "_face" + (on ? "_on" : "") ) )
            ) ) );
        generators.delegateItemModel( block, getModelLocation( block, "_off" ) );
    }

    private static void registerWiredModems( BlockModelProvider generators )
    {
        BlockWiredModemFull fullBlock = Registry.ModBlocks.WIRED_MODEM_FULL.get();
        generators.blockStateOutput.accept( FinishedVariantBlockState.multiVariant( fullBlock )
            .with( createModelDispatch( BlockWiredModemFull.MODEM_ON, BlockWiredModemFull.PERIPHERAL_ON, ( on, peripheral ) -> {
                String suffix = (on ? "_on" : "_off") + (peripheral ? "_peripheral" : "");
                ResourceLocation faceTexture = new ResourceLocation( ComputerCraft.MOD_ID, "block/wired_modem_face" + (peripheral ? "_peripheral" : "") + (on ? "_on" : "") );

                // TODO: Do this somewhere more elegant!
                modemModel( generators, new ResourceLocation( ComputerCraft.MOD_ID, "block/wired_modem" + suffix ), faceTexture );

                return StockModelShapes.CUBE_ALL.create(
                    getModelLocation( fullBlock, suffix ),
                    new ModelTextures().put( StockTextureAliases.ALL, faceTexture ),
                    generators.modelOutput
                );
            } ) ) );

        generators.delegateItemModel( fullBlock, getModelLocation( fullBlock, "_off" ) );
        generators.delegateItemModel( Registry.ModItems.WIRED_MODEM.get(), new ResourceLocation( ComputerCraft.MOD_ID, "block/wired_modem_off" ) );
    }

    private static ResourceLocation modemModel( BlockModelProvider generators, ResourceLocation name, ResourceLocation texture )
    {
        return MODEM.create(
            name,
            new ModelTextures()
                .put( StockTextureAliases.FRONT, texture )
                .put( StockTextureAliases.BACK, new ResourceLocation( ComputerCraft.MOD_ID, "block/modem_back" ) ),
            generators.modelOutput
        );
    }

    private static void registerMonitor( BlockModelProvider generators, BlockMonitor block )
    {
        monitorModel( generators, block, "", 16, 4, 0, 32 );
        monitorModel( generators, block, "_d", 20, 7, 0, 36 );
        monitorModel( generators, block, "_l", 19, 4, 1, 33 );
        monitorModel( generators, block, "_ld", 31, 7, 1, 45 );
        monitorModel( generators, block, "_lr", 18, 4, 2, 34 );
        monitorModel( generators, block, "_lrd", 30, 7, 2, 46 );
        monitorModel( generators, block, "_lru", 24, 5, 2, 40 );
        monitorModel( generators, block, "_lrud", 27, 6, 2, 43 );
        monitorModel( generators, block, "_lu", 25, 5, 1, 39 );
        monitorModel( generators, block, "_lud", 28, 6, 1, 42 );
        monitorModel( generators, block, "_r", 17, 4, 3, 35 );
        monitorModel( generators, block, "_rd", 29, 7, 3, 47 );
        monitorModel( generators, block, "_ru", 23, 5, 3, 41 );
        monitorModel( generators, block, "_rud", 26, 6, 3, 44 );
        monitorModel( generators, block, "_u", 22, 5, 0, 38 );
        monitorModel( generators, block, "_ud", 21, 6, 0, 37 );

        generators.blockStateOutput.accept( FinishedVariantBlockState.multiVariant( block )
            .with( createHorizontalFacingDispatch() )
            .with( createVerticalFacingDispatch( BlockMonitor.ORIENTATION ) )
            .with( createModelDispatch( BlockMonitor.STATE, edge -> getModelLocation( block, edge == MonitorEdgeState.NONE ? "" : "_" + edge.getSerializedName() ) ) )
        );
        generators.delegateItemModel( block, monitorModel( generators, block, "_item", 15, 4, 0, 32 ) );
    }

    private static ResourceLocation monitorModel( BlockModelProvider generators, BlockMonitor block, String corners, int front, int side, int top, int back )
    {
        return MONITOR_BASE.create(
            getModelLocation( block, corners ),
            new ModelTextures()
                .put( StockTextureAliases.FRONT, getBlockTexture( block, "_" + front ) )
                .put( StockTextureAliases.SIDE, getBlockTexture( block, "_" + side ) )
                .put( StockTextureAliases.TOP, getBlockTexture( block, "_" + top ) )
                .put( StockTextureAliases.BACK, getBlockTexture( block, "_" + back ) ),
            generators.modelOutput
        );
    }

    private static void registerCable( BlockModelProvider generators )
    {
        FinishedMultiPartBlockState generator = FinishedMultiPartBlockState.multiPart( Registry.ModBlocks.CABLE.get() );

        // When a cable only has a neighbour in a single direction, we redirect the core to face that direction.
        ResourceLocation coreFacing = new ResourceLocation( ComputerCraft.MOD_ID, "block/cable_core_facing" );
        generator.with( // Up/Down
            IMultiPartPredicateBuilder.or(
                cableNoNeighbour( Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST ).term( BlockCable.UP, true ),
                cableNoNeighbour( Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST ).term( BlockCable.DOWN, true )
            ),
            BlockModelDefinition.variant().with( BlockModelFields.MODEL, coreFacing ).with( BlockModelFields.X_ROT, BlockModelFields.Rotation.R90 )
        );

        generator.with( // North/South and no neighbours
            IMultiPartPredicateBuilder.or(
                cableNoNeighbour( Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST ),
                cableNoNeighbour( Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST ).term( BlockCable.NORTH, true ),
                cableNoNeighbour( Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST ).term( BlockCable.SOUTH, true )
            ),
            BlockModelDefinition.variant().with( BlockModelFields.MODEL, coreFacing ).with( BlockModelFields.Y_ROT, BlockModelFields.Rotation.R0 )
        );

        generator.with( // East/West
            IMultiPartPredicateBuilder.or(
                cableNoNeighbour( Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN ).term( BlockCable.EAST, true ),
                cableNoNeighbour( Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN ).term( BlockCable.WEST, true )
            ),
            BlockModelDefinition.variant().with( BlockModelFields.MODEL, coreFacing ).with( BlockModelFields.Y_ROT, BlockModelFields.Rotation.R90 )
        );

        // Find all other possibilities and emit a "solid" core which doesn't have a facing direction.
        ResourceLocation core = new ResourceLocation( ComputerCraft.MOD_ID, "block/cable_core_any" );
        List<IMultiPartPredicateBuilder.Properties> rightAngles = new ArrayList<>();
        for( int i = 0; i < DirectionUtil.FACINGS.length; i++ )
        {
            for( int j = i; j < DirectionUtil.FACINGS.length; j++ )
            {
                if( DirectionUtil.FACINGS[i].getAxis() == DirectionUtil.FACINGS[j].getAxis() ) continue;

                rightAngles.add( new IMultiPartPredicateBuilder.Properties()
                    .term( BlockCable.CABLE, true ).term( CABLE_DIRECTIONS[i], true ).term( CABLE_DIRECTIONS[j], true )
                );
            }
        }
        generator.with( IMultiPartPredicateBuilder.or( rightAngles.toArray( new IMultiPartPredicateBuilder[0] ) ), BlockModelDefinition.variant().with( BlockModelFields.MODEL, core ) );

        // Then emit the actual cable arms
        ResourceLocation arm = new ResourceLocation( ComputerCraft.MOD_ID, "block/cable_arm" );
        for( Direction direction : DirectionUtil.FACINGS )
        {
            generator.with(
                new IMultiPartPredicateBuilder.Properties().term( CABLE_DIRECTIONS[direction.ordinal()], true ),
                BlockModelDefinition.variant()
                    .with( BlockModelFields.MODEL, arm )
                    .with( BlockModelFields.X_ROT, toXAngle( direction.getOpposite() ) )
                    .with( BlockModelFields.Y_ROT, toYAngle( direction.getOpposite() ) )
            );
        }

        // And the modems!
        for( Direction direction : DirectionUtil.FACINGS )
        {
            for( boolean on : BOOLEANS )
            {
                for( boolean peripheral : BOOLEANS )
                {
                    String suffix = (on ? "_on" : "_off") + (peripheral ? "_peripheral" : "");
                    generator.with(
                        new IMultiPartPredicateBuilder.Properties().term( BlockCable.MODEM, CableModemVariant.from( direction, on, peripheral ) ),
                        BlockModelDefinition.variant()
                            .with( BlockModelFields.MODEL, new ResourceLocation( ComputerCraft.MOD_ID, "block/wired_modem" + suffix ) )
                            .with( BlockModelFields.X_ROT, toXAngle( direction ) )
                            .with( BlockModelFields.Y_ROT, toYAngle( direction ) )
                    );
                }
            }
        }

        generators.blockStateOutput.accept( generator );
    }

    private static final BooleanProperty[] CABLE_DIRECTIONS = { BlockCable.DOWN, BlockCable.UP, BlockCable.NORTH, BlockCable.SOUTH, BlockCable.WEST, BlockCable.EAST };
    private static final boolean[] BOOLEANS = new boolean[] { false, true };

    private static IMultiPartPredicateBuilder.Properties cableNoNeighbour( Direction... directions )
    {
        IMultiPartPredicateBuilder.Properties condition = new IMultiPartPredicateBuilder.Properties().term( BlockCable.CABLE, true );
        for( Direction direction : directions ) condition.term( CABLE_DIRECTIONS[direction.ordinal()], false );
        return condition;
    }

    private static void registerTurtleUpgrade( BlockModelProvider generators, String name, String texture )
    {
        TURTLE_UPGRADE_LEFT.create(
            new ResourceLocation( ComputerCraft.MOD_ID, name + "_left" ),
            ModelTextures.defaultTexture( new ResourceLocation( ComputerCraft.MOD_ID, texture ) ),
            generators.modelOutput
        );
        TURTLE_UPGRADE_RIGHT.create(
            new ResourceLocation( ComputerCraft.MOD_ID, name + "_right" ),
            ModelTextures.defaultTexture( new ResourceLocation( ComputerCraft.MOD_ID, texture ) ),
            generators.modelOutput
        );
    }

    private static void registerTurtleModem( BlockModelProvider generators, String name, String texture )
    {
        registerTurtleUpgrade( generators, name + "_off", texture );
        registerTurtleUpgrade( generators, name + "_on", texture + "_on" );
    }

    private static BlockModelFields.Rotation toXAngle( Direction direction )
    {
        switch( direction )
        {
            default:
                return BlockModelFields.Rotation.R0;
            case UP:
                return BlockModelFields.Rotation.R270;
            case DOWN:
                return BlockModelFields.Rotation.R90;
        }
    }

    private static BlockModelFields.Rotation toYAngle( Direction direction )
    {
        switch( direction )
        {
            default:
                return BlockModelFields.Rotation.R0;
            case NORTH:
                return BlockModelFields.Rotation.R0;
            case SOUTH:
                return BlockModelFields.Rotation.R180;
            case EAST:
                return BlockModelFields.Rotation.R90;
            case WEST:
                return BlockModelFields.Rotation.R270;
        }
    }

    private static BlockStateVariantBuilder createHorizontalFacingDispatch()
    {
        BlockStateVariantBuilder.One<Direction> dispatch = BlockStateVariantBuilder.property( BlockStateProperties.HORIZONTAL_FACING );
        for( Direction direction : BlockStateProperties.HORIZONTAL_FACING.getPossibleValues() )
        {
            dispatch.select( direction, BlockModelDefinition.variant().with( BlockModelFields.Y_ROT, toYAngle( direction ) ) );
        }
        return dispatch;
    }

    private static BlockStateVariantBuilder createVerticalFacingDispatch( Property<Direction> property )
    {
        BlockStateVariantBuilder.One<Direction> dispatch = BlockStateVariantBuilder.property( property );
        for( Direction direction : property.getPossibleValues() )
        {
            dispatch.select( direction, BlockModelDefinition.variant().with( BlockModelFields.X_ROT, toXAngle( direction ) ) );
        }
        return dispatch;
    }

    private static BlockStateVariantBuilder createFacingDispatch()
    {
        BlockStateVariantBuilder.One<Direction> dispatch = BlockStateVariantBuilder.property( BlockStateProperties.FACING );
        for( Direction direction : BlockStateProperties.FACING.getPossibleValues() )
        {
            dispatch.select( direction, BlockModelDefinition.variant()
                .with( BlockModelFields.Y_ROT, toYAngle( direction ) )
                .with( BlockModelFields.X_ROT, toXAngle( direction ) )
            );
        }
        return dispatch;
    }

    private static <T extends Comparable<T>> BlockStateVariantBuilder createModelDispatch( Property<T> property, Function<T, ResourceLocation> makeModel )
    {
        BlockStateVariantBuilder.One<T> variant = BlockStateVariantBuilder.property( property );
        for( T value : property.getPossibleValues() )
        {
            variant.select( value, BlockModelDefinition.variant().with( BlockModelFields.MODEL, makeModel.apply( value ) ) );
        }
        return variant;
    }

    private static <T extends Comparable<T>, U extends Comparable<U>> BlockStateVariantBuilder createModelDispatch(
        Property<T> propertyT, Property<U> propertyU, BiFunction<T, U, ResourceLocation> makeModel
    )
    {
        BlockStateVariantBuilder.Two<T, U> variant = BlockStateVariantBuilder.properties( propertyT, propertyU );
        for( T valueT : propertyT.getPossibleValues() )
        {
            for( U valueU : propertyU.getPossibleValues() )
            {
                variant.select( valueT, valueU, BlockModelDefinition.variant().with( BlockModelFields.MODEL, makeModel.apply( valueT, valueU ) ) );
            }
        }
        return variant;
    }
}

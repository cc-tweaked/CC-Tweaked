/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.google.gson.JsonObject;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.ModRegistry;
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
import net.minecraft.core.Direction;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.blockstates.*;
import net.minecraft.data.models.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.minecraft.data.models.model.ModelLocationUtils.getModelLocation;
import static net.minecraft.data.models.model.TextureMapping.getBlockTexture;

class BlockModelGenerator {
    private static final ModelTemplate MONITOR_BASE = new ModelTemplate(
        Optional.of(new ResourceLocation(ComputerCraft.MOD_ID, "block/monitor_base")),
        Optional.empty(),
        TextureSlot.FRONT, TextureSlot.SIDE, TextureSlot.TOP, TextureSlot.BACK
    );
    private static final ModelTemplate MODEM = new ModelTemplate(
        Optional.of(new ResourceLocation(ComputerCraft.MOD_ID, "block/modem")),
        Optional.empty(),
        TextureSlot.FRONT, TextureSlot.BACK
    );
    private static final ModelTemplate TURTLE = new ModelTemplate(
        Optional.of(new ResourceLocation(ComputerCraft.MOD_ID, "block/turtle_base")),
        Optional.empty(),
        TextureSlot.TEXTURE
    );
    private static final ModelTemplate TURTLE_UPGRADE_LEFT = new ModelTemplate(
        Optional.of(new ResourceLocation(ComputerCraft.MOD_ID, "block/turtle_upgrade_base_left")),
        Optional.of("_left"),
        TextureSlot.TEXTURE
    );
    private static final ModelTemplate TURTLE_UPGRADE_RIGHT = new ModelTemplate(
        Optional.of(new ResourceLocation(ComputerCraft.MOD_ID, "block/turtle_upgrade_base_right")),
        Optional.of("_left"),
        TextureSlot.TEXTURE
    );

    public static void addBlockModels(BlockModelGenerators generators) {
        registerComputer(generators, ModRegistry.Blocks.COMPUTER_NORMAL.get());
        registerComputer(generators, ModRegistry.Blocks.COMPUTER_ADVANCED.get());
        registerComputer(generators, ModRegistry.Blocks.COMPUTER_COMMAND.get());

        registerTurtle(generators, ModRegistry.Blocks.TURTLE_NORMAL.get());
        registerTurtle(generators, ModRegistry.Blocks.TURTLE_ADVANCED.get());

        registerWirelessModem(generators, ModRegistry.Blocks.WIRELESS_MODEM_NORMAL.get());
        registerWirelessModem(generators, ModRegistry.Blocks.WIRELESS_MODEM_ADVANCED.get());

        registerWiredModems(generators);

        registerMonitor(generators, ModRegistry.Blocks.MONITOR_NORMAL.get());
        registerMonitor(generators, ModRegistry.Blocks.MONITOR_ADVANCED.get());

        generators.createHorizontallyRotatedBlock(ModRegistry.Blocks.SPEAKER.get(), TexturedModel.ORIENTABLE_ONLY_TOP);
        registerDiskDrive(generators);
        registerPrinter(generators);

        registerCable(generators);

        registerTurtleUpgrade(generators, "block/turtle_crafting_table", "block/turtle_crafty_face");
        registerTurtleUpgrade(generators, "block/turtle_speaker", "block/turtle_speaker_face");
        registerTurtleModem(generators, "block/turtle_modem_normal", "block/wireless_modem_normal_face");
        registerTurtleModem(generators, "block/turtle_modem_advanced", "block/wireless_modem_advanced_face");
    }

    private static void registerDiskDrive(BlockModelGenerators generators) {
        var diskDrive = ModRegistry.Blocks.DISK_DRIVE.get();
        generators.blockStateOutput.accept(MultiVariantGenerator.multiVariant(diskDrive)
            .with(createHorizontalFacingDispatch())
            .with(createModelDispatch(BlockDiskDrive.STATE, value -> {
                var textureSuffix = switch (value) {
                    case EMPTY -> "_front";
                    case INVALID -> "_front_rejected";
                    case FULL -> "_front_accepted";
                };
                return ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(
                    diskDrive, "_" + value.getSerializedName(),
                    TextureMapping.orientableCube(diskDrive).put(TextureSlot.FRONT, getBlockTexture(diskDrive, textureSuffix)),
                    generators.modelOutput
                );
            }))
        );
        generators.delegateItemModel(diskDrive, getModelLocation(diskDrive, "_empty"));
    }

    private static void registerPrinter(BlockModelGenerators generators) {
        var printer = ModRegistry.Blocks.PRINTER.get();
        generators.blockStateOutput.accept(MultiVariantGenerator.multiVariant(printer)
            .with(createHorizontalFacingDispatch())
            .with(createModelDispatch(BlockPrinter.TOP, BlockPrinter.BOTTOM, (top, bottom) -> {
                String model, texture;
                if (top && bottom) {
                    model = "_both_full";
                    texture = "_both_trays";
                } else if (top) {
                    model = "_top_full";
                    texture = "_top_tray";
                } else if (bottom) {
                    model = "_bottom_full";
                    texture = "_bottom_tray";
                } else {
                    texture = model = "_empty";
                }

                return ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(printer, model,
                    TextureMapping.orientableCube(printer).put(TextureSlot.FRONT, getBlockTexture(printer, "_front" + texture)),
                    generators.modelOutput
                );
            }))
        );
        generators.delegateItemModel(printer, getModelLocation(printer, "_empty"));
    }

    private static void registerComputer(BlockModelGenerators generators, BlockComputer<?> block) {
        generators.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block)
            .with(createHorizontalFacingDispatch())
            .with(createModelDispatch(BlockComputer.STATE, state -> ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(
                block, "_" + state.getSerializedName(),
                TextureMapping.orientableCube(block).put(TextureSlot.FRONT, getBlockTexture(block, "_front" + state.getTexture())),
                generators.modelOutput
            )))
        );
        generators.delegateItemModel(block, getModelLocation(block, "_blinking"));
    }

    private static void registerTurtle(BlockModelGenerators generators, BlockTurtle block) {
        var model = TURTLE.create(block, TextureMapping.defaultTexture(block), generators.modelOutput);
        generators.blockStateOutput.accept(
            MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, model))
                .with(createHorizontalFacingDispatch())
        );

        generators.modelOutput.accept(getModelLocation(block.asItem()), () -> {
            var out = new JsonObject();
            out.addProperty("loader", "computercraft:turtle");
            out.addProperty("model", model.toString());
            return out;
        });
    }

    private static void registerWirelessModem(BlockModelGenerators generators, BlockWirelessModem block) {
        generators.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block)
            .with(createFacingDispatch())
            .with(createModelDispatch(BlockWirelessModem.ON,
                on -> modemModel(generators, getModelLocation(block, on ? "_on" : "_off"), getBlockTexture(block, "_face" + (on ? "_on" : "")))
            )));
        generators.delegateItemModel(block, getModelLocation(block, "_off"));
    }

    private static void registerWiredModems(BlockModelGenerators generators) {
        var fullBlock = ModRegistry.Blocks.WIRED_MODEM_FULL.get();
        generators.blockStateOutput.accept(MultiVariantGenerator.multiVariant(fullBlock)
            .with(createModelDispatch(BlockWiredModemFull.MODEM_ON, BlockWiredModemFull.PERIPHERAL_ON, (on, peripheral) -> {
                var suffix = (on ? "_on" : "_off") + (peripheral ? "_peripheral" : "");
                var faceTexture = new ResourceLocation(ComputerCraft.MOD_ID, "block/wired_modem_face" + (peripheral ? "_peripheral" : "") + (on ? "_on" : ""));

                // TODO: Do this somewhere more elegant!
                modemModel(generators, new ResourceLocation(ComputerCraft.MOD_ID, "block/wired_modem" + suffix), faceTexture);

                return ModelTemplates.CUBE_ALL.create(
                    getModelLocation(fullBlock, suffix),
                    new TextureMapping().put(TextureSlot.ALL, faceTexture),
                    generators.modelOutput
                );
            })));

        generators.delegateItemModel(fullBlock, getModelLocation(fullBlock, "_off"));
        generators.delegateItemModel(ModRegistry.Items.WIRED_MODEM.get(), new ResourceLocation(ComputerCraft.MOD_ID, "block/wired_modem_off"));
    }

    private static ResourceLocation modemModel(BlockModelGenerators generators, ResourceLocation name, ResourceLocation texture) {
        return MODEM.create(
            name,
            new TextureMapping()
                .put(TextureSlot.FRONT, texture)
                .put(TextureSlot.BACK, new ResourceLocation(ComputerCraft.MOD_ID, "block/modem_back")),
            generators.modelOutput
        );
    }

    private static void registerMonitor(BlockModelGenerators generators, BlockMonitor block) {
        monitorModel(generators, block, "", 16, 4, 0, 32);
        monitorModel(generators, block, "_d", 20, 7, 0, 36);
        monitorModel(generators, block, "_l", 19, 4, 1, 33);
        monitorModel(generators, block, "_ld", 31, 7, 1, 45);
        monitorModel(generators, block, "_lr", 18, 4, 2, 34);
        monitorModel(generators, block, "_lrd", 30, 7, 2, 46);
        monitorModel(generators, block, "_lru", 24, 5, 2, 40);
        monitorModel(generators, block, "_lrud", 27, 6, 2, 43);
        monitorModel(generators, block, "_lu", 25, 5, 1, 39);
        monitorModel(generators, block, "_lud", 28, 6, 1, 42);
        monitorModel(generators, block, "_r", 17, 4, 3, 35);
        monitorModel(generators, block, "_rd", 29, 7, 3, 47);
        monitorModel(generators, block, "_ru", 23, 5, 3, 41);
        monitorModel(generators, block, "_rud", 26, 6, 3, 44);
        monitorModel(generators, block, "_u", 22, 5, 0, 38);
        monitorModel(generators, block, "_ud", 21, 6, 0, 37);

        generators.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block)
            .with(createHorizontalFacingDispatch())
            .with(createVerticalFacingDispatch(BlockMonitor.ORIENTATION))
            .with(createModelDispatch(BlockMonitor.STATE, edge -> getModelLocation(block, edge == MonitorEdgeState.NONE ? "" : "_" + edge.getSerializedName())))
        );
        generators.delegateItemModel(block, monitorModel(generators, block, "_item", 15, 4, 0, 32));
    }

    private static ResourceLocation monitorModel(BlockModelGenerators generators, BlockMonitor block, String corners, int front, int side, int top, int back) {
        return MONITOR_BASE.create(
            getModelLocation(block, corners),
            new TextureMapping()
                .put(TextureSlot.FRONT, getBlockTexture(block, "_" + front))
                .put(TextureSlot.SIDE, getBlockTexture(block, "_" + side))
                .put(TextureSlot.TOP, getBlockTexture(block, "_" + top))
                .put(TextureSlot.BACK, getBlockTexture(block, "_" + back)),
            generators.modelOutput
        );
    }

    private static void registerCable(BlockModelGenerators generators) {
        var generator = MultiPartGenerator.multiPart(ModRegistry.Blocks.CABLE.get());

        // When a cable only has a neighbour in a single direction, we redirect the core to face that direction.
        var coreFacing = new ResourceLocation(ComputerCraft.MOD_ID, "block/cable_core_facing");
        // Up/Down
        generator.with(
            Condition.or(
                cableNoNeighbour(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST).term(BlockCable.UP, true),
                cableNoNeighbour(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST).term(BlockCable.DOWN, true)
            ),
            Variant.variant().with(VariantProperties.MODEL, coreFacing).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
        );

        // North/South and no neighbours
        generator.with(
            Condition.or(
                cableNoNeighbour(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST),
                cableNoNeighbour(Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST).term(BlockCable.NORTH, true),
                cableNoNeighbour(Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST).term(BlockCable.SOUTH, true)
            ),
            Variant.variant().with(VariantProperties.MODEL, coreFacing).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R0)
        );

        // East/West
        generator.with(
            Condition.or(
                cableNoNeighbour(Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN).term(BlockCable.EAST, true),
                cableNoNeighbour(Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN).term(BlockCable.WEST, true)
            ),
            Variant.variant().with(VariantProperties.MODEL, coreFacing).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
        );

        // Find all other possibilities and emit a "solid" core which doesn't have a facing direction.
        var core = new ResourceLocation(ComputerCraft.MOD_ID, "block/cable_core_any");
        List<Condition.TerminalCondition> rightAngles = new ArrayList<>();
        for (var i = 0; i < DirectionUtil.FACINGS.length; i++) {
            for (var j = i; j < DirectionUtil.FACINGS.length; j++) {
                if (DirectionUtil.FACINGS[i].getAxis() == DirectionUtil.FACINGS[j].getAxis()) continue;

                rightAngles.add(new Condition.TerminalCondition()
                    .term(BlockCable.CABLE, true).term(CABLE_DIRECTIONS[i], true).term(CABLE_DIRECTIONS[j], true)
                );
            }
        }
        generator.with(Condition.or(rightAngles.toArray(new Condition[0])), Variant.variant().with(VariantProperties.MODEL, core));

        // Then emit the actual cable arms
        var arm = new ResourceLocation(ComputerCraft.MOD_ID, "block/cable_arm");
        for (var direction : DirectionUtil.FACINGS) {
            generator.with(
                new Condition.TerminalCondition().term(CABLE_DIRECTIONS[direction.ordinal()], true),
                Variant.variant()
                    .with(VariantProperties.MODEL, arm)
                    .with(VariantProperties.X_ROT, toXAngle(direction.getOpposite()))
                    .with(VariantProperties.Y_ROT, toYAngle(direction.getOpposite()))
            );
        }

        // And the modems!
        for (var direction : DirectionUtil.FACINGS) {
            for (var on : BOOLEANS) {
                for (var peripheral : BOOLEANS) {
                    var suffix = (on ? "_on" : "_off") + (peripheral ? "_peripheral" : "");
                    generator.with(
                        new Condition.TerminalCondition().term(BlockCable.MODEM, CableModemVariant.from(direction, on, peripheral)),
                        Variant.variant()
                            .with(VariantProperties.MODEL, new ResourceLocation(ComputerCraft.MOD_ID, "block/wired_modem" + suffix))
                            .with(VariantProperties.X_ROT, toXAngle(direction))
                            .with(VariantProperties.Y_ROT, toYAngle(direction))
                    );
                }
            }
        }

        generators.blockStateOutput.accept(generator);
    }

    private static final BooleanProperty[] CABLE_DIRECTIONS = { BlockCable.DOWN, BlockCable.UP, BlockCable.NORTH, BlockCable.SOUTH, BlockCable.WEST, BlockCable.EAST };
    private static final boolean[] BOOLEANS = new boolean[]{ false, true };

    private static Condition.TerminalCondition cableNoNeighbour(Direction... directions) {
        var condition = new Condition.TerminalCondition().term(BlockCable.CABLE, true);
        for (var direction : directions) condition.term(CABLE_DIRECTIONS[direction.ordinal()], false);
        return condition;
    }

    private static void registerTurtleUpgrade(BlockModelGenerators generators, String name, String texture) {
        TURTLE_UPGRADE_LEFT.create(
            new ResourceLocation(ComputerCraft.MOD_ID, name + "_left"),
            TextureMapping.defaultTexture(new ResourceLocation(ComputerCraft.MOD_ID, texture)),
            generators.modelOutput
        );
        TURTLE_UPGRADE_RIGHT.create(
            new ResourceLocation(ComputerCraft.MOD_ID, name + "_right"),
            TextureMapping.defaultTexture(new ResourceLocation(ComputerCraft.MOD_ID, texture)),
            generators.modelOutput
        );
    }

    private static void registerTurtleModem(BlockModelGenerators generators, String name, String texture) {
        registerTurtleUpgrade(generators, name + "_off", texture);
        registerTurtleUpgrade(generators, name + "_on", texture + "_on");
    }

    private static VariantProperties.Rotation toXAngle(Direction direction) {
        return switch (direction) {
            default -> VariantProperties.Rotation.R0;
            case UP -> VariantProperties.Rotation.R270;
            case DOWN -> VariantProperties.Rotation.R90;
        };
    }

    private static VariantProperties.Rotation toYAngle(Direction direction) {
        return switch (direction) {
            default -> VariantProperties.Rotation.R0;
            case NORTH -> VariantProperties.Rotation.R0;
            case SOUTH -> VariantProperties.Rotation.R180;
            case EAST -> VariantProperties.Rotation.R90;
            case WEST -> VariantProperties.Rotation.R270;
        };
    }

    private static PropertyDispatch createHorizontalFacingDispatch() {
        var dispatch = PropertyDispatch.property(BlockStateProperties.HORIZONTAL_FACING);
        for (var direction : BlockStateProperties.HORIZONTAL_FACING.getPossibleValues()) {
            dispatch.select(direction, Variant.variant().with(VariantProperties.Y_ROT, toYAngle(direction)));
        }
        return dispatch;
    }

    private static PropertyDispatch createVerticalFacingDispatch(Property<Direction> property) {
        var dispatch = PropertyDispatch.property(property);
        for (var direction : property.getPossibleValues()) {
            dispatch.select(direction, Variant.variant().with(VariantProperties.X_ROT, toXAngle(direction)));
        }
        return dispatch;
    }

    private static PropertyDispatch createFacingDispatch() {
        var dispatch = PropertyDispatch.property(BlockStateProperties.FACING);
        for (var direction : BlockStateProperties.FACING.getPossibleValues()) {
            dispatch.select(direction, Variant.variant()
                .with(VariantProperties.Y_ROT, toYAngle(direction))
                .with(VariantProperties.X_ROT, toXAngle(direction))
            );
        }
        return dispatch;
    }

    private static <T extends Comparable<T>> PropertyDispatch createModelDispatch(Property<T> property, Function<T, ResourceLocation> makeModel) {
        var variant = PropertyDispatch.property(property);
        for (var value : property.getPossibleValues()) {
            variant.select(value, Variant.variant().with(VariantProperties.MODEL, makeModel.apply(value)));
        }
        return variant;
    }

    private static <T extends Comparable<T>, U extends Comparable<U>> PropertyDispatch createModelDispatch(
        Property<T> propertyT, Property<U> propertyU, BiFunction<T, U, ResourceLocation> makeModel
    ) {
        var variant = PropertyDispatch.properties(propertyT, propertyU);
        for (var valueT : propertyT.getPossibleValues()) {
            for (var valueU : propertyU.getPossibleValues()) {
                variant.select(valueT, valueU, Variant.variant().with(VariantProperties.MODEL, makeModel.apply(valueT, valueU)));
            }
        }
        return variant;
    }
}

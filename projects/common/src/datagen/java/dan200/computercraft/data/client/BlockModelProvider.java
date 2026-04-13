// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data.client;

import com.mojang.math.Quadrant;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.client.item.model.TurtleOverlayModel;
import dan200.computercraft.client.item.model.TurtleUpgradeModel;
import dan200.computercraft.client.item.properties.TurtleShowElfOverlay;
import dan200.computercraft.client.render.TurtleBlockEntityRenderer;
import dan200.computercraft.client.turtle.TurtleOverlay;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.blocks.ComputerBlock;
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveBlock;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock;
import dan200.computercraft.shared.peripheral.modem.wired.CableModemVariant;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemFullBlock;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemBlock;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlock;
import dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState;
import dan200.computercraft.shared.peripheral.printer.PrinterBlock;
import dan200.computercraft.shared.turtle.blocks.TurtleBlock;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.renderer.item.EmptyModel;
import net.minecraft.client.renderer.item.properties.conditional.HasComponent;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.minecraft.client.data.models.BlockModelGenerators.*;
import static net.minecraft.client.data.models.model.ModelLocationUtils.getModelLocation;
import static net.minecraft.client.data.models.model.TextureMapping.getBlockTexture;

public class BlockModelProvider {
    private static final TextureSlot CURSOR = TextureSlot.create("cursor");
    private static final TextureSlot LEFT = TextureSlot.create("left");
    private static final TextureSlot RIGHT = TextureSlot.create("right");
    private static final TextureSlot BACKPACK = TextureSlot.create("backpack");

    private static final ModelTemplate COMPUTER_ON = new ModelTemplate(
        Optional.of(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/computer_on")),
        Optional.empty(),
        TextureSlot.FRONT, TextureSlot.SIDE, TextureSlot.TOP, CURSOR
    );

    private static final ModelTemplate MONITOR_BASE = new ModelTemplate(
        Optional.of(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/monitor_base")),
        Optional.empty(),
        TextureSlot.FRONT, TextureSlot.SIDE, TextureSlot.TOP, TextureSlot.BACK
    );
    private static final ModelTemplate MODEM = new ModelTemplate(
        Optional.of(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/modem")),
        Optional.empty(),
        TextureSlot.FRONT, TextureSlot.BACK
    );
    private static final ModelTemplate TURTLE = new ModelTemplate(
        Optional.of(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_base")),
        Optional.empty(),
        TextureSlot.FRONT, TextureSlot.BACK, TextureSlot.TOP, TextureSlot.BOTTOM, LEFT, RIGHT, BACKPACK
    );
    private static final ModelTemplate TURTLE_UPGRADE_LEFT = new ModelTemplate(
        Optional.of(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_upgrade_base_left")),
        Optional.of("_left"),
        TextureSlot.TEXTURE
    );
    private static final ModelTemplate TURTLE_UPGRADE_RIGHT = new ModelTemplate(
        Optional.of(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_upgrade_base_right")),
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
        registerSimpleItemModel(generators, ModRegistry.Blocks.SPEAKER.get());

        registerDiskDrive(generators);
        registerPrinter(generators);

        registerCable(generators);

        registerRedstoneControl(generators);

        registerTurtleUpgrade(generators, "block/turtle_crafting_table", "block/turtle_crafty_face");
        registerTurtleUpgrade(generators, "block/turtle_speaker", "block/turtle_speaker_face");
        registerTurtleModem(generators, "block/turtle_modem_normal", "block/wireless_modem_normal_face");
        registerTurtleModem(generators, "block/turtle_modem_advanced", "block/wireless_modem_advanced_face");

        generators.blockStateOutput.accept(
            createSimpleBlock(ModRegistry.Blocks.LECTERN.get(), plainVariant(getModelLocation(Blocks.LECTERN)))
                .with(ROTATION_HORIZONTAL_FACING)
        );
    }

    private static void registerDiskDrive(BlockModelGenerators generators) {
        var diskDrive = ModRegistry.Blocks.DISK_DRIVE.get();
        generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(diskDrive)
            .with(createModelDispatch(DiskDriveBlock.STATE, value -> {
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
            .with(ROTATION_HORIZONTAL_FACING)
        );
        generators.registerSimpleItemModel(diskDrive, getModelLocation(diskDrive, "_empty"));
    }

    private static void registerPrinter(BlockModelGenerators generators) {
        var printer = ModRegistry.Blocks.PRINTER.get();
        generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(printer)
            .with(createModelDispatch(PrinterBlock.TOP, PrinterBlock.BOTTOM, (top, bottom) -> {
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
            .with(ROTATION_HORIZONTAL_FACING)
        );
        generators.registerSimpleItemModel(printer, getModelLocation(printer, "_empty"));
    }

    private static void registerComputer(BlockModelGenerators generators, ComputerBlock<?> block) {
        generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
            .with(createModelDispatch(ComputerBlock.STATE, state -> switch (state) {
                case OFF -> ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(
                    block, "_" + state.getSerializedName(),
                    TextureMapping.orientableCube(block),
                    generators.modelOutput
                );
                case ON, BLINKING -> COMPUTER_ON.createWithSuffix(
                    block, "_" + state.getSerializedName(),
                    TextureMapping.orientableCube(block).put(CURSOR, new Material(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/computer" + state.getTexture()))),
                    generators.modelOutput
                );
            }))
            .with(ROTATION_HORIZONTAL_FACING)
        );
        generators.registerSimpleItemModel(block, getModelLocation(block, "_blinking"));
    }

    private static void registerTurtle(BlockModelGenerators generators, TurtleBlock block) {
        // The actual turtle blockstate is an empty model with just the partcles.
        var particleModel = ModelTemplates.PARTICLE_ONLY.createWithSuffix(
            block, "_particle", TextureMapping.particle(getBlockTexture(block, "_front")), generators.modelOutput
        );
        generators.blockStateOutput.accept(createSimpleBlock(block, plainVariant(particleModel)));

        // We then register the full model for use in items and the BE renderer.
        var model = TURTLE.create(block, new TextureMapping()
                .put(TextureSlot.FRONT, getBlockTexture(block, "_front"))
                .put(TextureSlot.BACK, getBlockTexture(block, "_back"))
                .put(TextureSlot.TOP, getBlockTexture(block, "_top"))
                .put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"))
                .put(LEFT, getBlockTexture(block, "_left"))
                .put(RIGHT, getBlockTexture(block, "_right"))
                .put(BACKPACK, getBlockTexture(block, "_backpack")),
            generators.modelOutput
        );

        generators.itemModelOutput.accept(block.asItem(), ItemModelUtils.composite(
            ItemModelUtils.conditional(
                new HasComponent(DataComponents.DYED_COLOR, false),
                ItemModelUtils.tintedModel(TurtleBlockEntityRenderer.COLOUR_TURTLE_MODEL, new Dye(-1)),
                ItemModelUtils.plainModel(model)
            ),
            new TurtleUpgradeModel.Unbaked(TurtleSide.LEFT, model),
            new TurtleUpgradeModel.Unbaked(TurtleSide.RIGHT, model),
            new TurtleOverlayModel.Unbaked(model),
            ItemModelUtils.isXmas(
                ItemModelUtils.conditional(TurtleShowElfOverlay.create(), ItemModelUtils.plainModel(TurtleOverlay.ELF_MODEL), new EmptyModel.Unbaked()),
                new EmptyModel.Unbaked()
            )
        ));
    }

    private static void registerWirelessModem(BlockModelGenerators generators, WirelessModemBlock block) {
        generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
            .with(createModelDispatch(WirelessModemBlock.ON,
                on -> modemModel(generators, getModelLocation(block, on ? "_on" : "_off"), getBlockTexture(block, "_face" + (on ? "_on" : "")))
            ))
            .with(ROTATION_FACING));
        generators.registerSimpleItemModel(block, getModelLocation(block, "_off"));
    }

    private static void registerWiredModems(BlockModelGenerators generators) {
        var fullBlock = ModRegistry.Blocks.WIRED_MODEM_FULL.get();
        generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(fullBlock)
            .with(createModelDispatch(WiredModemFullBlock.MODEM_ON, WiredModemFullBlock.PERIPHERAL_ON, (on, peripheral) -> {
                var suffix = (on ? "_on" : "_off") + (peripheral ? "_peripheral" : "");
                var faceTexture = new Material(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/wired_modem_face" + (peripheral ? "_peripheral" : "") + (on ? "_on" : "")));

                // TODO: Do this somewhere more elegant!
                modemModel(generators, Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/wired_modem" + suffix), faceTexture);

                return ModelTemplates.CUBE_ALL.create(
                    getModelLocation(fullBlock, suffix),
                    new TextureMapping().put(TextureSlot.ALL, faceTexture),
                    generators.modelOutput
                );
            })));

        generators.registerSimpleItemModel(fullBlock, getModelLocation(fullBlock, "_off"));
        generators.registerSimpleItemModel(ModRegistry.Items.WIRED_MODEM.get(), Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/wired_modem_off"));
    }

    private static Identifier modemModel(BlockModelGenerators generators, Identifier name, Material texture) {
        return MODEM.create(
            name,
            new TextureMapping()
                .put(TextureSlot.FRONT, texture)
                .put(TextureSlot.BACK, new Material(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/modem_back"))),
            generators.modelOutput
        );
    }

    private static void registerMonitor(BlockModelGenerators generators, MonitorBlock block) {
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

        generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
            .with(createModelDispatch(MonitorBlock.STATE, edge -> getModelLocation(block, edge == MonitorEdgeState.NONE ? "" : "_" + edge.getSerializedName())))
            .with(ROTATION_HORIZONTAL_FACING)
            .with(createVerticalFacingDispatch(MonitorBlock.ORIENTATION))
        );
        generators.registerSimpleItemModel(block, monitorModel(generators, block, "_item", 15, 4, 0, 32));
    }

    private static Identifier monitorModel(BlockModelGenerators generators, MonitorBlock block, String corners, int front, int side, int top, int back) {
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
        var coreFacing = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/cable_core_facing");
        // Up/Down
        generator.with(
            or(
                cableNoNeighbour(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST).term(CableBlock.UP, true),
                cableNoNeighbour(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST).term(CableBlock.DOWN, true)
            ),
            plainVariant(coreFacing).with(VariantMutator.X_ROT.withValue(Quadrant.R90))
        );

        // North/South and no neighbours
        generator.with(
            or(
                cableNoNeighbour(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST),
                cableNoNeighbour(Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST).term(CableBlock.NORTH, true),
                cableNoNeighbour(Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST).term(CableBlock.SOUTH, true)
            ),
            plainVariant(coreFacing).with(VariantMutator.Y_ROT.withValue(Quadrant.R0))
        );

        // East/West
        generator.with(
            or(
                cableNoNeighbour(Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN).term(CableBlock.EAST, true),
                cableNoNeighbour(Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN).term(CableBlock.WEST, true)
            ),
            plainVariant(coreFacing).with(VariantMutator.Y_ROT.withValue(Quadrant.R90))
        );

        // Find all other possibilities and emit a "solid" core which doesn't have a facing direction.
        var core = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/cable_core_any");
        List<ConditionBuilder> rightAngles = new ArrayList<>();
        for (var i = 0; i < DirectionUtil.FACINGS.length; i++) {
            for (var j = i; j < DirectionUtil.FACINGS.length; j++) {
                if (DirectionUtil.FACINGS[i].getAxis() == DirectionUtil.FACINGS[j].getAxis()) continue;

                rightAngles.add(condition()
                    .term(CableBlock.CABLE, true).term(CABLE_DIRECTIONS[i], true).term(CABLE_DIRECTIONS[j], true)
                );
            }
        }
        generator.with(or(rightAngles.toArray(new ConditionBuilder[0])), plainVariant(core));

        // Then emit the actual cable arms
        var arm = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/cable_arm");
        for (var direction : DirectionUtil.FACINGS) {
            generator.with(
                condition().term(CABLE_DIRECTIONS[direction.ordinal()], true),
                plainVariant(arm)
                    .with(VariantMutator.X_ROT.withValue(toXAngle(direction.getOpposite())))
                    .with(VariantMutator.Y_ROT.withValue(toYAngle(direction.getOpposite())))
            );
        }

        // And the modems!
        for (var direction : DirectionUtil.FACINGS) {
            for (var on : BOOLEANS) {
                for (var peripheral : BOOLEANS) {
                    var suffix = (on ? "_on" : "_off") + (peripheral ? "_peripheral" : "");
                    generator.with(
                        condition().term(CableBlock.MODEM, CableModemVariant.from(direction, on, peripheral)),
                        plainVariant(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/wired_modem" + suffix))
                            .with(VariantMutator.X_ROT.withValue(toXAngle(direction)))
                            .with(VariantMutator.Y_ROT.withValue(toYAngle(direction)))
                    );
                }
            }
        }

        generators.blockStateOutput.accept(generator);
        generators.registerSimpleItemModel(ModRegistry.Items.CABLE.get(), getModelLocation(ModRegistry.Items.CABLE.get()));
    }

    private static void registerRedstoneControl(BlockModelGenerators generators) {
        var redstoneControl = ModRegistry.Blocks.REDSTONE_RELAY.get();
        generators.createHorizontallyRotatedBlock(redstoneControl, TexturedModel.ORIENTABLE);
        registerSimpleItemModel(generators, redstoneControl);
    }


    private static final BooleanProperty[] CABLE_DIRECTIONS = { CableBlock.DOWN, CableBlock.UP, CableBlock.NORTH, CableBlock.SOUTH, CableBlock.WEST, CableBlock.EAST };
    private static final boolean[] BOOLEANS = new boolean[]{ false, true };

    private static ConditionBuilder cableNoNeighbour(Direction... directions) {
        var condition = condition().term(CableBlock.CABLE, true);
        for (var direction : directions) condition.term(CABLE_DIRECTIONS[direction.ordinal()], false);
        return condition;
    }

    private static void registerTurtleUpgrade(BlockModelGenerators generators, String name, String texture) {
        TURTLE_UPGRADE_LEFT.create(
            Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, name + "_left"),
            TextureMapping.defaultTexture(new Material(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, texture))),
            generators.modelOutput
        );
        TURTLE_UPGRADE_RIGHT.create(
            Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, name + "_right"),
            TextureMapping.defaultTexture(new Material(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, texture))),
            generators.modelOutput
        );
    }

    private static void registerTurtleModem(BlockModelGenerators generators, String name, String texture) {
        registerTurtleUpgrade(generators, name + "_off", texture);
        registerTurtleUpgrade(generators, name + "_on", texture + "_on");
    }

    private static void registerSimpleItemModel(BlockModelGenerators generators, Block block) {
        generators.registerSimpleItemModel(block, ModelLocationUtils.getModelLocation(block));
    }

    private static Quadrant toXAngle(Direction direction) {
        return switch (direction) {
            default -> Quadrant.R0;
            case UP -> Quadrant.R270;
            case DOWN -> Quadrant.R90;
        };
    }

    private static Quadrant toYAngle(Direction direction) {
        return switch (direction) {
            default -> Quadrant.R0;
            case NORTH -> Quadrant.R0;
            case SOUTH -> Quadrant.R180;
            case EAST -> Quadrant.R90;
            case WEST -> Quadrant.R270;
        };
    }

    private static PropertyDispatch<VariantMutator> createVerticalFacingDispatch(Property<Direction> property) {
        var dispatch = PropertyDispatch.modify(property);
        for (var direction : property.getPossibleValues()) {
            dispatch.select(direction, VariantMutator.X_ROT.withValue(toXAngle(direction)));
        }
        return dispatch;
    }

    private static <T extends Comparable<T>> PropertyDispatch<MultiVariant> createModelDispatch(Property<T> property, Function<T, Identifier> makeModel) {
        var variant = PropertyDispatch.initial(property);
        for (var value : property.getPossibleValues()) {
            variant.select(value, plainVariant(makeModel.apply(value)));
        }
        return variant;
    }

    private static <T extends Comparable<T>, U extends Comparable<U>> PropertyDispatch<MultiVariant> createModelDispatch(
        Property<T> propertyT, Property<U> propertyU, BiFunction<T, U, Identifier> makeModel
    ) {
        var variant = PropertyDispatch.initial(propertyT, propertyU);
        for (var valueT : propertyT.getPossibleValues()) {
            for (var valueU : propertyU.getPossibleValues()) {
                variant.select(valueT, valueU, plainVariant(makeModel.apply(valueT, valueU)));
            }
        }
        return variant;
    }
}

// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.turtle.RegisterTurtleUpgradeModeller;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.client.render.CustomLecternRenderer;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.client.render.TurtleBlockEntityRenderer;
import dan200.computercraft.client.render.monitor.MonitorBlockEntityRenderer;
import dan200.computercraft.client.turtle.TurtleModemModeller;
import dan200.computercraft.client.turtle.TurtleUpgradeModellers;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.computer.inventory.AbstractComputerMenu;
import dan200.computercraft.shared.media.items.DiskItem;
import dan200.computercraft.shared.media.items.TreasureDiskItem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Registers client-side objects, such as {@link BlockEntityRendererProvider}s and
 * {@link MenuScreens.ScreenConstructor}.
 * <p>
 * The functions in this class should be called from a loader-specific class.
 *
 * @see ModRegistry The common registry for actual game objects.
 */
public final class ClientRegistry {
    private ClientRegistry() {
    }

    /**
     * Register any client-side objects which don't have to be done on the main thread.
     */
    public static void register() {
        BlockEntityRenderers.register(ModRegistry.BlockEntities.MONITOR_NORMAL.get(), MonitorBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModRegistry.BlockEntities.MONITOR_ADVANCED.get(), MonitorBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModRegistry.BlockEntities.TURTLE_NORMAL.get(), TurtleBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModRegistry.BlockEntities.TURTLE_ADVANCED.get(), TurtleBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModRegistry.BlockEntities.LECTERN.get(), CustomLecternRenderer::new);
    }

    /**
     * Register any client-side objects which must be done on the main thread.
     *
     * @param itemProperties Callback to register item properties.
     */
    public static void registerMainThread(RegisterItemProperty itemProperties) {
        MenuScreens.<AbstractComputerMenu, ComputerScreen<AbstractComputerMenu>>register(ModRegistry.Menus.COMPUTER.get(), ComputerScreen::new);
        MenuScreens.<AbstractComputerMenu, NoTermComputerScreen<AbstractComputerMenu>>register(ModRegistry.Menus.POCKET_COMPUTER_NO_TERM.get(), NoTermComputerScreen::new);
        MenuScreens.register(ModRegistry.Menus.TURTLE.get(), TurtleScreen::new);

        MenuScreens.register(ModRegistry.Menus.PRINTER.get(), PrinterScreen::new);
        MenuScreens.register(ModRegistry.Menus.DISK_DRIVE.get(), DiskDriveScreen::new);
        MenuScreens.register(ModRegistry.Menus.PRINTOUT.get(), PrintoutScreen::new);

        registerItemProperty(itemProperties, "state",
            new UnclampedPropertyFunction((stack, world, player, random) -> {
                var computer = ClientPocketComputers.get(stack);
                return (computer == null ? ComputerState.OFF : computer.getState()).ordinal();
            }),
            ModRegistry.Items.POCKET_COMPUTER_NORMAL, ModRegistry.Items.POCKET_COMPUTER_ADVANCED
        );
        registerItemProperty(itemProperties, "coloured",
            (stack, world, player, random) -> IColouredItem.getColourBasic(stack) != -1 ? 1 : 0,
            ModRegistry.Items.POCKET_COMPUTER_NORMAL, ModRegistry.Items.POCKET_COMPUTER_ADVANCED
        );
    }

    public static void registerTurtleModellers(RegisterTurtleUpgradeModeller register) {
        register.register(ModRegistry.TurtleSerialisers.SPEAKER.get(), TurtleUpgradeModeller.sided(
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_speaker_left"),
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_speaker_right")
        ));
        register.register(ModRegistry.TurtleSerialisers.WORKBENCH.get(), TurtleUpgradeModeller.sided(
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_crafting_table_left"),
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_crafting_table_right")
        ));
        register.register(ModRegistry.TurtleSerialisers.WIRELESS_MODEM_NORMAL.get(), new TurtleModemModeller(false));
        register.register(ModRegistry.TurtleSerialisers.WIRELESS_MODEM_ADVANCED.get(), new TurtleModemModeller(true));
        register.register(ModRegistry.TurtleSerialisers.TOOL.get(), TurtleUpgradeModeller.flatItem());
    }

    @SafeVarargs
    private static void registerItemProperty(RegisterItemProperty itemProperties, String name, ClampedItemPropertyFunction getter, Supplier<? extends Item>... items) {
        var id = new ResourceLocation(ComputerCraftAPI.MOD_ID, name);
        for (var item : items) itemProperties.register(item.get(), id, getter);
    }

    /**
     * Register an item property via {@link ItemProperties#register}. Forge and Fabric expose different methods, so we
     * supply this via mod-loader-specific code.
     */
    public interface RegisterItemProperty {
        void register(Item item, ResourceLocation name, ClampedItemPropertyFunction property);
    }

    public static void registerReloadListeners(Consumer<PreparableReloadListener> register, Minecraft minecraft) {
        register.accept(GuiSprites.initialise(minecraft.getTextureManager()));
    }

    private static final String[] EXTRA_MODELS = new String[]{
        "block/turtle_colour",
        "block/turtle_elf_overlay",
        "block/turtle_rainbow_overlay",
        "block/turtle_trans_overlay",
    };

    public static void registerExtraModels(Consumer<ResourceLocation> register) {
        for (var model : EXTRA_MODELS) register.accept(new ResourceLocation(ComputerCraftAPI.MOD_ID, model));
        TurtleUpgradeModellers.getDependencies().forEach(register);
    }

    public static void registerItemColours(BiConsumer<ItemColor, ItemLike> register) {
        register.accept(
            (stack, layer) -> layer == 1 ? ((DiskItem) stack.getItem()).getColour(stack) : 0xFFFFFF,
            ModRegistry.Items.DISK.get()
        );

        register.accept(
            (stack, layer) -> layer == 1 ? TreasureDiskItem.getColour(stack) : 0xFFFFFF,
            ModRegistry.Items.TREASURE_DISK.get()
        );

        register.accept(ClientRegistry::getPocketColour, ModRegistry.Items.POCKET_COMPUTER_NORMAL.get());
        register.accept(ClientRegistry::getPocketColour, ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get());

        register.accept(ClientRegistry::getTurtleColour, ModRegistry.Blocks.TURTLE_NORMAL.get());
        register.accept(ClientRegistry::getTurtleColour, ModRegistry.Blocks.TURTLE_ADVANCED.get());
    }

    private static int getPocketColour(ItemStack stack, int layer) {
        return switch (layer) {
            default -> 0xFFFFFF;
            case 1 -> IColouredItem.getColourBasic(stack); // Frame colour
            case 2 -> { // Light colour
                var computer = ClientPocketComputers.get(stack);
                yield computer == null || computer.getLightState() == -1 ? Colour.BLACK.getHex() : computer.getLightState();
            }
        };
    }

    private static int getTurtleColour(ItemStack stack, int layer) {
        return layer == 0 ? ((IColouredItem) stack.getItem()).getColour(stack) : 0xFFFFFF;
    }

    public static void registerShaders(ResourceProvider resources, BiConsumer<ShaderInstance, Consumer<ShaderInstance>> load) throws IOException {
        RenderTypes.registerShaders(resources, load);
    }

    private record UnclampedPropertyFunction(
        ClampedItemPropertyFunction function
    ) implements ClampedItemPropertyFunction {
        @Override
        public float unclampedCall(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int layer) {
            return function.unclampedCall(stack, level, entity, layer);
        }

        @Deprecated
        @Override
        public float call(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int layer) {
            return function.unclampedCall(stack, level, entity, layer);
        }
    }

    /**
     * Register client-side commands.
     *
     * @param dispatcher The dispatcher to register the commands to.
     * @param sendError  A function to send an error message.
     * @param <T>        The type of the client-side command context.
     */
    public static <T> void registerClientCommands(CommandDispatcher<T> dispatcher, BiConsumer<T, Component> sendError) {
        dispatcher.register(LiteralArgumentBuilder.<T>literal(CommandComputerCraft.CLIENT_OPEN_FOLDER)
            .requires(x -> Minecraft.getInstance().getSingleplayerServer() != null)
            .then(RequiredArgumentBuilder.<T, Integer>argument("computer_id", IntegerArgumentType.integer(0))
                .executes(c -> handleOpenComputerCommand(c.getSource(), sendError, c.getArgument("computer_id", Integer.class)))
            ));
    }

    /**
     * Handle the {@link CommandComputerCraft#CLIENT_OPEN_FOLDER} command.
     *
     * @param context   The command context.
     * @param sendError A function to send an error message.
     * @param id        The computer's id.
     * @param <T>       The type of the client-side command context.
     * @return {@code 1} if a folder was opened, {@code 0} otherwise.
     */
    private static <T> int handleOpenComputerCommand(T context, BiConsumer<T, Component> sendError, int id) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            sendError.accept(context, Component.literal("Not on a single-player server"));
            return 0;
        }

        var file = new File(ServerContext.get(server).storageDir().toFile(), "computer/" + id);
        if (!file.isDirectory()) {
            sendError.accept(context, Component.literal("Computer's folder does not exist"));
            return 0;
        }

        Util.getPlatform().openFile(file);
        return 1;
    }
}

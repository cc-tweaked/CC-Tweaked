/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.ComputerCraftAPIClient;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.client.render.TurtleBlockEntityRenderer;
import dan200.computercraft.client.render.monitor.MonitorBlockEntityRenderer;
import dan200.computercraft.client.turtle.TurtleModemModeller;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.inventory.AbstractComputerMenu;
import dan200.computercraft.shared.computer.inventory.ViewComputerMenu;
import dan200.computercraft.shared.media.items.DiskItem;
import dan200.computercraft.shared.media.items.TreasureDiskItem;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import javax.annotation.Nullable;
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
        ComputerCraftAPIClient.registerTurtleUpgradeModeller(ModRegistry.TurtleSerialisers.SPEAKER.get(), TurtleUpgradeModeller.sided(
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_speaker_left"),
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_speaker_right")
        ));
        ComputerCraftAPIClient.registerTurtleUpgradeModeller(ModRegistry.TurtleSerialisers.WORKBENCH.get(), TurtleUpgradeModeller.sided(
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_crafting_table_left"),
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_crafting_table_right")
        ));
        ComputerCraftAPIClient.registerTurtleUpgradeModeller(ModRegistry.TurtleSerialisers.WIRELESS_MODEM_NORMAL.get(), new TurtleModemModeller(false));
        ComputerCraftAPIClient.registerTurtleUpgradeModeller(ModRegistry.TurtleSerialisers.WIRELESS_MODEM_ADVANCED.get(), new TurtleModemModeller(true));
        ComputerCraftAPIClient.registerTurtleUpgradeModeller(ModRegistry.TurtleSerialisers.TOOL.get(), TurtleUpgradeModeller.flatItem());
    }

    /**
     * Register any client-side objects which must be done on the main thread.
     */
    public static void registerMainThread() {
        MenuScreens.<AbstractComputerMenu, ComputerScreen<AbstractComputerMenu>>register(ModRegistry.Menus.COMPUTER.get(), ComputerScreen::new);
        MenuScreens.<AbstractComputerMenu, ComputerScreen<AbstractComputerMenu>>register(ModRegistry.Menus.POCKET_COMPUTER.get(), ComputerScreen::new);
        MenuScreens.<AbstractComputerMenu, NoTermComputerScreen<AbstractComputerMenu>>register(ModRegistry.Menus.POCKET_COMPUTER_NO_TERM.get(), NoTermComputerScreen::new);
        MenuScreens.register(ModRegistry.Menus.TURTLE.get(), TurtleScreen::new);

        MenuScreens.register(ModRegistry.Menus.PRINTER.get(), PrinterScreen::new);
        MenuScreens.register(ModRegistry.Menus.DISK_DRIVE.get(), DiskDriveScreen::new);
        MenuScreens.register(ModRegistry.Menus.PRINTOUT.get(), PrintoutScreen::new);

        MenuScreens.<ViewComputerMenu, ComputerScreen<ViewComputerMenu>>register(ModRegistry.Menus.VIEW_COMPUTER.get(), ComputerScreen::new);

        registerItemProperty("state",
            new UnclampedPropertyFunction((stack, world, player, random) -> ClientPocketComputers.get(stack).getState().ordinal()),
            ModRegistry.Items.POCKET_COMPUTER_NORMAL, ModRegistry.Items.POCKET_COMPUTER_ADVANCED
        );
        registerItemProperty("coloured",
            (stack, world, player, random) -> IColouredItem.getColourBasic(stack) != -1 ? 1 : 0,
            ModRegistry.Items.POCKET_COMPUTER_NORMAL, ModRegistry.Items.POCKET_COMPUTER_ADVANCED
        );
    }

    @SafeVarargs
    private static void registerItemProperty(String name, ClampedItemPropertyFunction getter, Supplier<? extends Item>... items) {
        var id = new ResourceLocation(ComputerCraftAPI.MOD_ID, name);
        for (var item : items) ItemProperties.register(item.get(), id, getter);
    }

    private static final String[] EXTRA_MODELS = new String[]{
        // Turtle upgrades
        "block/turtle_modem_normal_off_left",
        "block/turtle_modem_normal_on_left",
        "block/turtle_modem_normal_off_right",
        "block/turtle_modem_normal_on_right",

        "block/turtle_modem_advanced_off_left",
        "block/turtle_modem_advanced_on_left",
        "block/turtle_modem_advanced_off_right",
        "block/turtle_modem_advanced_on_right",

        "block/turtle_crafting_table_left",
        "block/turtle_crafting_table_right",

        "block/turtle_speaker_left",
        "block/turtle_speaker_right",

        // Turtle block renderer
        "block/turtle_colour",
        "block/turtle_elf_overlay",
    };

    public static void registerExtraModels(Consumer<ResourceLocation> register) {
        for (var model : EXTRA_MODELS) register.accept(new ResourceLocation(ComputerCraftAPI.MOD_ID, model));
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
        switch (layer) {
            case 0:
            default:
                return 0xFFFFFF;
            case 1: // Frame colour
                return IColouredItem.getColourBasic(stack);
            case 2: { // Light colour
                var light = ClientPocketComputers.get(stack).getLightState();
                return light == -1 ? Colour.BLACK.getHex() : light;
            }
        }
    }

    private static int getTurtleColour(ItemStack stack, int layer) {
        return layer == 0 ? ((IColouredItem) stack.getItem()).getColour(stack) : 0xFFFFFF;
    }

    public static void registerBlockEntityRenderers(BlockEntityRenderRegistry register) {
        register.register(ModRegistry.BlockEntities.MONITOR_NORMAL.get(), MonitorBlockEntityRenderer::new);
        register.register(ModRegistry.BlockEntities.MONITOR_ADVANCED.get(), MonitorBlockEntityRenderer::new);
        register.register(ModRegistry.BlockEntities.TURTLE_NORMAL.get(), TurtleBlockEntityRenderer::new);
        register.register(ModRegistry.BlockEntities.TURTLE_ADVANCED.get(), TurtleBlockEntityRenderer::new);
    }

    public interface BlockEntityRenderRegistry {
        <T extends BlockEntity> void register(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> provider);
    }

    public static void registerShaders(ResourceManager resources, BiConsumer<ShaderInstance, Consumer<ShaderInstance>> load) throws IOException {
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
}

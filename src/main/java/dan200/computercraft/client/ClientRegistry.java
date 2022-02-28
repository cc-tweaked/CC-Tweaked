/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import dan200.computercraft.client.render.TileEntityTurtleRenderer;
import dan200.computercraft.client.render.TurtleModelLoader;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.function.Supplier;

/**
 * Registers textures and models for items.
 */
@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class ClientRegistry
{
    private static final String[] EXTRA_MODELS = new String[] {
        // Turtle upgrades
        "turtle_modem_normal_off_left",
        "turtle_modem_normal_on_left",
        "turtle_modem_normal_off_right",
        "turtle_modem_normal_on_right",

        "turtle_modem_advanced_off_left",
        "turtle_modem_advanced_on_left",
        "turtle_modem_advanced_off_right",
        "turtle_modem_advanced_on_right",
        "turtle_crafting_table_left",
        "turtle_crafting_table_right",

        "turtle_speaker_upgrade_left",
        "turtle_speaker_upgrade_right",

        // Turtle block renderer
        "turtle_colour",
        "turtle_elf_overlay",
    };

    private ClientRegistry() {}

    @SubscribeEvent
    public static void registerModels( ModelRegistryEvent event )
    {
        ModelLoaderRegistry.registerLoader( new ResourceLocation( ComputerCraft.MOD_ID, "turtle" ), TurtleModelLoader.INSTANCE );
        for( String model : EXTRA_MODELS )
        {
            ForgeModelBakery.addSpecialModel( new ModelResourceLocation( new ResourceLocation( ComputerCraft.MOD_ID, model ), "inventory" ) );
        }
    }

    @SubscribeEvent
    public static void onItemColours( ColorHandlerEvent.Item event )
    {
        if( Registry.ModItems.DISK == null || Registry.ModBlocks.TURTLE_NORMAL == null )
        {
            ComputerCraft.log.warn( "Block/item registration has failed. Skipping registration of item colours." );
            return;
        }

        event.getItemColors().register(
            ( stack, layer ) -> layer == 1 ? ((ItemDisk) stack.getItem()).getColour( stack ) : 0xFFFFFF,
            Registry.ModItems.DISK.get()
        );

        event.getItemColors().register(
            ( stack, layer ) -> layer == 1 ? ItemTreasureDisk.getColour( stack ) : 0xFFFFFF,
            Registry.ModItems.TREASURE_DISK.get()
        );

        event.getItemColors().register( ( stack, layer ) -> {
            switch( layer )
            {
                case 0:
                default:
                    return 0xFFFFFF;
                case 1: // Frame colour
                    return IColouredItem.getColourBasic( stack );
                case 2: // Light colour
                {
                    int light = ItemPocketComputer.getLightState( stack );
                    return light == -1 ? Colour.BLACK.getHex() : light;
                }
            }
        }, Registry.ModItems.POCKET_COMPUTER_NORMAL.get(), Registry.ModItems.POCKET_COMPUTER_ADVANCED.get() );

        // Setup turtle colours
        event.getItemColors().register(
            ( stack, tintIndex ) -> tintIndex == 0 ? ((IColouredItem) stack.getItem()).getColour( stack ) : 0xFFFFFF,
            Registry.ModBlocks.TURTLE_NORMAL.get(), Registry.ModBlocks.TURTLE_ADVANCED.get()
        );
    }

    @SubscribeEvent
    public static void setupClient( FMLClientSetupEvent event )
    {
        registerContainers();

        // While turtles themselves are not transparent, their upgrades may be.
        ItemBlockRenderTypes.setRenderLayer( Registry.ModBlocks.TURTLE_NORMAL.get(), RenderType.translucent() );
        ItemBlockRenderTypes.setRenderLayer( Registry.ModBlocks.TURTLE_ADVANCED.get(), RenderType.translucent() );

        // Monitors' textures have transparent fronts and so count as cutouts.
        ItemBlockRenderTypes.setRenderLayer( Registry.ModBlocks.MONITOR_NORMAL.get(), RenderType.cutout() );
        ItemBlockRenderTypes.setRenderLayer( Registry.ModBlocks.MONITOR_ADVANCED.get(), RenderType.cutout() );

        // Setup TESRs
        BlockEntityRenderers.register( Registry.ModBlockEntities.MONITOR_NORMAL.get(), TileEntityMonitorRenderer::new );
        BlockEntityRenderers.register( Registry.ModBlockEntities.MONITOR_ADVANCED.get(), TileEntityMonitorRenderer::new );
        BlockEntityRenderers.register( Registry.ModBlockEntities.TURTLE_NORMAL.get(), TileEntityTurtleRenderer::new );
        BlockEntityRenderers.register( Registry.ModBlockEntities.TURTLE_ADVANCED.get(), TileEntityTurtleRenderer::new );

        registerItemProperty( "state",
            ( stack, world, player, random ) -> ItemPocketComputer.getState( stack ).ordinal(),
            Registry.ModItems.POCKET_COMPUTER_NORMAL, Registry.ModItems.POCKET_COMPUTER_ADVANCED
        );
        registerItemProperty( "coloured",
            ( stack, world, player, random ) -> IColouredItem.getColourBasic( stack ) != -1 ? 1 : 0,
            Registry.ModItems.POCKET_COMPUTER_NORMAL, Registry.ModItems.POCKET_COMPUTER_ADVANCED
        );
    }

    @SafeVarargs
    private static void registerItemProperty( String name, ItemPropertyFunction getter, Supplier<? extends Item>... items )
    {
        ResourceLocation id = new ResourceLocation( ComputerCraft.MOD_ID, name );
        for( Supplier<? extends Item> item : items )
        {
            ItemProperties.register( item.get(), id, getter );
        }
    }


    private static void registerContainers()
    {
        // My IDE doesn't think so, but we do actually need these generics.

        MenuScreens.<ContainerComputerBase, GuiComputer<ContainerComputerBase>>register( Registry.ModContainers.COMPUTER.get(), GuiComputer::create );
        MenuScreens.<ContainerComputerBase, GuiComputer<ContainerComputerBase>>register( Registry.ModContainers.POCKET_COMPUTER.get(), GuiComputer::createPocket );
        MenuScreens.<ContainerComputerBase, NoTermComputerScreen<ContainerComputerBase>>register( Registry.ModContainers.POCKET_COMPUTER_NO_TERM.get(), NoTermComputerScreen::new );
        MenuScreens.register( Registry.ModContainers.TURTLE.get(), GuiTurtle::new );

        MenuScreens.register( Registry.ModContainers.PRINTER.get(), GuiPrinter::new );
        MenuScreens.register( Registry.ModContainers.DISK_DRIVE.get(), GuiDiskDrive::new );
        MenuScreens.register( Registry.ModContainers.PRINTOUT.get(), GuiPrintout::new );

        MenuScreens.<ContainerViewComputer, GuiComputer<ContainerViewComputer>>register( Registry.ModContainers.VIEW_COMPUTER.get(), GuiComputer::createView );
    }
}

/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.client.ComputerCraftAPIClient;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import dan200.computercraft.client.render.TileEntityTurtleRenderer;
import dan200.computercraft.client.render.TurtleModelLoader;
import dan200.computercraft.client.turtle.TurtleModemModeller;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
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

    private ClientRegistry() {}

    @SubscribeEvent
    public static void registerModelLoaders( ModelEvent.RegisterGeometryLoaders event )
    {
        event.register( "turtle", TurtleModelLoader.INSTANCE );
    }

    @SubscribeEvent
    public static void registerModels( ModelEvent.RegisterAdditional event )
    {
        for( String model : EXTRA_MODELS )
        {
            event.register( new ResourceLocation( ComputerCraft.MOD_ID, model ) );
        }
    }

    @SubscribeEvent
    public static void onItemColours( RegisterColorHandlersEvent.Item event )
    {
        if( Registry.ModItems.DISK == null || Registry.ModBlocks.TURTLE_NORMAL == null )
        {
            ComputerCraft.log.warn( "Block/item registration has failed. Skipping registration of item colours." );
            return;
        }

        event.register(
            ( stack, layer ) -> layer == 1 ? ((ItemDisk) stack.getItem()).getColour( stack ) : 0xFFFFFF,
            Registry.ModItems.DISK.get()
        );

        event.register(
            ( stack, layer ) -> layer == 1 ? ItemTreasureDisk.getColour( stack ) : 0xFFFFFF,
            Registry.ModItems.TREASURE_DISK.get()
        );

        event.register( ( stack, layer ) -> {
            switch( layer )
            {
                case 0:
                default:
                    return 0xFFFFFF;
                case 1: // Frame colour
                    return IColouredItem.getColourBasic( stack );
                case 2: // Light colour
                {
                    int light = ClientPocketComputers.get( stack ).getLightState();
                    return light == -1 ? Colour.BLACK.getHex() : light;
                }
            }
        }, Registry.ModItems.POCKET_COMPUTER_NORMAL.get(), Registry.ModItems.POCKET_COMPUTER_ADVANCED.get() );

        // Setup turtle colours
        event.register(
            ( stack, tintIndex ) -> tintIndex == 0 ? ((IColouredItem) stack.getItem()).getColour( stack ) : 0xFFFFFF,
            Registry.ModBlocks.TURTLE_NORMAL.get(), Registry.ModBlocks.TURTLE_ADVANCED.get()
        );
    }

    @SubscribeEvent
    public static void setupClient( FMLClientSetupEvent event )
    {
        // Setup TESRs
        BlockEntityRenderers.register( Registry.ModBlockEntities.MONITOR_NORMAL.get(), TileEntityMonitorRenderer::new );
        BlockEntityRenderers.register( Registry.ModBlockEntities.MONITOR_ADVANCED.get(), TileEntityMonitorRenderer::new );
        BlockEntityRenderers.register( Registry.ModBlockEntities.TURTLE_NORMAL.get(), TileEntityTurtleRenderer::new );
        BlockEntityRenderers.register( Registry.ModBlockEntities.TURTLE_ADVANCED.get(), TileEntityTurtleRenderer::new );

        ComputerCraftAPIClient.registerTurtleUpgradeModeller( Registry.ModTurtleSerialisers.SPEAKER.get(), TurtleUpgradeModeller.sided(
            new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_speaker_left" ),
            new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_speaker_right" )
        ) );
        ComputerCraftAPIClient.registerTurtleUpgradeModeller( Registry.ModTurtleSerialisers.WORKBENCH.get(), TurtleUpgradeModeller.sided(
            new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_crafting_table_left" ),
            new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_crafting_table_right" )
        ) );
        ComputerCraftAPIClient.registerTurtleUpgradeModeller( Registry.ModTurtleSerialisers.WIRELESS_MODEM_NORMAL.get(), new TurtleModemModeller( false ) );
        ComputerCraftAPIClient.registerTurtleUpgradeModeller( Registry.ModTurtleSerialisers.WIRELESS_MODEM_ADVANCED.get(), new TurtleModemModeller( true ) );
        ComputerCraftAPIClient.registerTurtleUpgradeModeller( Registry.ModTurtleSerialisers.TOOL.get(), TurtleUpgradeModeller.flatItem() );

        event.enqueueWork( () -> {
            registerContainers();

            registerItemProperty( "state",
                ( stack, world, player, random ) -> ClientPocketComputers.get( stack ).getState().ordinal(),
                Registry.ModItems.POCKET_COMPUTER_NORMAL, Registry.ModItems.POCKET_COMPUTER_ADVANCED
            );
            registerItemProperty( "coloured",
                ( stack, world, player, random ) -> IColouredItem.getColourBasic( stack ) != -1 ? 1 : 0,
                Registry.ModItems.POCKET_COMPUTER_NORMAL, Registry.ModItems.POCKET_COMPUTER_ADVANCED
            );
        } );
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

        MenuScreens.<ContainerComputerBase, GuiComputer<ContainerComputerBase>>register( Registry.ModContainers.COMPUTER.get(), GuiComputer::new );
        MenuScreens.<ContainerComputerBase, GuiComputer<ContainerComputerBase>>register( Registry.ModContainers.POCKET_COMPUTER.get(), GuiComputer::new );
        MenuScreens.<ContainerComputerBase, NoTermComputerScreen<ContainerComputerBase>>register( Registry.ModContainers.POCKET_COMPUTER_NO_TERM.get(), NoTermComputerScreen::new );
        MenuScreens.register( Registry.ModContainers.TURTLE.get(), GuiTurtle::new );

        MenuScreens.register( Registry.ModContainers.PRINTER.get(), GuiPrinter::new );
        MenuScreens.register( Registry.ModContainers.DISK_DRIVE.get(), GuiDiskDrive::new );
        MenuScreens.register( Registry.ModContainers.PRINTOUT.get(), GuiPrintout::new );

        MenuScreens.<ContainerViewComputer, GuiComputer<ContainerViewComputer>>register( Registry.ModContainers.VIEW_COMPUTER.get(), GuiComputer::new );
    }
}

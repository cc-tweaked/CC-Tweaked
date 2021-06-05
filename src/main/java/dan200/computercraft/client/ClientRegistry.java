/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.TurtleModelLoader;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
            ModelLoader.addSpecialModel( new ModelResourceLocation( new ResourceLocation( ComputerCraft.MOD_ID, model ), "inventory" ) );
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
}

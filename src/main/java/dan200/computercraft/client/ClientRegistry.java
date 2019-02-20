/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.TurtleModelLoader;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.model.ModelRotation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers textures and models for items.
 */
@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
public class ClientRegistry
{
    private static final String[] EXTRA_MODELS = {
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

        "turtle_colour",
        "turtle_elf_overlay",
    };

    @SubscribeEvent
    public static void registerModels( ModelRegistryEvent event )
    {
        ModelLoaderRegistry.registerLoader( TurtleModelLoader.INSTANCE );
    }

    @SubscribeEvent
    public static void onModelBakeEvent( ModelBakeEvent event )
    {
        // Load all extra models
        for( String model : EXTRA_MODELS ) loadItemModel( event, model );
    }

    @SubscribeEvent
    public static void onItemColours( ColorHandlerEvent.Item event )
    {
        event.getItemColors().register( ( stack, tintIndex ) -> {
            if( tintIndex == 1 ) return ((IColouredItem) stack.getItem()).getColour( stack );
            return 0xFFFFFF;
        }, ComputerCraft.Items.disk );

        event.getItemColors().register( ( stack, layer ) -> {
            switch( layer )
            {
                case 0:
                default:
                    return 0xFFFFFF;
                case 1: // Frame colour
                    return IColouredItem.getColourBasic( stack );
                case 2: // Light colour
                    return ItemPocketComputer.getLightState( stack );
            }
        }, ComputerCraft.Items.pocketComputerNormal, ComputerCraft.Items.pocketComputerAdvanced );

        // Setup turtle colours
        event.getItemColors().register( ( stack, tintIndex ) -> {
            if( tintIndex == 0 ) return ((IColouredItem) stack.getItem()).getColour( stack );
            return 0xFFFFFF;
        }, ComputerCraft.Blocks.turtleNormal, ComputerCraft.Blocks.turtleAdvanced );
    }

    private static void loadItemModel( ModelBakeEvent event, String name )
    {
        ModelLoader loader = event.getModelLoader();
        IBakedModel bakedModel = loader
            .getUnbakedModel( new ResourceLocation( ComputerCraft.MOD_ID, "item/" + name ) )
            .bake(
                loader::getUnbakedModel,
                Minecraft.getInstance().getTextureMap()::getSprite,
                ModelRotation.X0_Y0, false, DefaultVertexFormats.BLOCK
            );

        if( bakedModel != null )
        {
            event.getModelRegistry().put( new ModelResourceLocation( ComputerCraft.MOD_ID + ":" + name, "inventory" ), bakedModel );
        }
    }
}

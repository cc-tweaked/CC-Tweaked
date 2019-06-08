/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.TurtleModelLoader;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.BasicState;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Map;

/**
 * Registers textures and models for items.
 */
@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
public final class ClientRegistry
{
    private static final String[] EXTRA_MODELS = new String[] {
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

    private static final String[] EXTRA_TEXTURES = new String[] {
        // TODO: Gather these automatically from the model. I'm unable to get this working with Forge's current
        //  model loading code.
        "block/turtle_colour",
        "block/turtle_elf_overlay",
        "block/turtle_crafty_face",
        "block/turtle_speaker_face",
    };

    private ClientRegistry() {}

    @SubscribeEvent
    public static void registerModels( ModelRegistryEvent event )
    {
        ModelLoaderRegistry.registerLoader( TurtleModelLoader.INSTANCE );
    }

    @SubscribeEvent
    public static void onTextureStitchEvent( TextureStitchEvent.Pre event )
    {
        IResourceManager manager = Minecraft.getInstance().getResourceManager();
        for( String extra : EXTRA_TEXTURES )
        {
            // TODO: event.getMap().registerSprite( manager, new ResourceLocation( ComputerCraft.MOD_ID, extra ) );
        }
    }

    @SubscribeEvent
    public static void onModelBakeEvent( ModelBakeEvent event )
    {
        // Load all extra models
        ModelLoader loader = event.getModelLoader();
        Map<ModelResourceLocation, IBakedModel> registry = event.getModelRegistry();

        for( String model : EXTRA_MODELS )
        {
            IBakedModel bakedModel = bake( loader, loader.getUnbakedModel( new ResourceLocation( ComputerCraft.MOD_ID, "item/" + model ) ) );

            if( bakedModel != null )
            {
                registry.put(
                    new ModelResourceLocation( new ResourceLocation( ComputerCraft.MOD_ID, model ), "inventory" ),
                    bakedModel
                );
            }
        }

        // And load the custom turtle models in too.
        registry.put(
            new ModelResourceLocation( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_normal" ), "inventory" ),
            bake( loader, TurtleModelLoader.INSTANCE.loadModel( new ResourceLocation( ComputerCraft.MOD_ID, "item/turtle_normal" ) ) )
        );

        registry.put(
            new ModelResourceLocation( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_advanced" ), "inventory" ),
            bake( loader, TurtleModelLoader.INSTANCE.loadModel( new ResourceLocation( ComputerCraft.MOD_ID, "item/turtle_advanced" ) ) )
        );
    }

    @SubscribeEvent
    public static void onItemColours( ColorHandlerEvent.Item event )
    {
        if( ComputerCraft.Items.disk == null || ComputerCraft.Blocks.turtleNormal == null )
        {
            ComputerCraft.log.warn( "Block/item registration has failed. Skipping registration of item colours." );
            return;
        }

        event.getItemColors().register(
            ( stack, layer ) -> layer == 1 ? ((ItemDisk) stack.getItem()).getColour( stack ) : 0xFFFFFF,
            ComputerCraft.Items.disk
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
                    return light == -1 ? Colour.Black.getHex() : light;
                }
            }
        }, ComputerCraft.Items.pocketComputerNormal, ComputerCraft.Items.pocketComputerAdvanced );

        // Setup turtle colours
        event.getItemColors().register(
            ( stack, tintIndex ) -> tintIndex == 0 ? ((IColouredItem) stack.getItem()).getColour( stack ) : 0xFFFFFF,
            ComputerCraft.Blocks.turtleNormal, ComputerCraft.Blocks.turtleAdvanced
        );
    }

    private static IBakedModel bake( ModelLoader loader, IUnbakedModel model )
    {
        model.getTextures( loader::getUnbakedModel, new HashSet<>() );

        return model.bake(
            loader,
            ModelLoader.defaultTextureGetter(),
            new BasicState( model.getDefaultState(), false ), DefaultVertexFormats.BLOCK
        );
    }
}

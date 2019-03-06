/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import net.fabricmc.fabric.api.client.render.ColorProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.ModelRotation;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.SpriteAtlasTexture;

import java.util.HashSet;

/**
 * Registers textures and models for items.
 */
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

    private static final String[] EXTRA_TEXTURES = new String[] {
        // TODO: Gather these automatically from the model when Forge's model loading is less broken.
        "block/turtle_colour",
        "block/turtle_elf_overlay",
        "block/turtle_crafty_face",
        "block/turtle_speaker_face",
    };

    /*
    @SubscribeEvent
    public static void onTextureStitchEvent( TextureStitchEvent.Pre event )
    {
        ResourceManager manager = MinecraftClient.getInstance().getResourceManager();
        for( String extra : EXTRA_TEXTURES )
        {
            event.getMap().registerSprite( manager, new Identifier( ComputerCraft.MOD_ID, extra ) );
        }
    }

    @SubscribeEvent
    public static void onModelBakeEvent( ModelBakeEvent event )
    {
        // Load all extra models
        ModelLoader loader = event.getModelLoader();
        Map<ModelIdentifier, BakedModel> registry = event.getModelRegistry();

        for( String model : EXTRA_MODELS )
        {
            BakedModel bakedModel = bake( loader, loader.getOrLoadModel( new Identifier( ComputerCraft.MOD_ID, "item/" + model ) ) );

            if( bakedModel != null )
            {
                registry.put(
                    new ModelIdentifier( new Identifier( ComputerCraft.MOD_ID, model ), "inventory" ),
                    bakedModel
                );
            }
        }

        // And load the custom turtle models in too.
        registry.put(
            new ModelIdentifier( new Identifier( ComputerCraft.MOD_ID, "turtle_normal" ), "inventory" ),
            bake( loader, TurtleModelLoader.INSTANCE.loadModel( new Identifier( ComputerCraft.MOD_ID, "item/turtle_normal" ) ) )
        );

        registry.put(
            new ModelIdentifier( new Identifier( ComputerCraft.MOD_ID, "turtle_advanced" ), "inventory" ),
            bake( loader, TurtleModelLoader.INSTANCE.loadModel( new Identifier( ComputerCraft.MOD_ID, "item/turtle_advanced" ) ) )
        );
    }
    */

    public static void onItemColours()
    {
        ColorProviderRegistry.ITEM.register( ( stack, tintIndex ) -> {
            if( tintIndex == 1 ) return ((IColouredItem) stack.getItem()).getColour( stack );
            return 0xFFFFFF;
        }, ComputerCraft.Items.disk );

        ColorProviderRegistry.ITEM.register( ( stack, layer ) -> {
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
        ColorProviderRegistry.ITEM.register( ( stack, tintIndex ) -> {
            if( tintIndex == 0 ) return ((IColouredItem) stack.getItem()).getColour( stack );
            return 0xFFFFFF;
        }, ComputerCraft.Blocks.turtleNormal, ComputerCraft.Blocks.turtleAdvanced );
    }

    private static BakedModel bake( ModelLoader loader, UnbakedModel model )
    {
        model.getTextureDependencies( loader::getOrLoadModel, new HashSet<>() );
        SpriteAtlasTexture sprite = MinecraftClient.getInstance().getSpriteAtlas();
        return model.bake( loader, sprite::getSprite, ModelRotation.X0_Y0 );
    }
}

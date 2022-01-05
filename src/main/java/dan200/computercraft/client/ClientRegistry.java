/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.util.Colour;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.HashSet;
import java.util.function.Consumer;

/**
 * Registers textures and models for items.
 */
@SuppressWarnings( {
    "MethodCallSideOnly",
    "LocalVariableDeclarationSideOnly"
} )
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
        // TODO: Gather these automatically from the model. Sadly the model loader isn't available
        //  when stitching textures.
        "block/turtle_colour",
        "block/turtle_elf_overlay",
        "block/turtle_crafty_face",
        "block/turtle_speaker_face",
    };

    private ClientRegistry() {}

    public static void onTextureStitchEvent( TextureAtlas atlasTexture, ClientSpriteRegistryCallback.Registry registry )
    {
        for( String extra : EXTRA_TEXTURES )
        {
            registry.register( new ResourceLocation( ComputerCraft.MOD_ID, extra ) );
        }
    }

    @SuppressWarnings( "NewExpressionSideOnly" )
    public static void onModelBakeEvent( ResourceManager manager, Consumer<ResourceLocation> out )
    {
        for( String model : EXTRA_MODELS )
        {
            out.accept( new ModelResourceLocation( new ResourceLocation( ComputerCraft.MOD_ID, model ), "inventory" ) );
        }
    }

    public static void onItemColours()
    {
        ColorProviderRegistry.ITEM.register( ( stack, layer ) -> {
            return layer == 1 ? ((ItemDisk) stack.getItem()).getColour( stack ) : 0xFFFFFF;
        }, ComputerCraftRegistry.ModItems.DISK );

        ColorProviderRegistry.ITEM.register( ( stack, layer ) -> layer == 1 ? ItemTreasureDisk.getColour( stack ) : 0xFFFFFF,
            ComputerCraftRegistry.ModItems.TREASURE_DISK );

        ColorProviderRegistry.ITEM.register( ( stack, layer ) -> {
            switch( layer )
            {
                case 0:
                default:
                    return 0xFFFFFF;
                case 1: // Frame colour
                    return IColouredItem.getColourBasic( stack );
                case 2: // Light colour
                    int light = ItemPocketComputer.getLightState( stack );
                    return light == -1 ? Colour.BLACK.getHex() : light;
            }
        }, ComputerCraftRegistry.ModItems.POCKET_COMPUTER_NORMAL, ComputerCraftRegistry.ModItems.POCKET_COMPUTER_ADVANCED );

        // Setup turtle colours
        ColorProviderRegistry.ITEM.register( ( stack, tintIndex ) -> tintIndex == 0 ? ((IColouredItem) stack.getItem()).getColour( stack ) : 0xFFFFFF,
            ComputerCraftRegistry.ModBlocks.TURTLE_NORMAL,
            ComputerCraftRegistry.ModBlocks.TURTLE_ADVANCED );
    }

    private static BakedModel bake( ModelBakery loader, UnbakedModel model, ResourceLocation identifier )
    {
        model.getMaterials( loader::getModel, new HashSet<>() );
        return model.bake( loader,
            spriteIdentifier -> Minecraft.getInstance()
                .getTextureAtlas( spriteIdentifier.atlasLocation() )
                .apply( spriteIdentifier.texture() ),
            BlockModelRotation.X0_Y0,
            identifier );
    }
}

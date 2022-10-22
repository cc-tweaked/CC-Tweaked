/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import net.minecraft.data.*;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.util.Optional;

import static net.minecraft.data.ModelsResourceUtil.getModelLocation;

public final class ItemModelGenerator
{
    private ItemModelGenerator()
    {
    }

    public static void addItemModels( ItemModelProvider generators )
    {
        registerDisk( generators, Registry.ModItems.DISK.get() );
        registerDisk( generators, Registry.ModItems.TREASURE_DISK.get() );

        registerPocketComputer( generators, getModelLocation( Registry.ModItems.POCKET_COMPUTER_NORMAL.get() ), false );
        registerPocketComputer( generators, getModelLocation( Registry.ModItems.POCKET_COMPUTER_ADVANCED.get() ), false );
        registerPocketComputer( generators, new ResourceLocation( ComputerCraft.MOD_ID, "item/pocket_computer_colour" ), true );

        generators.generateFlatItem( Registry.ModItems.PRINTED_BOOK.get(), StockModelShapes.FLAT_ITEM );
        generators.generateFlatItem( Registry.ModItems.PRINTED_PAGE.get(), StockModelShapes.FLAT_ITEM );
        generators.generateFlatItem( Registry.ModItems.PRINTED_PAGES.get(), StockModelShapes.FLAT_ITEM );
    }

    private static void registerPocketComputer( ItemModelProvider generators, ResourceLocation id, boolean off )
    {
        createFlatItem( generators, addSuffix( id, "_blinking" ),
            new ResourceLocation( ComputerCraft.MOD_ID, "item/pocket_computer_blink" ),
            id,
            new ResourceLocation( ComputerCraft.MOD_ID, "item/pocket_computer_light" )
        );

        createFlatItem( generators, addSuffix( id, "_on" ),
            new ResourceLocation( ComputerCraft.MOD_ID, "item/pocket_computer_on" ),
            id,
            new ResourceLocation( ComputerCraft.MOD_ID, "item/pocket_computer_light" )
        );

        // Don't emit the default/off state for advanced/normal pocket computers, as they have item overrides.
        if( off )
        {
            createFlatItem( generators, id,
                new ResourceLocation( ComputerCraft.MOD_ID, "item/pocket_computer_frame" ),
                id
            );
        }
    }

    private static void registerDisk( ItemModelProvider generators, Item item )
    {
        createFlatItem( generators, item,
            new ResourceLocation( ComputerCraft.MOD_ID, "item/disk_frame" ),
            new ResourceLocation( ComputerCraft.MOD_ID, "item/disk_colour" )
        );
    }

    private static void createFlatItem( ItemModelProvider generators, Item item, ResourceLocation... ids )
    {
        createFlatItem( generators, getModelLocation( item ), ids );
    }

    /**
     * Generate a flat item from an arbitrary number of layers.
     *
     * @param generators The current item generator helper.
     * @param model      The model we're writing to.
     * @param textures   The textures which make up this model.
     * @see net.minecraft.client.renderer.model.ItemModelGenerator The parser for this file format.
     */
    private static void createFlatItem( ItemModelProvider generators, ResourceLocation model, ResourceLocation... textures )
    {
        if( textures.length > 5 ) throw new IndexOutOfBoundsException( "Too many layers" );
        if( textures.length == 0 ) throw new IndexOutOfBoundsException( "Must have at least one texture" );
        if( textures.length == 1 )
        {
            StockModelShapes.FLAT_ITEM.create( model, ModelTextures.layer0( textures[0] ), generators.output );
            return;
        }

        StockTextureAliases[] slots = new StockTextureAliases[textures.length];
        ModelTextures mapping = new ModelTextures();
        for( int i = 0; i < textures.length; i++ )
        {
            StockTextureAliases slot = slots[i] = StockTextureAliases.create( "layer" + i );
            mapping.put( slot, textures[i] );
        }

        new ModelsUtil( Optional.of( new ResourceLocation( "item/generated" ) ), Optional.empty(), slots )
            .create( model, mapping, generators.output );
    }

    private static ResourceLocation addSuffix( ResourceLocation location, String suffix )
    {
        return new ResourceLocation( location.getNamespace(), location.getPath() + suffix );
    }
}

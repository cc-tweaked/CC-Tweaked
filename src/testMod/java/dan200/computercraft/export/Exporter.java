/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.export;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dan200.computercraft.ComputerCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides a {@literal /ccexport <path>} command which exports icons and recipes for all ComputerCraft items.
 */
public class Exporter
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static <S> void register( CommandDispatcher<S> dispatcher )
    {
        dispatcher.register(
            LiteralArgumentBuilder.<S>literal( "ccexport" )
                .then( RequiredArgumentBuilder.<S, String>argument( "path", StringArgumentType.string() )
                    .executes( c -> {
                        run( c.getArgument( "name", String.class ) );
                        return 0;
                    } ) ) );
    }

    private static void run( String path )
    {
        Path output = new File( path ).getAbsoluteFile().toPath();
        if( !Files.isDirectory( output ) )
        {
            Minecraft.getInstance().gui.getChat().addMessage( Component.literal( "Output path does not exist" ) );
            return;
        }

        RenderSystem.assertOnRenderThread();
        try( ImageRenderer renderer = new ImageRenderer() )
        {
            export( output, renderer );
        }
        catch( IOException e )
        {
            throw new UncheckedIOException( e );
        }

        Minecraft.getInstance().gui.getChat().addMessage( Component.literal( "Export finished!" ) );
    }

    private static void export( Path root, ImageRenderer renderer ) throws IOException
    {
        JsonDump dump = new JsonDump();

        Set<Item> items = new HashSet<>();

        // First find all CC items
        for( Item item : ForgeRegistries.ITEMS )
        {
            if( ForgeRegistries.ITEMS.getKey( item ).getNamespace().equals( ComputerCraft.MOD_ID ) ) items.add( item );
        }

        // Now find all CC recipes.
        for( CraftingRecipe recipe : Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor( RecipeType.CRAFTING ) )
        {
            ItemStack result = recipe.getResultItem();
            if( !ForgeRegistries.ITEMS.getKey( result.getItem() ).getNamespace().equals( ComputerCraft.MOD_ID ) )
            {
                continue;
            }
            if( result.hasTag() )
            {
                ComputerCraft.log.warn( "Skipping recipe {} as it has NBT", recipe.getId() );
                continue;
            }

            if( recipe instanceof ShapedRecipe shaped )
            {
                JsonDump.Recipe converted = new JsonDump.Recipe( result );

                for( int x = 0; x < shaped.getWidth(); x++ )
                {
                    for( int y = 0; y < shaped.getHeight(); y++ )
                    {
                        Ingredient ingredient = shaped.getIngredients().get( x + y * shaped.getWidth() );
                        if( ingredient.isEmpty() ) continue;

                        converted.setInput( x + y * 3, ingredient, items );
                    }
                }

                dump.recipes.put( recipe.getId().toString(), converted );
            }
            else if( recipe instanceof ShapelessRecipe shapeless )
            {
                JsonDump.Recipe converted = new JsonDump.Recipe( result );

                NonNullList<Ingredient> ingredients = shapeless.getIngredients();
                for( int i = 0; i < ingredients.size(); i++ )
                {
                    converted.setInput( i, ingredients.get( i ), items );
                }

                dump.recipes.put( recipe.getId().toString(), converted );
            }
            else
            {
                ComputerCraft.log.info( "Don't know how to handle recipe {}", recipe );
            }
        }

        Path itemDir = root.resolve( "items" );
        if( Files.exists( itemDir ) ) MoreFiles.deleteRecursively( itemDir, RecursiveDeleteOption.ALLOW_INSECURE );

        renderer.setupState();
        for( Item item : items )
        {
            ItemStack stack = new ItemStack( item );
            ResourceLocation location = ForgeRegistries.ITEMS.getKey( item );

            dump.itemNames.put( location.toString(), stack.getHoverName().getString() );
            renderer.captureRender( itemDir.resolve( location.getNamespace() ).resolve( location.getPath() + ".png" ),
                () -> Minecraft.getInstance().getItemRenderer().renderAndDecorateFakeItem( stack, 0, 0 )
            );
        }
        renderer.clearState();

        try( Writer writer = Files.newBufferedWriter( root.resolve( "index.json" ) ) )
        {
            GSON.toJson( dump, writer );
        }
    }
}

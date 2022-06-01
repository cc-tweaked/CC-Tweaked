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
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.ingame.mod.TestMod;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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
@Mod.EventBusSubscriber( modid = TestMod.MOD_ID, value = Dist.CLIENT )
public class Exporter
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SubscribeEvent
    public static void onClientCommands( ClientChatEvent event )
    {
        String prefix = "/ccexport";
        if( !event.getMessage().startsWith( prefix ) ) return;
        event.setCanceled( true );

        Path output = new File( event.getMessage().substring( prefix.length() ).trim() ).getAbsoluteFile().toPath();
        if( !Files.isDirectory( output ) )
        {
            Minecraft.getInstance().gui.getChat().addMessage( new StringTextComponent( "Output path does not exist" ) );
            return;
        }

        RenderSystem.assertThread( RenderSystem::isOnRenderThread );
        try( ImageRenderer renderer = new ImageRenderer() )
        {
            export( output, renderer );
        }
        catch( IOException e )
        {
            throw new UncheckedIOException( e );
        }

        Minecraft.getInstance().gui.getChat().addMessage( new StringTextComponent( "Export finished!" ) );
    }

    private static void export( Path root, ImageRenderer renderer ) throws IOException
    {
        JsonDump dump = new JsonDump();

        Set<Item> items = new HashSet<>();

        // First find all CC items
        for( Item item : ForgeRegistries.ITEMS.getValues() )
        {
            if( item.getRegistryName().getNamespace().equals( ComputerCraft.MOD_ID ) ) items.add( item );
        }

        // Now find all CC recipes.
        for( ICraftingRecipe recipe : Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor( IRecipeType.CRAFTING ) )
        {
            ItemStack result = recipe.getResultItem();
            if( !result.getItem().getRegistryName().getNamespace().equals( ComputerCraft.MOD_ID ) ) continue;
            if( result.hasTag() )
            {
                ComputerCraft.log.warn( "Skipping recipe {} as it has NBT", recipe.getId() );
                continue;
            }

            if( recipe instanceof ShapedRecipe )
            {
                JsonDump.Recipe converted = new JsonDump.Recipe( result );

                ShapedRecipe shaped = (ShapedRecipe) recipe;
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
            else if( recipe instanceof ShapelessRecipe )
            {
                JsonDump.Recipe converted = new JsonDump.Recipe( result );

                ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
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
            dump.itemNames.put( item.getRegistryName().toString(), stack.getHoverName().getString() );

            ResourceLocation location = item.getRegistryName();
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

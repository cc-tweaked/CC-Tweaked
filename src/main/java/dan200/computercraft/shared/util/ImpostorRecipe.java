/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.google.gson.JsonObject;
import dan200.computercraft.ComputerCraft;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeSerializers;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nonnull;

public class ImpostorRecipe extends ShapedRecipe
{
    private final String group;

    private ImpostorRecipe( @Nonnull ResourceLocation id, @Nonnull String group, int width, int height, NonNullList<Ingredient> ingredients, @Nonnull ItemStack result )
    {
        super( id, group, width, height, ingredients, result );
        this.group = group;
    }

    @Nonnull
    @Override
    public String getGroup()
    {
        return group;
    }

    @Override
    public boolean matches( @Nonnull IInventory inv, World world )
    {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult( @Nonnull IInventory inventory )
    {
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public IRecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    private static final ResourceLocation ID = new ResourceLocation( ComputerCraft.MOD_ID, "impostor_shaped" );
    public static final IRecipeSerializer<ImpostorRecipe> SERIALIZER = new IRecipeSerializer<ImpostorRecipe>()
    {
        @Override
        public ImpostorRecipe read( @Nonnull ResourceLocation identifier, @Nonnull JsonObject json )
        {
            String group = JsonUtils.getString( json, "group", "" );
            ShapedRecipe recipe = RecipeSerializers.CRAFTING_SHAPED.read( identifier, json );
            ItemStack result = CraftingHelper.getItemStack( JsonUtils.getJsonObject( json, "result" ), false );
            return new ImpostorRecipe( identifier, group, recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), result );
        }

        @Override
        public ImpostorRecipe read( @Nonnull ResourceLocation identifier, @Nonnull PacketBuffer buf )
        {
            int width = buf.readVarInt();
            int height = buf.readVarInt();
            String group = buf.readString( Short.MAX_VALUE );
            NonNullList<Ingredient> items = NonNullList.withSize( width * height, Ingredient.EMPTY );
            for( int k = 0; k < items.size(); ++k ) items.set( k, Ingredient.read( buf ) );
            ItemStack result = buf.readItemStack();
            return new ImpostorRecipe( identifier, group, width, height, items, result );
        }

        @Override
        public void write( @Nonnull PacketBuffer buf, @Nonnull ImpostorRecipe recipe )
        {
            buf.writeVarInt( recipe.getRecipeWidth() );
            buf.writeVarInt( recipe.getRecipeHeight() );
            buf.writeString( recipe.getGroup() );
            for( Ingredient ingredient : recipe.getIngredients() ) ingredient.write( buf );
            buf.writeItemStack( recipe.getRecipeOutput() );
        }

        @Nonnull
        @Override
        public ResourceLocation getName()
        {
            return ID;
        }
    };
}

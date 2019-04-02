/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.*;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.NonNullList;

import java.util.Map;
import java.util.Set;

// TODO: Replace some things with Forge??

public final class RecipeUtil
{
    private RecipeUtil() {}

    public static class ShapedTemplate
    {
        public final int width;
        public final int height;
        public final NonNullList<Ingredient> ingredients;

        public ShapedTemplate( int width, int height, NonNullList<Ingredient> ingredients )
        {
            this.width = width;
            this.height = height;
            this.ingredients = ingredients;
        }
    }

    public static ShapedTemplate getTemplate( JsonObject json )
    {
        Map<Character, Ingredient> ingMap = Maps.newHashMap();
        for( Map.Entry<String, JsonElement> entry : JsonUtils.getJsonObject( json, "key" ).entrySet() )
        {
            if( entry.getKey().length() != 1 )
            {
                throw new JsonSyntaxException( "Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only)." );
            }
            if( " ".equals( entry.getKey() ) )
            {
                throw new JsonSyntaxException( "Invalid key entry: ' ' is a reserved symbol." );
            }

            ingMap.put( entry.getKey().charAt( 0 ), Ingredient.deserialize( entry.getValue() ) );
        }

        ingMap.put( ' ', Ingredient.EMPTY );

        JsonArray patternJ = JsonUtils.getJsonArray( json, "pattern" );

        if( patternJ.size() == 0 )
        {
            throw new JsonSyntaxException( "Invalid pattern: empty pattern not allowed" );
        }

        String[] pattern = new String[patternJ.size()];
        for( int x = 0; x < pattern.length; x++ )
        {
            String line = JsonUtils.getString( patternJ.get( x ), "pattern[" + x + "]" );
            if( x > 0 && pattern[0].length() != line.length() )
            {
                throw new JsonSyntaxException( "Invalid pattern: each row must  be the same width" );
            }
            pattern[x] = line;
        }

        int width = pattern[0].length();
        int height = pattern.length;
        NonNullList<Ingredient> ingredients = NonNullList.withSize( width * height, Ingredient.EMPTY );

        Set<Character> missingKeys = Sets.newHashSet( ingMap.keySet() );
        missingKeys.remove( ' ' );

        int i = 0;
        for( String line : pattern )
        {
            for( char chr : line.toCharArray() )
            {
                Ingredient ing = ingMap.get( chr );
                if( ing == null )
                {
                    throw new JsonSyntaxException( "Pattern references symbol '" + chr + "' but it's not defined in the key" );
                }
                ingredients.set( i++, ing );
                missingKeys.remove( chr );
            }
        }

        if( !missingKeys.isEmpty() )
        {
            throw new JsonSyntaxException( "Key defines symbols that aren't used in pattern: " + missingKeys );
        }

        return new ShapedTemplate( width, height, ingredients );
    }

    public static NonNullList<Ingredient> getIngredients( JsonObject json )
    {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for( JsonElement ele : JsonUtils.getJsonArray( json, "ingredients" ) )
        {
            ingredients.add( Ingredient.deserialize( ele ) );
        }

        if( ingredients.isEmpty() ) throw new JsonParseException( "No ingredients for recipe" );
        return ingredients;
    }

    public static ComputerFamily getFamily( JsonObject json, String name )
    {
        String familyName = JsonUtils.getString( json, name );
        try
        {
            return ComputerFamily.valueOf( familyName );
        }
        catch( IllegalArgumentException e )
        {
            throw new JsonSyntaxException( "Unknown computer family '" + familyName + "' for field " + name );
        }
    }
}

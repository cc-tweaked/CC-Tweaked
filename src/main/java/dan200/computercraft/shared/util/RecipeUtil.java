/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
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
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.JsonContext;

import java.util.Map;
import java.util.Set;

public class RecipeUtil
{
    public static CraftingHelper.ShapedPrimer getPrimer( JsonContext context, JsonObject json )
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

            ingMap.put( entry.getKey().charAt( 0 ), CraftingHelper.getIngredient( entry.getValue(), context ) );
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

        CraftingHelper.ShapedPrimer primer = new CraftingHelper.ShapedPrimer();
        primer.width = pattern[0].length();
        primer.height = pattern.length;
        primer.mirrored = false;
        primer.input = NonNullList.withSize( primer.width * primer.height, Ingredient.EMPTY );

        Set<Character> keys = Sets.newHashSet( ingMap.keySet() );
        keys.remove( ' ' );

        int x = 0;
        for( String line : pattern )
        {
            for( char chr : line.toCharArray() )
            {
                Ingredient ing = ingMap.get( chr );
                if( ing == null )
                {
                    throw new JsonSyntaxException( "Pattern references symbol '" + chr + "' but it's not defined in the key" );
                }
                primer.input.set( x++, ing );
                keys.remove( chr );
            }
        }

        if( !keys.isEmpty() )
        {
            throw new JsonSyntaxException( "Key defines symbols that aren't used in pattern: " + keys );
        }

        return primer;
    }

    public static NonNullList<Ingredient> getIngredients( JsonContext context, JsonObject json )
    {
        NonNullList<Ingredient> ings = NonNullList.create();
        for( JsonElement ele : JsonUtils.getJsonArray( json, "ingredients" ) )
        {
            ings.add( CraftingHelper.getIngredient( ele, context ) );
        }

        if( ings.isEmpty() )
        {
            throw new JsonParseException( "No ingredients for recipe" );
        }

        return ings;
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

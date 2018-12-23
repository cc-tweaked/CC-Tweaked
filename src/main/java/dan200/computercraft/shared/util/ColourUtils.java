package dan200.computercraft.shared.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;

public final class ColourUtils
{
    private static final String[] DYES = new String[] {
        "dyeBlack", "dyeRed", "dyeGreen", "dyeBrown",
        "dyeBlue", "dyePurple", "dyeCyan", "dyeLightGray",
        "dyeGray", "dyePink", "dyeLime", "dyeYellow",
        "dyeLightBlue", "dyeMagenta", "dyeOrange", "dyeWhite"
    };

    private static int[] ids;

    public static int getStackColour( ItemStack stack )
    {
        if( ids == null )
        {
            int ids[] = ColourUtils.ids = new int[DYES.length];
            for( int i = 0; i < DYES.length; i++ )
            {
                ids[i] = OreDictionary.getOreID( DYES[i] );
            }
        }

        for( int id : OreDictionary.getOreIDs( stack ) )
        {
            int index = ArrayUtils.indexOf( ids, id );
            if( index >= 0 ) return index;
        }

        return -1;
    }

    public static int getHexColour( @Nonnull NBTTagCompound tag )
    {
        if( tag.hasKey( "colourIndex", Constants.NBT.TAG_ANY_NUMERIC ) )
        {
            return Colour.VALUES[tag.getInteger( "colourIndex" ) & 0xF].getHex();
        }
        else if( tag.hasKey( "colour", Constants.NBT.TAG_ANY_NUMERIC ) )
        {
            return tag.getInteger( "colour" );
        }
        else if( tag.hasKey( "color", Constants.NBT.TAG_ANY_NUMERIC ) )
        {
            return tag.getInteger( "color" );
        }
        else
        {
            return -1;
        }
    }

    public static Colour getColour( @Nonnull NBTTagCompound tag )
    {
        if( tag.hasKey( "colourIndex", Constants.NBT.TAG_ANY_NUMERIC ) )
        {
            return Colour.fromInt( tag.getInteger( "colourIndex" ) & 0xF );
        }
        else
        {
            return null;
        }
    }

}

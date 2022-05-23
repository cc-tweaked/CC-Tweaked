/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.data.providers;

import dan200.computercraft.api.detail.BasicItemDetailProvider;
import dan200.computercraft.shared.media.items.ItemPrintout;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class PrintoutItemDetailProvider extends BasicItemDetailProvider<ItemPrintout>
{
    public PrintoutItemDetailProvider()
    {
        super( "printout", ItemPrintout.class );
    }

    @Override
    public void provideDetails( @Nonnull Map<? super String, Object> data, @Nonnull ItemStack stack,
                                @Nonnull ItemPrintout printout )
    {
        data.put( "type", printout.getType().toString() );
        data.put( "title", ItemPrintout.getTitle( stack ) );
        data.put( "pages", ItemPrintout.getPageCount( stack ) );

        Map<Integer, String> lines = new HashMap<>();
        String[] lineArray = ItemPrintout.getText( stack );
        for( int i = 0; i < lineArray.length; i++ )
        {
            lines.put( i + 1, lineArray[i] );
        }
        data.put( "lines", lines );
    }
}

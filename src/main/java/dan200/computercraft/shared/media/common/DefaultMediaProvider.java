/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.media.common;

import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.IMediaProvider;
import dan200.computercraft.shared.media.items.RecordMedia;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class DefaultMediaProvider implements IMediaProvider
{
    @Override
    public IMedia getMedia( @Nonnull ItemStack stack )
    {
        Item item = stack.getItem();
        if( item instanceof IMedia ) return (IMedia) item;
        if( item instanceof ItemRecord ) return RecordMedia.INSTANCE;
        return null;
    }
}

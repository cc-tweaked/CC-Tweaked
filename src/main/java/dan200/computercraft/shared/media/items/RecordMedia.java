/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.media.items;

import dan200.computercraft.api.media.IMedia;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;

import javax.annotation.Nonnull;

/**
 * An implementation of IMedia for ItemRecords.
 */
public final class RecordMedia implements IMedia
{
    public static final RecordMedia INSTANCE = new RecordMedia();

    private RecordMedia()
    {
    }

    @Override
    public String getLabel( @Nonnull ItemStack stack )
    {
        return getAudioTitle( stack );
    }

    @Override
    public String getAudioTitle( @Nonnull ItemStack stack )
    {
        Item item = stack.getItem();
        if( !(item instanceof RecordItem) ) return null;

        return new TranslatableComponent( item.getDescriptionId() + ".desc" ).getString();
    }

    @Override
    public SoundEvent getAudio( @Nonnull ItemStack stack )
    {
        Item item = stack.getItem();
        if( !(item instanceof RecordItem) ) return null;

        return ((RecordItem) item).getSound();
    }
}

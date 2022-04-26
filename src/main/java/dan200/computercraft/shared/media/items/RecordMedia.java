/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.media.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.media.IMedia;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper.UnableToAccessFieldException;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper.UnableToFindFieldException;

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
        if( !(item instanceof MusicDiscItem) ) return null;

        return new TranslationTextComponent( item.getDescriptionId() + ".desc" ).getString();
    }

    @Override
    public SoundEvent getAudio( @Nonnull ItemStack stack )
    {
        Item item = stack.getItem();
        if( !(item instanceof MusicDiscItem) ) return null;

        try
        {
            return ObfuscationReflectionHelper.getPrivateValue( MusicDiscItem.class, (MusicDiscItem) item, "field_185076_b" );
        }
        catch( UnableToAccessFieldException | UnableToFindFieldException e )
        {
            ComputerCraft.log.error( "Cannot get disk sound", e );
            return null;
        }
    }
}

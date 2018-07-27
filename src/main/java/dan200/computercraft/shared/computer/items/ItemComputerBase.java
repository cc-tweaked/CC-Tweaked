/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class ItemComputerBase extends ItemBlock implements IComputerItem, IMedia
{
    protected ItemComputerBase( Block block )
    {
        super( block );
    }

    @Override
    public int getMetadata( int damage )
    {
        return super.getMetadata( damage );
    }

    @Override
    public void addInformation( @Nonnull ItemStack stack, @Nullable World world, @Nonnull List<String> list, @Nonnull ITooltipFlag flag )
    {
        if( flag.isAdvanced() )
        {
            int id = getComputerID( stack );
            if( id >= 0 )
            {
                list.add( "(Computer ID: " + id + ")" );
            }
        }
    }

    // IComputerItem implementation

    @Override
    public abstract int getComputerID( @Nonnull ItemStack stack );

    @Override
    public String getLabel( @Nonnull ItemStack stack )
    {
        if( stack.hasDisplayName() )
        {
            return stack.getDisplayName();
        }
        return null;
    }

    // IMedia implementation

    @Override
    public boolean setLabel( @Nonnull ItemStack stack, String label )
    {
        if( label != null )
        {
            stack.setStackDisplayName( label );
        }
        else
        {
            stack.clearCustomName();
        }
        return true;
    }

    @Override
    public String getAudioTitle( @Nonnull ItemStack stack )
    {
        return null;
    }

    @Override
    public SoundEvent getAudio( @Nonnull ItemStack stack )
    {
        return null;
    }

    @Override
    public IMount createDataMount( @Nonnull ItemStack stack, @Nonnull World world )
    {
        ComputerFamily family = getFamily();
        if( family != ComputerFamily.Command )
        {
            int id = getComputerID( stack );
            if( id >= 0 )
            {
                return ComputerCraft.createSaveDirMount( world, "computer/" + id, ComputerCraft.computerSpaceLimit );
            }
        }
        return null;
    }
}

/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.util.StringUtil;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
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

    public abstract ComputerFamily getFamily( int damage );

    @Override
    public final int getMetadata( int damage )
    {
        return damage;
    }

    @Override
    public void addInformation( @Nonnull ItemStack stack, @Nullable World world, @Nonnull List<String> list, @Nonnull ITooltipFlag flag )
    {
        if( flag.isAdvanced() || getLabel( stack ) == null )
        {
            int id = getComputerID( stack );
            if( id >= 0 ) list.add( StringUtil.translateFormatted( "gui.computercraft.tooltip.computer_id", id ) );
        }
    }

    @Override
    public String getLabel( @Nonnull ItemStack stack )
    {
        return IComputerItem.super.getLabel( stack );
    }

    @Override
    public final ComputerFamily getFamily( @Nonnull ItemStack stack )
    {
        return getFamily( stack.getItemDamage() );
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
    public IMount createDataMount( @Nonnull ItemStack stack, @Nonnull World world )
    {
        ComputerFamily family = getFamily( stack );
        if( family != ComputerFamily.Command )
        {
            int id = getComputerID( stack );
            if( id >= 0 )
            {
                return ComputerCraftAPI.createSaveDirMount( world, "computer/" + id, ComputerCraft.computerSpaceLimit );
            }
        }
        return null;
    }
}

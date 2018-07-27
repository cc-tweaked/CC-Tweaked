package dan200.computercraft.shared.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public interface IDefaultInventory extends IInventory
{
    @Override
    default int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    default boolean isUsableByPlayer( @Nonnull EntityPlayer player )
    {
        return true;
    }

    @Override
    default void openInventory( @Nonnull EntityPlayer player )
    {
    }

    @Override
    default void closeInventory( @Nonnull EntityPlayer player )
    {
    }

    @Override
    default boolean isItemValidForSlot( int index, @Nonnull ItemStack stack )
    {
        return true;
    }

    @Override
    default int getField( int id )
    {
        return 0;
    }

    @Override
    default void setField( int id, int value )
    {
    }

    @Override
    default int getFieldCount()
    {
        return 0;
    }

    @Override
    default boolean hasCustomName()
    {
        return false;
    }
}

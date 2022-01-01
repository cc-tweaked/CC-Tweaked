/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

/**
 * Control the current pocket computer, adding or removing upgrades.
 *
 * This API is only available on pocket computers. As such, you may use its presence to determine what kind of computer
 * you are using:
 *
 * <pre>{@code
 * if pocket then
 *   print("On a pocket computer")
 * else
 *   print("On something else")
 * end
 * }</pre>
 *
 * @cc.module pocket
 */
public class PocketAPI implements ILuaAPI
{
    private final PocketServerComputer computer;

    public PocketAPI( PocketServerComputer computer )
    {
        this.computer = computer;
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "pocket" };
    }

    /**
     * Search the player's inventory for another upgrade, replacing the existing one with that item if found.
     *
     * This inventory search starts from the player's currently selected slot, allowing you to prioritise upgrades.
     *
     * @return The result of equipping.
     * @cc.treturn boolean If an item was equipped.
     * @cc.treturn string|nil The reason an item was not equipped.
     */
    @LuaFunction( mainThread = true )
    public final Object[] equipBack()
    {
        Entity entity = computer.getEntity();
        if( !(entity instanceof Player player) ) return new Object[] { false, "Cannot find player" };
        Inventory inventory = player.getInventory();
        IPocketUpgrade previousUpgrade = computer.getUpgrade();

        // Attempt to find the upgrade, starting in the main segment, and then looking in the opposite
        // one. We start from the position the item is currently in and loop round to the start.
        IPocketUpgrade newUpgrade = findUpgrade( inventory.items, inventory.selected, previousUpgrade );
        if( newUpgrade == null )
        {
            newUpgrade = findUpgrade( inventory.offhand, 0, previousUpgrade );
        }
        if( newUpgrade == null ) return new Object[] { false, "Cannot find a valid upgrade" };

        // Remove the current upgrade
        if( previousUpgrade != null )
        {
            ItemStack stack = previousUpgrade.getCraftingItem();
            if( !stack.isEmpty() )
            {
                stack = InventoryUtil.storeItems( stack, new PlayerMainInvWrapper( inventory ), inventory.selected );
                if( !stack.isEmpty() )
                {
                    WorldUtil.dropItemStack( stack, player.getCommandSenderWorld(), player.position() );
                }
            }
        }

        // Set the new upgrade
        computer.setUpgrade( newUpgrade );

        return new Object[] { true };
    }

    /**
     * Remove the pocket computer's current upgrade.
     *
     * @return The result of unequipping.
     * @cc.treturn boolean If the upgrade was unequipped.
     * @cc.treturn string|nil The reason an upgrade was not unequipped.
     */
    @LuaFunction( mainThread = true )
    public final Object[] unequipBack()
    {
        Entity entity = computer.getEntity();
        if( !(entity instanceof Player player) ) return new Object[] { false, "Cannot find player" };
        Inventory inventory = player.getInventory();
        IPocketUpgrade previousUpgrade = computer.getUpgrade();

        if( previousUpgrade == null ) return new Object[] { false, "Nothing to unequip" };

        computer.setUpgrade( null );

        ItemStack stack = previousUpgrade.getCraftingItem();
        if( !stack.isEmpty() )
        {
            stack = InventoryUtil.storeItems( stack, new PlayerMainInvWrapper( inventory ), inventory.selected );
            if( stack.isEmpty() )
            {
                WorldUtil.dropItemStack( stack, player.getCommandSenderWorld(), player.position() );
            }
        }

        return new Object[] { true };
    }

    private static IPocketUpgrade findUpgrade( NonNullList<ItemStack> inv, int start, IPocketUpgrade previous )
    {
        for( int i = 0; i < inv.size(); i++ )
        {
            ItemStack invStack = inv.get( (i + start) % inv.size() );
            if( !invStack.isEmpty() )
            {
                IPocketUpgrade newUpgrade = PocketUpgrades.instance().get( invStack );

                if( newUpgrade != null && newUpgrade != previous )
                {
                    // Consume an item from this stack and exit the loop
                    invStack = invStack.copy();
                    invStack.shrink( 1 );
                    inv.set( (i + start) % inv.size(), invStack.isEmpty() ? ItemStack.EMPTY : invStack );

                    return newUpgrade;
                }
            }
        }

        return null;
    }
}

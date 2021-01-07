/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.crafttweaker.actions;

import com.blamejared.crafttweaker.api.actions.IUndoableAction;
import com.blamejared.crafttweaker.api.logger.ILogger;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.TurtleUpgrades;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;

/**
 * Removes a turtle upgrade crafted with the given stack.
 */
public class RemoveTurtleUpgradeByItem implements IUndoableAction
{
    private final ItemStack stack;
    private ITurtleUpgrade upgrade;

    public RemoveTurtleUpgradeByItem( ItemStack stack )
    {
        this.stack = stack;
    }

    @Override
    public void apply()
    {
        ITurtleUpgrade upgrade = this.upgrade = TurtleUpgrades.get( stack );
        if( upgrade != null ) TurtleUpgrades.disable( upgrade );
    }

    @Override
    public String describe()
    {
        return String.format( "Remove turtle upgrades crafted with '%s'", stack );
    }

    @Override
    public void undo()
    {
        if( this.upgrade != null ) TurtleUpgrades.enable( upgrade );
    }

    @Override
    public String describeUndo()
    {
        return String.format( "Adding back turtle upgrades crafted with '%s'", stack );
    }

    @Override
    public boolean validate( ILogger logger )
    {
        if( TurtleUpgrades.get( stack ) == null )
        {
            logger.error( String.format( "Unknown turtle upgrade crafted with '%s'.", stack ) );
            return false;
        }

        return true;
    }

    @Override
    public boolean shouldApplyOn( LogicalSide side )
    {
        return true;
    }
}

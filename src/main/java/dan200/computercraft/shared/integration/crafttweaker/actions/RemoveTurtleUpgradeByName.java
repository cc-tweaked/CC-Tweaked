/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.crafttweaker.actions;

import com.blamejared.crafttweaker.api.actions.IUndoableAction;
import com.blamejared.crafttweaker.api.logger.ILogger;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.TurtleUpgrades;
import net.minecraftforge.fml.LogicalSide;

/**
 * Removes a turtle upgrade with the given id.
 */
public class RemoveTurtleUpgradeByName implements IUndoableAction
{
    private final String id;
    private ITurtleUpgrade upgrade;

    public RemoveTurtleUpgradeByName( String id )
    {
        this.id = id;
    }

    @Override
    public void apply()
    {
        ITurtleUpgrade upgrade = this.upgrade = TurtleUpgrades.get( id );
        if( upgrade != null ) TurtleUpgrades.disable( upgrade );
    }

    @Override
    public String describe()
    {
        return String.format( "Remove turtle upgrade '%s'", id );
    }

    @Override
    public void undo()
    {
        if( upgrade != null ) TurtleUpgrades.enable( upgrade );
    }

    @Override
    public String describeUndo()
    {
        return String.format( "Adding back turtle upgrade '%s'", id );
    }

    @Override
    public boolean validate( ILogger logger )
    {
        if( TurtleUpgrades.get( id ) == null )
        {
            logger.error( String.format( "Unknown turtle upgrade '%s'.", id ) );
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

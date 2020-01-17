/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.crafttweaker.actions;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.TurtleUpgrades;

import java.util.Optional;

/**
 * Removes a turtle upgrade with the given id.
 */
public class RemoveTurtleUpgradeByName implements IAction
{
    private final String id;

    public RemoveTurtleUpgradeByName( String id )
    {
        this.id = id;
    }

    @Override
    public void apply()
    {
        ITurtleUpgrade upgrade = TurtleUpgrades.get( id );
        if( upgrade != null ) TurtleUpgrades.disable( upgrade );
    }

    @Override
    public String describe()
    {
        return String.format( "Remove turtle upgrade '%s'", id );
    }

    @Override
    public Optional<String> getValidationProblem()
    {
        if( TurtleUpgrades.get( id ) == null )
        {
            return Optional.of( String.format( "Unknown turtle upgrade '%s'.", id ) );
        }

        return Optional.empty();
    }
}

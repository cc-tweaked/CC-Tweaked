/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.crafttweaker.actions;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.TurtleUpgrades;
import net.minecraft.item.ItemStack;

import java.util.Optional;

/**
 * Removes a turtle upgrade crafted with the given stack.
 */
public class RemoveTurtleUpgradeByItem implements IAction
{
    private final ItemStack stack;

    public RemoveTurtleUpgradeByItem( ItemStack stack )
    {
        this.stack = stack;
    }

    @Override
    public void apply()
    {
        ITurtleUpgrade upgrade = TurtleUpgrades.get( stack );
        if( upgrade != null ) TurtleUpgrades.disable( upgrade );
    }

    @Override
    public String describe()
    {
        return String.format( "Remove turtle upgrades crafted with '%s'", stack );
    }

    @Override
    public Optional<String> getValidationProblem()
    {
        if( TurtleUpgrades.get( stack ) == null )
        {
            return Optional.of( String.format( "Unknown turtle upgrade crafted with '%s'.", stack ) );
        }

        return Optional.empty();
    }
}

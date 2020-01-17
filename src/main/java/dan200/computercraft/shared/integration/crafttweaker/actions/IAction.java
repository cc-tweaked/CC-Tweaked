/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.crafttweaker.actions;

import java.util.Optional;

/**
 * An extension of {@link IAction} with a single validation function, rather than two.
 */
public interface IAction extends crafttweaker.IAction
{
    default Optional<String> getValidationProblem()
    {
        return Optional.empty();
    }

    @Override
    default boolean validate()
    {
        return !getValidationProblem().isPresent();
    }

    @Override
    default String describeInvalid()
    {
        return getValidationProblem().orElse( "No problems found." );
    }
}

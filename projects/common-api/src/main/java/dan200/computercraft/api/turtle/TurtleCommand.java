// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.turtle;


/**
 * An interface for objects executing custom turtle commands, used with {@link ITurtleAccess#executeCommand(TurtleCommand)}.
 *
 * @see ITurtleAccess#executeCommand(TurtleCommand)
 */
@FunctionalInterface
public interface TurtleCommand {
    /**
     * Will be called by the turtle on the main thread when it is time to execute the custom command.
     * <p>
     * The handler should either perform the work of the command, and return success, or return
     * failure with an error message to indicate the command cannot be executed at this time.
     *
     * @param turtle Access to the turtle for whom the command was issued.
     * @return A result, indicating whether this action succeeded or not.
     * @see ITurtleAccess#executeCommand(TurtleCommand)
     * @see TurtleCommandResult#success()
     * @see TurtleCommandResult#failure(String)
     * @see TurtleCommandResult
     */
    TurtleCommandResult execute(ITurtleAccess turtle);
}

// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.TurtleCommand;

public record TurtleCommandQueueEntry(int callbackID, TurtleCommand command) {
}

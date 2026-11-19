// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.lua.MachineResult;
import org.jspecify.annotations.Nullable;

/**
 * A {@link ILuaMachine} which does no work.
 */
public class MockLuaMachine implements ILuaMachine {
    public static ILuaMachine.Factory factory() {
        return (m, b) -> new MockLuaMachine();
    }

    @Override
    public MachineResult handleEvent(@Nullable String eventName, @Nullable Object @Nullable [] arguments) {
        return MachineResult.OK;
    }

    @Override
    public void printExecutionState(StringBuilder out) {
    }

    @Override
    public void close() {
    }
}

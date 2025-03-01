// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import com.simibubi.create.content.contraptions.BlockMovementChecks;
import com.simibubi.create.content.contraptions.BlockMovementChecks.CheckResult;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemBlock;

/**
 * Integration with Create.
 */
public final class CreateIntegration {
    public static final String ID = "create";

    private CreateIntegration() {
    }

    public static void setup() {
        // Allow modems to be treated as "attached" to their adjacent block.
        BlockMovementChecks.registerAttachedCheck((state, world, pos, direction) -> {
            var block = state.getBlock();
            if (block instanceof WirelessModemBlock) {
                return CheckResult.of(state.getValue(WirelessModemBlock.FACING) == direction);
            } else if (block instanceof CableBlock) {
                return CheckResult.of(state.getValue(CableBlock.MODEM).getFacing() == direction);
            } else {
                return CheckResult.PASS;
            }
        });
    }
}

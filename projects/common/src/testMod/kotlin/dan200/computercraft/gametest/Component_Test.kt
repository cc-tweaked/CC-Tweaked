// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.GameTest
import dan200.computercraft.gametest.api.assertNoPeripheral
import dan200.computercraft.gametest.api.assertPeripheral
import dan200.computercraft.gametest.api.immediate
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.platform.ComponentAccess
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import java.util.*

/**
 * Checks that we expose [ComponentAccess] for various blocks/block entities
 */
class Component_Test {
    @GameTest(template = "default")
    fun Peripheral(context: GameTestHelper) = context.immediate {
        val pos = BlockPos(2, 2, 2)
        // We fetch peripherals from the NORTH, as that is the default direction for modems. This is a bit of a hack,
        // but avoids having to override the block state.
        val side = Direction.NORTH

        for ((block, type) in mapOf(
            // Computers
            ModRegistry.Blocks.COMPUTER_NORMAL to Optional.of("computer"),
            ModRegistry.Blocks.COMPUTER_ADVANCED to Optional.of("computer"),
            ModRegistry.Blocks.COMPUTER_COMMAND to Optional.empty(),
            // Turtles
            ModRegistry.Blocks.TURTLE_NORMAL to Optional.of("turtle"),
            ModRegistry.Blocks.TURTLE_ADVANCED to Optional.of("turtle"),
            // Peripherals
            ModRegistry.Blocks.SPEAKER to Optional.of("speaker"),
            ModRegistry.Blocks.DISK_DRIVE to Optional.of("drive"),
            ModRegistry.Blocks.PRINTER to Optional.of("printer"),
            ModRegistry.Blocks.MONITOR_NORMAL to Optional.of("monitor"),
            ModRegistry.Blocks.MONITOR_ADVANCED to Optional.of("monitor"),
            ModRegistry.Blocks.WIRELESS_MODEM_NORMAL to Optional.of("modem"),
            ModRegistry.Blocks.WIRELESS_MODEM_ADVANCED to Optional.of("modem"),
            ModRegistry.Blocks.WIRED_MODEM_FULL to Optional.of("modem"),
            ModRegistry.Blocks.REDSTONE_RELAY to Optional.of("redstone_relay"),
        )) {
            context.setBlock(pos, block.get())
            if (type.isPresent) {
                context.assertPeripheral(pos, side, type.get())
            } else {
                context.assertNoPeripheral(pos, side)
            }
        }
    }
}

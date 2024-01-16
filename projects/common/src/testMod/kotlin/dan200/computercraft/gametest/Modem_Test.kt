// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.api.lua.ObjectArguments
import dan200.computercraft.core.apis.PeripheralAPI
import dan200.computercraft.core.computer.ComputerSide
import dan200.computercraft.gametest.api.getBlockEntity
import dan200.computercraft.gametest.api.sequence
import dan200.computercraft.gametest.api.thenOnComputer
import dan200.computercraft.gametest.api.thenStartComputer
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock
import dan200.computercraft.test.core.assertArrayEquals
import dan200.computercraft.test.core.computer.LuaTaskContext
import dan200.computercraft.test.core.computer.getApi
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class Modem_Test {
    @GameTest
    fun Have_peripherals(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            assertEquals(listOf("monitor_0", "printer_0", "right"), getPeripheralNames(), "Starts with peripherals")
        }
    }

    @GameTest
    fun Gains_peripherals(helper: GameTestHelper) = helper.sequence {
        val position = BlockPos(2, 2, 2)
        thenOnComputer {
            assertEquals(listOf("back"), getPeripheralNames(), "Starts with peripherals")
        }
        thenExecute {
            helper.setBlock(
                position,
                CableBlock.correctConnections(
                    helper.level,
                    helper.absolutePos(position),
                    ModRegistry.Blocks.CABLE.get().defaultBlockState().setValue(CableBlock.CABLE, true),
                ),
            )
        }
        thenIdle(1)
        thenOnComputer {
            assertEquals(listOf("back", "monitor_1", "printer_1"), getPeripheralNames(), "Gains new peripherals")
        }
    }

    /**
     * Sends a modem message to another computer on the same network
     */
    @GameTest
    fun Transmits_messages(context: GameTestHelper) = context.sequence {
        thenStartComputer("send") {
            val modem = findPeripheral("modem") ?: throw IllegalStateException("Cannot find modem")
            while (true) {
                callPeripheral(modem, "transmit", 12, 34, "Hello")
                sleep(50.milliseconds)
            }
        }
        thenOnComputer("receive") {
            val modem = findPeripheral("modem") ?: throw IllegalStateException("Cannot find modem")
            callPeripheral(modem, "open", 12)

            pullEvent("modem_message")
                .assertArrayEquals("modem_message", "left", 12, 34, "Hello", 4, message = "Modem message")
        }
    }

    /**
     * Assert that full block modems act like cables.
     *
     * @see [#1278](https://github.com/cc-tweaked/CC-Tweaked/issues/1278)
     */
    @GameTest(setupTicks = 1)
    fun Full_modems_form_networks(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            val modem1 = helper.getBlockEntity(BlockPos(1, 2, 1), ModRegistry.BlockEntities.WIRED_MODEM_FULL.get())
            val modem2 = helper.getBlockEntity(BlockPos(3, 2, 1), ModRegistry.BlockEntities.WIRED_MODEM_FULL.get())
            assertEquals(modem1.element.node.network, modem2.element.node.network, "On the same network")
        }
    }
}

private fun LuaTaskContext.findPeripheral(type: String): String? {
    val peripheral = getApi<PeripheralAPI>()
    for (side in ComputerSide.NAMES) {
        val hasType = peripheral.hasType(side, type)
        if (hasType != null && hasType[0] == true) return side
    }

    return null
}

private suspend fun LuaTaskContext.getPeripheralNames(): List<String> {
    val peripheral = getApi<PeripheralAPI>()
    val peripherals = mutableListOf<String>()
    for (side in ComputerSide.NAMES) {
        if (!peripheral.isPresent(side)) continue
        peripherals.add(side)

        val hasType = peripheral.hasType(side, "modem")
        if (hasType == null || hasType[0] != true) continue

        val names = peripheral.call(context, ObjectArguments(side, "getNamesRemote")).await() ?: continue
        @Suppress("UNCHECKED_CAST")
        peripherals.addAll(names[0] as Collection<String>)
    }

    peripherals.sort()
    return peripherals
}

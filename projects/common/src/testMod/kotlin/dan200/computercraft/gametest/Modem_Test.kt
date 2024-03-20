// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.api.lua.ObjectArguments
import dan200.computercraft.core.apis.PeripheralAPI
import dan200.computercraft.core.computer.ComputerSide
import dan200.computercraft.gametest.api.*
import dan200.computercraft.impl.network.wired.WiredNodeImpl
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock
import dan200.computercraft.shared.peripheral.modem.wired.CableModemVariant
import dan200.computercraft.test.core.assertArrayEquals
import dan200.computercraft.test.core.computer.LuaTaskContext
import dan200.computercraft.test.core.computer.getApi
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
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
            assertEquals((modem1.element.node as WiredNodeImpl).network, (modem2.element.node as WiredNodeImpl).network, "On the same network")
        }
    }

    /**
     * Modems do not include the current peripheral when attached.
     */
    @GameTest
    fun Cable_modem_does_not_report_self(helper: GameTestHelper) = helper.sequence {
        // Modem does not report the computer as a peripheral.
        thenOnComputer { assertEquals(listOf("back", "right"), getPeripheralNames()) }

        // However, if we connect the network, the other modem does.
        thenExecute {
            helper.setBlock(
                BlockPos(1, 2, 3),
                ModRegistry.Blocks.CABLE.get().defaultBlockState().setValue(CableBlock.CABLE, true),
            )
        }
        thenIdle(2)
        thenOnComputer { assertEquals(listOf("back", "computer_0", "right"), getPeripheralNames()) }
    }

    /**
     * Modems do not include the current peripheral when attached.
     */
    @GameTest
    fun Full_block_modem_does_not_report_self(helper: GameTestHelper) = helper.sequence {
        // Modem does not report the computer as a peripheral.
        thenOnComputer { assertEquals(listOf("back", "right"), getPeripheralNames()) }

        // However, if we connect the network, the other modem does.
        thenExecute {
            helper.setBlock(
                BlockPos(1, 2, 3),
                ModRegistry.Blocks.CABLE.get().defaultBlockState().setValue(CableBlock.CABLE, true),
            )
        }
        thenIdle(2)
        thenOnComputer { assertEquals(listOf("back", "computer_1", "right"), getPeripheralNames()) }
    }

    /**
     * Test wired modems (without a cable) drop an item when the adjacent block is removed.
     */
    @GameTest
    fun Modem_drops_when_neighbour_removed(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            helper.setBlock(BlockPos(2, 3, 2), Blocks.AIR)
            helper.assertItemEntityPresent(ModRegistry.Items.WIRED_MODEM.get(), BlockPos(2, 2, 2), 0.0)
            helper.assertBlockPresent(Blocks.AIR, BlockPos(2, 2, 2))
        }
    }

    /**
     * Test wired modems (with a cable) drop an item, but keep their cable when the adjacent block is removed.
     */
    @GameTest
    fun Modem_keeps_cable_when_neighbour_removed(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            helper.setBlock(BlockPos(2, 3, 2), Blocks.AIR)
            helper.assertItemEntityPresent(ModRegistry.Items.WIRED_MODEM.get(), BlockPos(2, 2, 2), 0.0)
            helper.assertBlockIs(BlockPos(2, 2, 2)) {
                it.block == ModRegistry.Blocks.CABLE.get() && it.getValue(CableBlock.MODEM) == CableModemVariant.None && it.getValue(CableBlock.CABLE)
            }
        }
    }

    /**
     * Check chest peripherals are reattached with a new size.
     */
    @GameTest
    fun Chest_resizes_on_change(context: GameTestHelper) = context.sequence {
        thenOnComputer {
            callRemotePeripheral("minecraft:chest_0", "size").assertArrayEquals(27)
        }
        thenExecute { context.placeItemAt(ItemStack(Items.CHEST), BlockPos(2, 2, 2), Direction.WEST) }
        thenIdle(1)
        thenOnComputer {
            callRemotePeripheral("minecraft:chest_0", "size").assertArrayEquals(54)
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

        val hasType = peripheral.hasType(side, "peripheral_hub")
        if (hasType == null || hasType[0] != true) continue

        val names = peripheral.call(context, ObjectArguments(side, "getNamesRemote")).await() ?: continue
        @Suppress("UNCHECKED_CAST")
        peripherals.addAll(names[0] as Collection<String>)
    }

    peripherals.sort()
    return peripherals
}

private suspend fun LuaTaskContext.callRemotePeripheral(name: String, method: String, vararg args: Any): Array<out Any?>? {
    val peripheral = getApi<PeripheralAPI>()
    if (peripheral.isPresent(name)) return peripheral.call(context, ObjectArguments(name, method, *args)).await()

    for (side in ComputerSide.NAMES) {
        if (!peripheral.isPresent(side)) continue

        val hasType = peripheral.hasType(side, "peripheral_hub")
        if (hasType == null || hasType[0] != true) continue

        val isPresent = peripheral.call(context, ObjectArguments(side, "isPresentRemote", name)).await() ?: continue
        if (isPresent[0] as Boolean) {
            return peripheral.call(context, ObjectArguments(side, "callRemote", name, method, *args)).await()
        }
    }

    throw IllegalArgumentException("No such peripheral $name")
}

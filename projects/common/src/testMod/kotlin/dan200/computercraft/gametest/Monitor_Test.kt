// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.*
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.peripheral.monitor.MonitorBlock
import dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState
import net.minecraft.commands.arguments.blocks.BlockInput
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestGenerator
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.TestFunction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import org.junit.jupiter.api.Assertions.*

class Monitor_Test {
    @GameTest
    fun Ensures_valid_on_place(context: GameTestHelper) = context.sequence {
        val pos = BlockPos(2, 1, 2)

        thenExecute {
            val tag = CompoundTag()
            tag.putInt("Width", 2)
            tag.putInt("Height", 2)

            val toSet = BlockInput(
                ModRegistry.Blocks.MONITOR_ADVANCED.get().defaultBlockState(),
                emptySet(),
                tag,
            )

            context.setBlock(pos, Blocks.AIR.defaultBlockState())
            context.setBlock(pos, toSet)
        }
        thenIdle(2)
        thenExecute {
            val tile = context.getBlockEntity(pos, ModRegistry.BlockEntities.MONITOR_ADVANCED.get())

            if (tile.width != 1 || tile.height != 1) {
                context.fail("Tile has width and height of ${tile.width}x${tile.height}, but should be 1x1", pos)
            }
        }
    }

    /**
     * When a monitor is destroyed, assert its neighbors correctly contract.
     */
    @GameTest
    fun Contract_on_destroy(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            helper.setBlock(BlockPos(2, 1, 2), Blocks.AIR.defaultBlockState())
            helper.assertBlockHas(BlockPos(1, 1, 2), MonitorBlock.STATE, MonitorEdgeState.NONE)
            helper.assertBlockHas(BlockPos(3, 1, 2), MonitorBlock.STATE, MonitorEdgeState.NONE)
        }
    }

    /**
     * When a monitor is destroyed and then replaced, the terminal is recreated.
     */
    @GameTest
    fun Creates_terminal(helper: GameTestHelper) = helper.sequence {
        fun monitorAt(x: Int) =
            helper.getBlockEntity(BlockPos(x, 1, 2), ModRegistry.BlockEntities.MONITOR_ADVANCED.get())

        thenExecute {
            for (i in 1..3) {
                assertNull(monitorAt(i).cachedServerMonitor, "Monitor $i starts with no ServerMonitor")
            }

            monitorAt(2).peripheral()
            assertNotNull(monitorAt(1).cachedServerMonitor?.terminal, "Creating a peripheral creates a terminal")

            // Then remove the middle monitor and check it splits into two.
            helper.setBlock(BlockPos(2, 1, 2), Blocks.AIR.defaultBlockState())

            assertNotNull(monitorAt(3).cachedServerMonitor, "Origin retains its monitor")
            assertNull(monitorAt(3).cachedServerMonitor!!.terminal, "Origin deletes the terminal")
            assertNotEquals(monitorAt(1).cachedServerMonitor, monitorAt(3).cachedServerMonitor, "Monitors are different")

            // Then set the monitor, check it rejoins and recreates the terminal.
            val pos = BlockPos(2, 1, 2)
            helper.setBlock(pos, ModRegistry.Blocks.MONITOR_ADVANCED.get())
            ModRegistry.Blocks.MONITOR_ADVANCED.get().setPlacedBy(
                helper.level,
                helper.absolutePos(pos),
                helper.getBlockState(pos),
                helper.makeMockPlayer(GameType.SURVIVAL),
                ItemStack.EMPTY,
            )
            monitorAt(2).peripheral()

            assertNotNull(monitorAt(1).cachedServerMonitor?.terminal, "Recreates the terminal")
        }
    }

    /**
     * Test monitors render correctly
     */
    @GameTestGenerator
    fun Render_monitor_tests(): List<TestFunction> {
        val tests = mutableListOf<TestFunction>()

        fun addTest(label: String, time: Long = Times.NOON, tag: String = TestTags.CLIENT) {
            if (!TestTags.isEnabled(tag)) return

            val className = this::class.java.simpleName.lowercase()
            val testName = "$className.render_monitor"

            tests.add(
                TestFunction(
                    "$testName.$label",
                    "$testName.$label",
                    testName,
                    Timeouts.DEFAULT,
                    0,
                    true,
                ) { renderMonitor(it, time) },
            )
        }

        addTest("noon", Times.NOON)
        addTest("midnight", Times.MIDNIGHT)

        addTest("sodium_tbo", tag = "sodium")
        addTest("sodium_vbo", tag = "sodium")

        addTest("iris_noon", Times.NOON, tag = "iris")
        addTest("iris_midnight", Times.MIDNIGHT, tag = "iris")

        return tests
    }

    private fun renderMonitor(helper: GameTestHelper, time: Long) = helper.sequence {
        thenExecute {
            helper.level.dayTime = time
            helper.positionAtArmorStand()

            // Get the monitor and peripheral. This forces us to create a server monitor at this location.
            val monitor = helper.getBlockEntity(BlockPos(2, 1, 3), ModRegistry.BlockEntities.MONITOR_ADVANCED.get())
            monitor.peripheral()

            val terminal = monitor.cachedServerMonitor!!.terminal!!
            terminal.write("Hello, world!")
            terminal.setCursorPos(1, 2)
            terminal.textColour = 2
            terminal.backgroundColour = 3
            terminal.write("Some coloured text")
        }

        thenScreenshot()

        thenExecute { helper.level.dayTime = Times.NOON }
    }
}

/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.*
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.config.Config
import dan200.computercraft.shared.peripheral.monitor.MonitorBlock
import dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer
import net.minecraft.commands.arguments.blocks.BlockInput
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestGenerator
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.TestFunction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.Blocks
import java.util.*

class Monitor_Test {
    @GameTest
    fun Ensures_valid_on_place(context: GameTestHelper) = context.sequence {
        val pos = BlockPos(2, 2, 2)

        thenExecute {
            val tag = CompoundTag()
            tag.putInt("Width", 2)
            tag.putInt("Height", 2)

            val toSet = BlockInput(
                ModRegistry.Blocks.MONITOR_ADVANCED.get().defaultBlockState(),
                Collections.emptySet(),
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
            helper.setBlock(BlockPos(2, 2, 2), Blocks.AIR.defaultBlockState())
            helper.assertBlockHas(BlockPos(1, 2, 2), MonitorBlock.STATE, MonitorEdgeState.NONE)
            helper.assertBlockHas(BlockPos(3, 2, 2), MonitorBlock.STATE, MonitorEdgeState.NONE)
        }
    }

    /**
     * Test monitors render correctly
     */
    @GameTestGenerator
    fun Render_monitor_tests(): List<TestFunction> {
        val tests = mutableListOf<TestFunction>()

        fun addTest(label: String, renderer: MonitorRenderer, time: Long = Times.NOON, tag: String = TestTags.CLIENT) {
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
                ) { renderMonitor(it, renderer, time) },
            )
        }

        addTest("tbo_noon", MonitorRenderer.TBO, Times.NOON)
        addTest("tbo_midnight", MonitorRenderer.TBO, Times.MIDNIGHT)
        addTest("vbo_noon", MonitorRenderer.VBO, Times.NOON)
        addTest("vbo_midnight", MonitorRenderer.VBO, Times.MIDNIGHT)

        addTest("sodium_tbo", MonitorRenderer.TBO, tag = "sodium")
        addTest("sodium_vbo", MonitorRenderer.VBO, tag = "sodium")

        addTest("iris_noon", MonitorRenderer.BEST, Times.NOON, tag = "iris")
        addTest("iris_midnight", MonitorRenderer.BEST, Times.MIDNIGHT, tag = "iris")

        return tests
    }

    private fun renderMonitor(helper: GameTestHelper, renderer: MonitorRenderer, time: Long) = helper.sequence {
        thenExecute {
            Config.monitorRenderer = renderer
            helper.level.dayTime = time
            helper.positionAtArmorStand()

            // Get the monitor and peripheral. This forces us to create a server monitor at this location.
            val monitor = helper.getBlockEntity(BlockPos(2, 2, 3), ModRegistry.BlockEntities.MONITOR_ADVANCED.get())
            monitor.peripheral()

            val terminal = monitor.cachedServerMonitor!!.terminal!!
            terminal.write("Hello, world!")
            terminal.setCursorPos(1, 2)
            terminal.textColour = 2
            terminal.backgroundColour = 3
            terminal.write("Some coloured text")
        }

        thenScreenshot()
    }
}

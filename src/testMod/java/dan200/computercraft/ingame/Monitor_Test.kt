package dan200.computercraft.ingame

import dan200.computercraft.ComputerCraft
import dan200.computercraft.ingame.api.*
import dan200.computercraft.ingame.api.Timeouts.CLIENT_TIMEOUT
import dan200.computercraft.shared.Capabilities
import dan200.computercraft.shared.Registry
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer
import dan200.computercraft.shared.peripheral.monitor.TileMonitor
import net.minecraft.block.Blocks
import net.minecraft.command.arguments.BlockStateInput
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.math.BlockPos
import java.util.*

class Monitor_Test {
    @GameTest
    fun Ensures_valid_on_place(context: GameTestHelper) = context.sequence {
        val pos = BlockPos(2, 2, 2)
        val tag = CompoundNBT()
        tag.putInt("Width", 2)
        tag.putInt("Height", 2)

        val toSet = BlockStateInput(
            Registry.ModBlocks.MONITOR_ADVANCED.get().defaultBlockState(),
            Collections.emptySet(),
            tag,
        )

        context.setBlock(pos, Blocks.AIR.defaultBlockState())
        context.setBlock(pos, toSet)

        this
            .thenIdle(2)
            .thenExecute {
                val tile = context.getBlockEntity(pos)
                if (tile !is TileMonitor) {
                    context.fail("Expected tile to be monitor, is $tile", pos)
                    return@thenExecute
                }

                if (tile.width != 1 || tile.height != 1) {
                    context.fail("Tile has width and height of ${tile.width}x${tile.height}, but should be 1x1", pos)
                }
            }
    }

    private fun looksAcceptable(helper: GameTestHelper, renderer: MonitorRenderer) = helper.sequence {
        this
            .thenExecute {
                ComputerCraft.monitorRenderer = renderer
                helper.positionAtArmorStand()

                // Get the monitor and peripheral. This forces us to create a server monitor at this location.
                val monitor = helper.getBlockEntity(BlockPos(2, 2, 3)) as TileMonitor
                monitor.getCapability(Capabilities.CAPABILITY_PERIPHERAL)

                val terminal = monitor.cachedServerMonitor!!.terminal!!
                terminal.write("Hello, world!")
                terminal.setCursorPos(1, 2)
                terminal.textColour = 2
                terminal.backgroundColour = 3
                terminal.write("Some coloured text")
            }
            .thenScreenshot()
    }

    @GameTest(batch = "client:Monitor_Test.Looks_acceptable", timeoutTicks = CLIENT_TIMEOUT, template = LOOKS_ACCEPTABLE)
    fun Looks_acceptable(helper: GameTestHelper) = looksAcceptable(helper, renderer = MonitorRenderer.TBO)

    @GameTest(batch = "client:Monitor_Test.Looks_acceptable_dark", timeoutTicks = CLIENT_TIMEOUT, template = LOOKS_ACCEPTABLE_DARK)
    fun Looks_acceptable_dark(helper: GameTestHelper) = looksAcceptable(helper, renderer = MonitorRenderer.TBO)

    @GameTest(batch = "client:Monitor_Test.Looks_acceptable_vbo", timeoutTicks = CLIENT_TIMEOUT, template = LOOKS_ACCEPTABLE)
    fun Looks_acceptable_vbo(helper: GameTestHelper) = looksAcceptable(helper, renderer = MonitorRenderer.VBO)

    @GameTest(batch = "client:Monitor_Test.Looks_acceptable_dark_vbo", timeoutTicks = CLIENT_TIMEOUT, template = LOOKS_ACCEPTABLE_DARK)
    fun Looks_acceptable_dark_vbo(helper: GameTestHelper) = looksAcceptable(helper, renderer = MonitorRenderer.VBO)

    private companion object {
        const val LOOKS_ACCEPTABLE = "looks_acceptable"
        const val LOOKS_ACCEPTABLE_DARK = "looks_acceptable_dark"
    }
}

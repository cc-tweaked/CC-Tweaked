package dan200.computercraft.ingame

import dan200.computercraft.ComputerCraft
import dan200.computercraft.ingame.api.*
import dan200.computercraft.shared.Capabilities
import dan200.computercraft.shared.Registry
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer
import dan200.computercraft.shared.peripheral.monitor.TileMonitor
import net.minecraft.commands.arguments.blocks.BlockInput
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.gametest.GameTestHolder
import java.util.*

@GameTestHolder(ComputerCraft.MOD_ID)
class Monitor_Test {
    @GameTest
    fun Ensures_valid_on_place(context: GameTestHelper) = context.sequence {
        val pos = BlockPos(2, 2, 2)
        val tag = CompoundTag()
        tag.putInt("Width", 2)
        tag.putInt("Height", 2)

        val toSet = BlockInput(
            Registry.ModBlocks.MONITOR_ADVANCED.get().defaultBlockState(),
            Collections.emptySet(),
            tag
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
                val monitor = helper.getBlockEntity(BlockPos(2, 2, 3), Registry.ModBlockEntities.MONITOR_ADVANCED.get())
                monitor.getCapability(Capabilities.CAPABILITY_PERIPHERAL)

                val terminal = monitor.cachedServerMonitor!!.terminal
                terminal.write("Hello, world!")
                terminal.setCursorPos(1, 2)
                terminal.textColour = 2
                terminal.backgroundColour = 3
                terminal.write("Some coloured text")
            }
            .thenScreenshot()
    }

    // @GameTest(batch = "Monitor_Test.Looks_acceptable", template = LOOKS_ACCEPTABLE)
    fun Looks_acceptable(helper: GameTestHelper) = looksAcceptable(helper, renderer = MonitorRenderer.TBO)

    // @GameTest(batch = "Monitor_Test.Looks_acceptable_dark", template = LOOKS_ACCEPTABLE_DARK)
    fun Looks_acceptable_dark(helper: GameTestHelper) = looksAcceptable(helper, renderer = MonitorRenderer.TBO)

    // @GameTest(batch = "Monitor_Test.Looks_acceptable_vbo", template = LOOKS_ACCEPTABLE)
    fun Looks_acceptable_vbo(helper: GameTestHelper) = looksAcceptable(helper, renderer = MonitorRenderer.VBO)

    // @GameTest(batch = "Monitor_Test.Looks_acceptable_dark_vbo", template = LOOKS_ACCEPTABLE_DARK)
    fun Looks_acceptable_dark_vbo(helper: GameTestHelper) = looksAcceptable(helper, renderer = MonitorRenderer.VBO)

    private companion object {
        const val LOOKS_ACCEPTABLE = "looks_acceptable"
        const val LOOKS_ACCEPTABLE_DARK = "looks_acceptable_dark"
    }
}

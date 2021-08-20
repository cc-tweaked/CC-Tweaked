package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.*
import dan200.computercraft.shared.Registry
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
}

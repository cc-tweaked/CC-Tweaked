package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.*
import dan200.computercraft.shared.Registry
import dan200.computercraft.shared.peripheral.monitor.TileMonitor
import net.minecraft.block.Blocks
import net.minecraft.command.arguments.BlockStateInput
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.math.BlockPos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.fail
import java.util.*

class MonitorTest {
    @GameTest
    suspend fun `Ensures valid on place`(context: TestContext) {
        val pos = BlockPos(2, 0, 2)
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

        context.sleep(2)

        val tile = context.getTile(pos)
        if (tile !is TileMonitor) fail("Expected tile to be monitor, is $tile")

        assertEquals(1, tile.width, "Width should be 1")
        assertEquals(1, tile.height, "Width should be 1")
    }
}

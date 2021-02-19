package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.*
import net.minecraft.block.LeverBlock
import net.minecraft.block.RedstoneLampBlock
import net.minecraft.util.math.BlockPos
import org.junit.jupiter.api.Assertions.assertFalse

class ComputerTest {
    /**
     * Ensures redstone signals do not travel through computers.
     *
     * @see [#548](https://github.com/SquidDev-CC/CC-Tweaked/issues/548)
     */
    @GameTest
    suspend fun `No through signal`(context: TestContext) {
        val lamp = BlockPos(2, 0, 4)
        val lever = BlockPos(2, 0, 0)

        assertFalse(context.getBlock(lamp).getValue(RedstoneLampBlock.LIT), "Lamp should not be lit")

        context.modifyBlock(lever) { x -> x.setValue(LeverBlock.POWERED, true) }
        context.sleep(3)

        assertFalse(context.getBlock(lamp).getValue(RedstoneLampBlock.LIT), "Lamp should still not be lit")
    }
}

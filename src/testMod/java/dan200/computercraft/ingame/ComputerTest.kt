package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.*
import net.minecraft.block.LeverBlock
import net.minecraft.block.RedstoneLampBlock
import net.minecraft.util.math.BlockPos

class Computer_Test {
    /**
     * Ensures redstone signals do not travel through computers.
     *
     * @see [#548](https://github.com/SquidDev-CC/CC-Tweaked/issues/548)
     */
    @GameTest
    fun No_through_signal(context: GameTestHelper) = context.sequence {
        val lamp = BlockPos(2, 2, 4)
        val lever = BlockPos(2, 2, 0)
        this
            .thenExecute {
                context.assertBlockState(lamp, { !it.getValue(RedstoneLampBlock.LIT) }, { "Lamp should not be lit" })
                context.modifyBlock(lever) { x -> x.setValue(LeverBlock.POWERED, true) }
            }
            .thenIdle(3)
            .thenExecute {
                context.assertBlockState(
                    lamp,
                    { !it.getValue(RedstoneLampBlock.LIT) },
                    { "Lamp should still not be lit" })
            }
    }
}

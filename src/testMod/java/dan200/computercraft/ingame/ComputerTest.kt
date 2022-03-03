package dan200.computercraft.ingame

import dan200.computercraft.ComputerCraft
import dan200.computercraft.ingame.api.modifyBlock
import dan200.computercraft.ingame.api.sequence
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.LeverBlock
import net.minecraft.world.level.block.RedstoneLampBlock
import net.minecraftforge.gametest.GameTestHolder

@GameTestHolder(ComputerCraft.MOD_ID)
class Computer_Test {
    /**
     * Ensures redstone signals do not travel through computers.
     *
     * @see [#548](https://github.com/cc-tweaked/CC-Tweaked/issues/548)
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

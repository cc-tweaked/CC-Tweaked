package dan200.computercraft.ingame.api

import dan200.computercraft.ComputerCraft
import net.minecraft.block.BlockState
import net.minecraft.command.arguments.BlockStateInput
import net.minecraft.util.math.BlockPos

/**
 * Wait until a computer has finished running and check it is OK.
 */
fun GameTestSequence.thenComputerOk(id: Int, marker: String = ComputerState.DONE): GameTestSequence =
    thenWaitUntil {
        val computer = ComputerState.get(id)
        if (computer == null || !computer.isDone(marker)) throw GameTestAssertException("Computer #${id} has not finished yet.")
    }.thenExecute {
        ComputerState.get(id).check(marker)
    }

/**
 * Modify a block state within the test.
 */
fun GameTestHelper.modifyBlock(pos: BlockPos, modify: (BlockState) -> BlockState) {
    setBlock(pos, modify(getBlockState(pos)))
}

fun GameTestHelper.sequence(run: GameTestSequence.() -> GameTestSequence) {
    run(startSequence()).thenSucceed()
}

/**
 * Set a block within the test structure.
 */
fun GameTestHelper.setBlock(pos: BlockPos, state: BlockStateInput) = state.place(level, absolutePos(pos), 3)


fun GameTestSequence.thenExecuteSafe(runnable: Runnable) {
    try {
        runnable.run()
    } catch (e: GameTestAssertException) {
        throw e
    } catch (e: RuntimeException) {
        ComputerCraft.log.error("Error in test", e)
        throw GameTestAssertException(e.toString())
    }
}

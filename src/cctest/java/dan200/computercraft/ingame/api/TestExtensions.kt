package dan200.computercraft.ingame.api

import net.minecraft.commands.arguments.blocks.BlockInput
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.GameTestSequence
import net.minecraft.world.level.block.state.BlockState

/**
 * Wait until a computer has finished running and check it is OK.
 */
fun GameTestSequence.thenComputerOk(id: Int): GameTestSequence =
    thenWaitUntil {
        val computer = ComputerState.get(id)
        if (computer == null || !computer.isDone) throw GameTestAssertException("Computer #${id} has not finished yet.")
    }.thenExecute {
        ComputerState.get(id).check()
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
fun GameTestHelper.setBlock(pos: BlockPos, state: BlockInput) = state.place(level, absolutePos(pos), 3)

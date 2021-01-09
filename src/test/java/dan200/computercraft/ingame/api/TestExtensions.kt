package dan200.computercraft.ingame.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos

/**
 * Wait until a predicate matches (or the test times out).
 */
suspend inline fun TestContext.waitUntil(fn: () -> Boolean) {
    while (true) {
        if (isDone) throw CancellationException()
        if (fn()) return

        delay(50)
    }
}

/**
 * Wait until a computer has finished running and check it is OK.
 */
suspend fun TestContext.checkComputerOk(id: Int) {
    waitUntil {
        val computer = ComputerState.get(id)
        computer != null && computer.isDone
    }

    ComputerState.get(id).check()
}

/**
 * Sleep for a given number of ticks.
 */
suspend fun TestContext.sleep(ticks: Int = 1) {
    val target = tracker.level.gameTime + ticks
    waitUntil { tracker.level.gameTime >= target }
}

private fun TestContext.offset(pos: BlockPos): BlockPos = tracker.testPos.offset(pos.x, pos.y + 2, pos.z)

/**
 * Get a block within the test structure.
 */
fun TestContext.getBlock(pos: BlockPos): BlockState = tracker.level.getBlockState(offset(pos))

/**
 * Set a block within the test structure.
 */
fun TestContext.setBlock(pos: BlockPos, state: BlockState) {
    tracker.level.setBlockAndUpdate(offset(pos), state)
}

/**
 * Modify a block state within the test.
 */
fun TestContext.modifyBlock(pos: BlockPos, modify: (BlockState) -> BlockState) {
    val level = tracker.level
    val offset = offset(pos)
    level.setBlockAndUpdate(offset, modify(level.getBlockState(offset)))
}

package dan200.computercraft.ingame.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import net.minecraft.block.BlockState
import net.minecraft.command.arguments.BlockStateInput
import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.AxisAlignedBB
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

fun TestContext.offset(pos: BlockPos): BlockPos = tracker.testPos.offset(pos.x, pos.y + 2, pos.z)

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
 * Set a block within the test structure.
 */
fun TestContext.setBlock(pos: BlockPos, state: BlockStateInput) = state.place(tracker.level, offset(pos), 3)

/**
 * Modify a block state within the test.
 */
fun TestContext.modifyBlock(pos: BlockPos, modify: (BlockState) -> BlockState) {
    val level = tracker.level
    val offset = offset(pos)
    level.setBlockAndUpdate(offset, modify(level.getBlockState(offset)))
}

/**
 * Get a tile within the test structure.
 */
fun TestContext.getTile(pos: BlockPos): TileEntity? = tracker.level.getBlockEntity(offset(pos))

/**
 * Get an entity within the test structure.
 */
fun TestContext.getEntity(pos: BlockPos): Entity? {
    val entities = tracker.level.getEntitiesOfClass(Entity::class.java, AxisAlignedBB(offset(pos)))
    return if (entities.isEmpty()) null else entities.get(0)
}

/**
 * Get an entity within the test structure.
 */
inline fun <reified T : Entity> TestContext.getEntityOfType(pos: BlockPos): T? {
    val entities = tracker.level.getEntitiesOfClass(T::class.java, AxisAlignedBB(offset(pos)))
    return if (entities.isEmpty()) null else entities.get(0)
}

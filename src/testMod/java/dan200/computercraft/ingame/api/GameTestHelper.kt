package dan200.computercraft.ingame.api

import dan200.computercraft.ingame.mod.TestMod
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityType
import net.minecraft.item.Item
import net.minecraft.test.*
import net.minecraft.tileentity.StructureBlockTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.fml.unsafe.UnsafeHacks
import java.lang.reflect.Field

/**
 * Backport of 1.17's GameTestHelper class.
 *
 * Much of the underlying functionality is present in MC, it's just the methods to use it have been stripped out!
 */
class GameTestHelper(holder: TestTrackerHolder) {
    val tracker: TestTracker = holder.testInfo

    val level: ServerWorld = tracker.level

    fun startSequence(): GameTestSequence {
        val sequence = UnsafeHacks.newInstance(GameTestSequence::class.java) as GameTestSequence
        sequence.parent = tracker
        sequence.lastTick = tracker.tickCount
        sequence.events = mutableListOf()
        tracker.sequences.add(sequence)
        return sequence
    }

    fun absolutePos(pos: BlockPos): BlockPos = tracker.structureBlockPos.offset(pos)

    fun getBlockState(pos: BlockPos): BlockState = tracker.level.getBlockState(absolutePos(pos))

    fun setBlock(pos: BlockPos, block: BlockState) = tracker.level.setBlock(absolutePos(pos), block, 3)

    fun getBlockEntity(pos: BlockPos): TileEntity? = tracker.level.getBlockEntity(absolutePos(pos))

    fun assertBlockState(pos: BlockPos, predicate: (BlockState) -> Boolean, message: () -> String) {
        val block = getBlockState(pos)
        if (!predicate(block)) throw GameTestAssertException("Invalid block $block at $pos: ${message()}")
    }

    fun assertItemEntityPresent(item: Item, pos: BlockPos, range: Double) {
        val absPos = absolutePos(pos)
        for (entity in level.getEntities(EntityType.ITEM, AxisAlignedBB(absPos).inflate(range)) { it.isAlive }) {
            if (entity.item.item == item) return
        }

        throw GameTestAssertException("Expected $item at $pos")
    }

    fun fail(e: Throwable) {
        tracker.fail(e)
    }

    fun fail(message: String) {
        tracker.fail(GameTestAssertException(message))
    }

    fun fail(message: String, pos: BlockPos) {
        tracker.fail(GameTestAssertException("$message at $pos"))
    }

    val bounds: AxisAlignedBB
        get() {
            val structure = tracker.level.getBlockEntity(tracker.structureBlockPos)
            if (structure !is StructureBlockTileEntity) throw IllegalStateException("Cannot find structure block")
            return StructureHelper.getStructureBounds(structure)
        }
}

typealias GameTestSequence = TestList

typealias GameTestAssertException = RuntimeException

private fun GameTestSequence.addResult(task: () -> Unit, delay: Long? = null): GameTestSequence {
    val result = UnsafeHacks.newInstance(TestTickResult::class.java) as TestTickResult
    result.assertion = Runnable(task)
    result.expectedDelay = delay
    events.add(result)
    return this
}

fun GameTestSequence.thenSucceed() {
    addResult({
        if (parent.error != null) return@addResult

        parent.finish()
        parent.markAsComplete()
    })
}

fun GameTestSequence.thenWaitUntil(task: () -> Unit) = addResult(task)

fun GameTestSequence.thenExecute(task: () -> Unit) = addResult({
    try {
        task()
    } catch (e: Exception) {
        parent.fail(e)
    }
})

fun GameTestSequence.thenIdle(delay: Long) = addResult({
    if (parent.tickCount < lastTick + delay) {
        throw GameTestAssertException("Waiting")
    }
})

/**
 * Proguard strips out all the "on success" code as it's never called anywhere. This is workable most of the time, but
 * means we can't support multiple test batches as the [TestExecutor] never thinks the first batch has finished.
 *
 * This function does two nasty things:
 *  - Update the beacon when the test passes.
 *  - Find the current test executor by searching through our listener list and call its [TestExecutor.testCompleted]
 *    method.
 */
private fun TestTracker.markAsComplete() {
    try {
        TestUtils.spawnBeacon(this, Blocks.LIME_STAINED_GLASS)

        val listeners: Collection<ITestCallback> = TestTracker::class.java.getDeclaredField("listeners").unsafeGet(this)
        for (listener in listeners) {
            if (listener.javaClass.name != "net.minecraft.test.TestExecutor$1") continue

            for (field in listener.javaClass.declaredFields) {
                if (field.type == TestExecutor::class.java) {
                    field.unsafeGet<TestExecutor>(listener).testCompleted(this)
                }
            }
            break
        }
    } catch (e: Exception) {
        TestMod.log.error("Failed to mark as complete", e)
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> Field.unsafeGet(owner: Any): T {
    isAccessible = true
    return get(owner) as T
}

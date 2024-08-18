// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.api

import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.gametest.core.ManagedComputers
import dan200.computercraft.mixin.gametest.GameTestHelperAccessor
import dan200.computercraft.mixin.gametest.GameTestInfoAccessor
import dan200.computercraft.mixin.gametest.GameTestSequenceAccessor
import dan200.computercraft.shared.platform.PlatformHelper
import dan200.computercraft.shared.platform.RegistryWrappers
import dan200.computercraft.test.core.computer.LuaTaskContext
import dan200.computercraft.test.shared.ItemStackMatcher.isStack
import net.minecraft.commands.arguments.blocks.BlockInput
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.hamcrest.Matchers
import org.hamcrest.StringDescription

/**
 * Globally usable structures.
 *
 * @see GameTest.template
 */
object Structures {
    /** The "default" structure, a 5x5 area with a polished Andesite floor */
    const val DEFAULT = "default"
}

/** Pre-set in-game times */
object Times {
    const val NOON: Long = 6000

    const val MIDNIGHT: Long = 18000
}

/**
 * Custom timeouts for various test types.
 *
 * @see GameTest.timeoutTicks
 */
object Timeouts {
    const val SECOND: Int = 20

    const val DEFAULT: Int = SECOND * 5

    const val COMPUTER_TIMEOUT: Int = SECOND * 15
}

/**
 * Equivalent to [GameTestSequence.thenExecute], but which won't run the next steps if the parent fails.
 */
fun GameTestSequence.thenExecuteFailFast(task: Runnable): GameTestSequence =
    thenExecute(task).thenWaitUntil {
        val failure = (this as GameTestSequenceAccessor).parent.error
        if (failure != null) throw failure
    }

/**
 * Wait until a computer has finished running and check it is OK.
 */
fun GameTestSequence.thenComputerOk(name: String? = null, marker: String = ComputerState.DONE): GameTestSequence {
    val label = (this as GameTestSequenceAccessor).parent.testName + (if (name == null) "" else ".$name")

    thenWaitUntil {
        val computer = ComputerState.get(label)
        if (computer == null || !computer.isDone(marker)) throw GameTestAssertException("Computer '$label' has not reached $marker yet.")
    }
    thenExecuteFailFast { ComputerState.get(label)!!.check(marker) }
    return this
}

/**
 * Run a task on a computer but don't wait for it to finish.
 */
fun GameTestSequence.thenStartComputer(name: String? = null, action: suspend LuaTaskContext.() -> Unit): GameTestSequence {
    val test = (this as GameTestSequenceAccessor).parent
    val label = test.testName + (if (name == null) "" else ".$name")
    return thenExecuteFailFast { ManagedComputers.enqueue(test, label, action) }
}

/**
 * Run a task on a computer and wait for it to finish.
 */
fun GameTestSequence.thenOnComputer(name: String? = null, action: suspend LuaTaskContext.() -> Unit): GameTestSequence {
    val self = (this as GameTestSequenceAccessor)
    val test = self.parent

    val label = test.testName + (if (name == null) "" else ".$name")
    var monitor: ManagedComputers.Monitor? = null
    thenExecuteFailFast { monitor = ManagedComputers.enqueue(test, label, action) }
    thenWaitUntil {
        if (!monitor!!.isFinished) {
            val runningFor = (test as GameTestInfoAccessor).`computercraft$getTick`() - self.lastTick
            throw GameTestAssertException("Computer '$label' has not finished yet (running for $runningFor ticks).")
        }
    }
    thenExecuteFailFast { monitor!!.check() }
    return this
}

/**
 * Create a new game test sequence
 */
fun GameTestHelper.sequence(run: GameTestSequence.() -> Unit) {
    val sequence = startSequence()
    run(sequence)
    sequence.thenSucceed()
}

/**
 * A custom instance of [GameTestAssertPosException] which allows for longer error messages.
 */
private class VerboseGameTestAssertPosException(message: String, absolutePos: BlockPos, relativePos: BlockPos, tick: Long) :
    GameTestAssertPosException(message, absolutePos, relativePos, tick) {
    override fun getMessageToShowAtBlock(): String = message!!.lineSequence().first()
}

/**
 * Fail this test. Unlike [GameTestHelper.fail], this trims the in-game error message to the first line.
 */
private fun GameTestHelper.failVerbose(message: String, pos: BlockPos): Nothing {
    throw VerboseGameTestAssertPosException(message, absolutePos(pos), pos, tick)
}

/** Fail with an optional context message. */
private fun GameTestHelper.fail(message: String?, detail: String, pos: BlockPos): Nothing {
    failVerbose(if (message.isNullOrEmpty()) detail else "$message: $detail", pos)
}

/**
 * A version of [GameTestHelper.assertBlockState] which also includes the current block state.
 */
fun GameTestHelper.assertBlockIs(pos: BlockPos, predicate: (BlockState) -> Boolean) = assertBlockIs(pos, predicate, "")

/**
 * A version of [GameTestHelper.assertBlockState] which also includes the current block state.
 */
fun GameTestHelper.assertBlockIs(pos: BlockPos, predicate: (BlockState) -> Boolean, message: String) {
    val state = getBlockState(pos)
    if (!predicate(state)) fail(message, state.toString(), pos)
}

/**
 * A version of [GameTestHelper.assertBlockProperty] which includes the current block state in the error message.
 */
fun <T : Comparable<T>> GameTestHelper.assertBlockHas(pos: BlockPos, property: Property<T>, value: T, message: String = "") {
    val state = getBlockState(pos)
    if (!state.hasProperty(property)) {
        val id = RegistryWrappers.BLOCKS.getKey(state.block)
        fail(message, "block $id does not have property ${property.name}", pos)
    } else if (state.getValue(property) != value) {
        fail(message, "${property.name} is ${state.getValue(property)}, expected $value", pos)
    }
}

/**
 * Get a [Container] at a given position.
 */
fun GameTestHelper.getContainerAt(pos: BlockPos): Container =
    when (val container = getBlockEntity(pos)) {
        is Container -> container
        null -> failVerbose("Expected a container at $pos, found nothing", pos)
        else -> failVerbose("Expected a container at $pos, found ${getName(container.type)}", pos)
    }

/**
 * Assert a container contains exactly these items and no more.
 *
 * @param pos The position of the container.
 * @param items The list of items this container must contain. This should be equal to the expected contents of the
 * first `n` slots - the remaining are required to be empty.
 */
fun GameTestHelper.assertContainerExactly(pos: BlockPos, items: List<ItemStack>) =
    assertContainerExactlyImpl(pos, getContainerAt(pos), items)

/**
 * Assert an container contains exactly these items and no more.
 *
 * @param entity The entity containing these items.
 * @param items The list of items this container must contain. This should be equal to the expected contents of the
 * first `n` slots - the remaining are required to be empty.
 */
fun <T> GameTestHelper.assertContainerExactly(entity: T, items: List<ItemStack>) where T : Entity, T : Container =
    assertContainerExactlyImpl(entity.blockPosition(), entity, items)

private fun GameTestHelper.assertContainerExactlyImpl(pos: BlockPos, container: Container, items: List<ItemStack>) {
    val slot = (0 until container.containerSize).indexOfFirst { slot ->
        val expected = if (slot >= items.size) ItemStack.EMPTY else items[slot]
        !ItemStack.matches(container.getItem(slot), expected)
    }

    if (slot >= 0) {
        failVerbose(
            """
            Items do not match (first mismatch at slot $slot).
            Expected:  $items
            Container: ${(0 until container.containerSize).map { container.getItem(it) }.dropLastWhile { it.isEmpty }}
            """.trimIndent(),
            pos,
        )
    }
}

/**
 * A nasty hack to get a peripheral at a given position, by creating a dummy [BlockEntity].
 */
private fun GameTestHelper.getPeripheralAt(pos: BlockPos, direction: Direction): IPeripheral? {
    val be = BarrelBlockEntity(absolutePos(pos).relative(direction), Blocks.BARREL.defaultBlockState())
    be.setLevel(level)
    return PlatformHelper.get().createPeripheralAccess(be) { }.get(direction.opposite)
}

fun GameTestHelper.assertPeripheral(pos: BlockPos, direction: Direction = Direction.UP, type: String) {
    val peripheral = getPeripheralAt(pos, direction)
    when {
        peripheral == null -> fail("No peripheral at position", pos)
        peripheral.type != type -> fail("Peripheral is of type ${peripheral.type}, expected $type", pos)
    }
}

fun GameTestHelper.assertNoPeripheral(pos: BlockPos, direction: Direction = Direction.UP) {
    val peripheral = getPeripheralAt(pos, direction)
    if (peripheral != null) fail("Expected no peripheral, got a ${peripheral.type}", pos)
}

fun GameTestHelper.assertExactlyItems(vararg expected: ItemStack, message: String? = null) {
    val actual = getEntities(EntityType.ITEM).map { it.item }
    val matcher = Matchers.containsInAnyOrder(expected.map { isStack(it) })
    if (!matcher.matches(actual)) {
        val description = StringDescription()
        matcher.describeMismatch(actual, description)
        fail(if (message.isNullOrEmpty()) description.toString() else "$message: $description")
    }
}

/**
 * Similar to [GameTestHelper.assertItemEntityCountIs], but searching anywhere in the structure bounds.
 */
fun GameTestHelper.assertItemEntityCountIs(expected: Item, count: Int) {
    val actualCount = getEntities(EntityType.ITEM).sumOf { if (it.item.`is`(expected)) it.item.count else 0 }
    if (actualCount != count) {
        throw GameTestAssertException("Expected $count ${expected.description.string} items to exist (found $actualCount)")
    }
}

private fun getName(type: BlockEntityType<*>): ResourceLocation = RegistryWrappers.BLOCK_ENTITY_TYPES.getKey(type)!!

/**
 * Get a [BlockEntity] of a specific type.
 */
fun <T : BlockEntity> GameTestHelper.getBlockEntity(pos: BlockPos, type: BlockEntityType<T>): T {
    val tile = getBlockEntity(pos)
    @Suppress("UNCHECKED_CAST")
    return when {
        tile == null -> failVerbose("Expected ${getName(type)}, but no tile was there", pos)
        tile.type != type -> failVerbose("Expected ${getName(type)} but got ${getName(tile.type)}", pos)
        else -> tile as T
    }
}

/**
 * Get all entities of a specific type within the test structure.
 */
fun <T : Entity> GameTestHelper.getEntities(type: EntityType<T>): List<T> {
    val info = (this as GameTestHelperAccessor).testInfo
    return level.getEntities(type, info.structureBounds!!) { it.isAlive }
}

/**
 * Get an [Entity] inside the game structure, requiring there to be a single one.
 */
fun <T : Entity> GameTestHelper.getEntity(type: EntityType<T>): T {
    val entities = getEntities(type)
    when (entities.size) {
        0 -> throw GameTestAssertException("No $type entities")
        1 -> return entities[0]
        else -> throw GameTestAssertException("Multiple $type entities (${entities.size} in bounding box)")
    }
}

/**
 * Set a block within the test structure.
 */
fun GameTestHelper.setBlock(pos: BlockPos, state: BlockInput) = state.place(level, absolutePos(pos), 3)

/**
 * Modify a block state within the test.
 */
fun GameTestHelper.modifyBlock(pos: BlockPos, modify: (BlockState) -> BlockState) {
    setBlock(pos, modify(getBlockState(pos)))
}

/**
 * Update items in the container at [pos], setting the item in the specified [slot] to [item], and then marking it
 * changed.
 */
fun GameTestHelper.setContainerItem(pos: BlockPos, slot: Int, item: ItemStack) {
    val container = getContainerAt(pos)
    container.setItem(slot, item)
    container.setChanged()
}

/**
 * An alternative version ot [GameTestHelper.placeAt], which sets the player's held item first.
 *
 * This is required for compatibility with Forge, which uses the in-hand stack, rather than the stack requested.
 */
fun GameTestHelper.placeItemAt(stack: ItemStack, pos: BlockPos, direction: Direction) {
    val player = makeMockPlayer()
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)
    val absolutePos = absolutePos(pos.relative(direction))
    val hit = BlockHitResult(Vec3.atCenterOf(absolutePos), direction, absolutePos, false)
    stack.useOn(UseOnContext(player, InteractionHand.MAIN_HAND, hit))
}

/**
 * Run a function multiple times until it succeeds.
 */
inline fun tryMultipleTimes(count: Int, action: () -> Unit) {
    for (remaining in count - 1 downTo 0) {
        try {
            action()
        } catch (e: AssertionError) {
            if (remaining == 0) throw e
        }
    }
}

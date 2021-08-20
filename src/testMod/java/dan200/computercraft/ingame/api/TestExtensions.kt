package dan200.computercraft.ingame.api

import dan200.computercraft.ingame.mod.ImageUtils
import dan200.computercraft.ingame.mod.TestMod
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.client.Minecraft
import net.minecraft.command.arguments.BlockStateInput
import net.minecraft.entity.item.ArmorStandEntity
import net.minecraft.util.ScreenShotHelper
import net.minecraft.util.math.BlockPos
import net.minecraft.world.gen.Heightmap
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier
import javax.imageio.ImageIO


/**
 * Wait until a computer has finished running and check it is OK.
 */
fun GameTestSequence.thenComputerOk(name: String? = null, marker: String = ComputerState.DONE): GameTestSequence {
    val label = parent.testName + (if (name == null) "" else ".$name")
    return this.thenWaitUntil {
        val computer = ComputerState.get(label)
        if (computer == null || !computer.isDone(marker)) throw GameTestAssertException("Computer '$label' has not finished yet.")
    }.thenExecute {
        ComputerState.get(label).check(marker)
    }
}

/**
 * Run a task on the client
 */
fun GameTestSequence.thenOnClient(task: ClientTestHelper.() -> Unit): GameTestSequence {
    var future: CompletableFuture<Unit>? = null
    return this
        .thenExecute { future = Minecraft.getInstance().submit(Supplier { task(ClientTestHelper()) }) }
        .thenWaitUntil { if (!future!!.isDone) throw GameTestAssertException("Not done task yet") }
        .thenExecute {
            try {
                future!!.get()
            } catch (e: ExecutionException) {
                throw e.cause ?: e
            }
        }
}

/**
 * Idle for one tick to allow the client to catch up, then take a screenshot.
 */
fun GameTestSequence.thenScreenshot(name: String? = null): GameTestSequence {
    val suffix = if (name == null) "" else "-$name"
    val fullName = "${parent.testName}$suffix"

    val counter = AtomicInteger()
    return this
        // Wait until all chunks have been rendered and we're idle for an extended period.
        .thenExecute { counter.set(0) }
        .thenWaitUntil {
            if (Minecraft.getInstance().levelRenderer.hasRenderedAllChunks()) {
                val idleFor = counter.getAndIncrement()
                if (idleFor <= 20) throw GameTestAssertException("Only idle for $idleFor ticks")
            } else {
                counter.set(0)
                throw GameTestAssertException("Waiting for client to finish rendering")
            }
        }
        // Now disable the GUI, take a screenshot and reenable it. We sleep either side to give the client time to do
        // its thing.
        .thenExecute { Minecraft.getInstance().options.hideGui = true }
        .thenIdle(5) // Some delay before/after to ensure the render thread has caught up.
        .thenOnClient { screenshot("$fullName.png") }
        .thenIdle(2)
        .thenExecute {
            Minecraft.getInstance().options.hideGui = false

            val screenshotsPath = Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots")
            val screenshotPath = screenshotsPath.resolve("$fullName.png")
            val originalPath = TestMod.sourceDir.resolve("screenshots").resolve("$fullName.png")

            if (!Files.exists(originalPath)) throw GameTestAssertException("$fullName does not exist. Use `/cctest promote' to create it.");

            val screenshot = ImageIO.read(screenshotPath.toFile())
            val original = ImageIO.read(originalPath.toFile())

            if (screenshot.width != original.width || screenshot.height != original.height) {
                throw GameTestAssertException("$fullName screenshot is ${screenshot.width}x${screenshot.height} but original is ${original.width}x${original.height}")
            }

            if (ImageUtils.areSame(screenshot, original)) return@thenExecute

            ImageUtils.writeDifference(screenshotsPath.resolve("$fullName.diff.png"), screenshot, original)
            throw GameTestAssertException("Images are different.")
        }
}

val GameTestHelper.testName: String get() = tracker.testName

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

/**
 * "Normalise" the current world in preparation for screenshots.
 *
 * Basically removes any dirt and replaces it with concrete.
 */
fun GameTestHelper.normaliseScene() {
    val y = level.getHeightmapPos(Heightmap.Type.WORLD_SURFACE, absolutePos(BlockPos.ZERO))
    for (x in -100..100) {
        for (z in -100..100) {
            val pos = y.offset(x, -3, z)
            val block = level.getBlockState(pos).block
            if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
                level.setBlock(pos, Blocks.WHITE_CONCRETE.defaultBlockState(), 3)
            }
        }
    }
}

/**
 * Position the player at an armor stand.
 */
fun GameTestHelper.positionAtArmorStand() {
    val entities = level.getEntities(null, bounds) { it.name.string == testName }
    if (entities.size <= 0 || entities[0] !is ArmorStandEntity) throw IllegalStateException("Cannot find armor stand")

    val stand = entities[0] as ArmorStandEntity
    val player = level.randomPlayer ?: throw NullPointerException("Player does not exist")

    player.connection.teleport(stand.x, stand.y, stand.z, stand.yRot, stand.xRot)
}


class ClientTestHelper {
    val minecraft: Minecraft = Minecraft.getInstance()

    fun screenshot(name: String) {
        ScreenShotHelper.grab(
            minecraft.gameDirectory, name,
            minecraft.window.width, minecraft.window.height, minecraft.mainRenderTarget
        ) { TestMod.log.info(it.string) }
    }
}

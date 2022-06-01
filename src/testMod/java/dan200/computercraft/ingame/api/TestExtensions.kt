package dan200.computercraft.ingame.api

import dan200.computercraft.ingame.mod.ImageUtils
import dan200.computercraft.ingame.mod.TestMod
import net.minecraft.block.BlockState
import net.minecraft.client.Minecraft
import net.minecraft.command.arguments.BlockStateInput
import net.minecraft.entity.item.ArmorStandEntity
import net.minecraft.util.ScreenShotHelper
import net.minecraft.util.math.BlockPos
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import javax.imageio.ImageIO

object Times {
    const val NOON: Long = 6000
}

/**
 * Custom timeouts for various test types.
 */
object Timeouts {
    private const val SECOND: Int = 20

    const val COMPUTER_TIMEOUT: Int = SECOND * 15

    const val CLIENT_TIMEOUT: Int = SECOND * 20
}

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

    var counter = 0
    val hasScreenshot = AtomicBoolean()

    return this
        // Wait until all chunks have been rendered and we're idle for an extended period.
        .thenExecute { counter = 0 }
        .thenWaitUntil {
            val renderer = Minecraft.getInstance().levelRenderer
            if (renderer.chunkRenderDispatcher != null && renderer.hasRenderedAllChunks()) {
                val idleFor = ++counter
                if (idleFor <= 20) throw GameTestAssertException("Only idle for $idleFor ticks")
            } else {
                counter = 0
                throw GameTestAssertException("Waiting for client to finish rendering")
            }
        }
        // Now disable the GUI, take a screenshot and reenable it. We sleep either side to give the client time to do
        // its thing.
        .thenExecute {
            Minecraft.getInstance().options.hideGui = true
            hasScreenshot.set(false)
        }
        .thenIdle(5) // Some delay before/after to ensure the render thread has caught up.
        .thenOnClient { screenshot("$fullName.png") { hasScreenshot.set(true) } }
        .thenWaitUntil { if (!hasScreenshot.get()) throw GameTestAssertException("Screenshot does not exist") }
        .thenExecute {
            Minecraft.getInstance().options.hideGui = false

            val screenshotsPath = Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots")
            val screenshotPath = screenshotsPath.resolve("$fullName.png")
            val originalPath = TestMod.sourceDir.resolve("screenshots").resolve("$fullName.png")

            if (!Files.exists(originalPath)) throw GameTestAssertException("$fullName does not exist. Use `/cctest promote' to create it.");

            val screenshot = ImageIO.read(screenshotPath.toFile())
                ?: throw GameTestAssertException("Error reading screenshot from $screenshotPath")
            val original = ImageIO.read(originalPath.toFile())

            if (screenshot.width != original.width || screenshot.height != original.height) {
                throw GameTestAssertException("$fullName screenshot is ${screenshot.width}x${screenshot.height} but original is ${original.width}x${original.height}")
            }

            ImageUtils.writeDifference(screenshotsPath.resolve("$fullName.diff.png"), screenshot, original)
            if (!ImageUtils.areSame(screenshot, original)) throw GameTestAssertException("Images are different.")
        }
}

val GameTestHelper.testName: String get() = tracker.testName

val GameTestHelper.structureName: String get() = tracker.structureName

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
 * Position the player at an armor stand.
 */
fun GameTestHelper.positionAtArmorStand() {
    val entities = level.getEntities(null, bounds) { it.name.string == structureName }
    if (entities.size <= 0 || entities[0] !is ArmorStandEntity) throw GameTestAssertException("Cannot find armor stand")

    val stand = entities[0] as ArmorStandEntity
    val player = level.randomPlayer ?: throw GameTestAssertException("Player does not exist")

    player.connection.teleport(stand.x, stand.y, stand.z, stand.yRot, stand.xRot)
}


class ClientTestHelper {
    val minecraft: Minecraft = Minecraft.getInstance()

    fun screenshot(name: String, callback: () -> Unit = {}) {
        ScreenShotHelper.grab(
            minecraft.gameDirectory, name,
            minecraft.window.width, minecraft.window.height, minecraft.mainRenderTarget
        ) {
            TestMod.log.info(it.string)
            callback()
        }
    }
}

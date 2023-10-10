// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.api

import dan200.computercraft.gametest.core.MinecraftExtensions
import dan200.computercraft.mixin.gametest.GameTestSequenceAccessor
import dan200.computercraft.shared.platform.RegistryWrappers
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.screens.inventory.MenuAccess
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.GameTestSequence
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Attempt to guess whether all chunks have been rendered.
 */
fun Minecraft.isRenderingStable(): Boolean = (this as MinecraftExtensions).`computercraft$isRenderingStable`()

/**
 * Run a task on the client.
 */
fun GameTestSequence.thenOnClient(task: ClientTestHelper.() -> Unit): GameTestSequence {
    var future: CompletableFuture<Void>? = null
    thenExecute { future = Minecraft.getInstance().submit { task(ClientTestHelper()) } }
    thenWaitUntil { if (!future!!.isDone) throw GameTestAssertException("Not done task yet") }
    thenExecute {
        try {
            future!!.get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }
    return this
}

/**
 * Take a screenshot of the current game state.
 */
fun GameTestSequence.thenScreenshot(name: String? = null, showGui: Boolean = false): GameTestSequence {
    val suffix = if (name == null) "" else "-$name"
    val test = (this as GameTestSequenceAccessor).parent
    val fullName = "${test.testName}$suffix"

    // Wait until all chunks have been rendered and we're idle for an extended period.
    var counter = 0
    thenWaitUntil {
        if (Minecraft.getInstance().isRenderingStable()) {
            val idleFor = ++counter
            if (idleFor <= 20) throw GameTestAssertException("Only idle for $idleFor ticks")
        } else {
            counter = 0
            throw GameTestAssertException("Waiting for client to finish rendering")
        }
    }

    // Now disable the GUI, take a screenshot and reenable it. Sleep a little afterwards to ensure the render thread
    // has caught up.
    thenOnClient { minecraft.options.hideGui = !showGui }
    thenIdle(2)

    // Take a screenshot and wait for it to have finished.
    val hasScreenshot = AtomicBoolean()
    thenOnClient { screenshot("$fullName.png") { hasScreenshot.set(true) } }
    thenWaitUntil { if (!hasScreenshot.get()) throw GameTestAssertException("Screenshot does not exist") }
    thenOnClient { minecraft.options.hideGui = false }

    return this
}

/**
 * "Reset" the current player, ensuring.
 */
fun ServerPlayer.setupForTest() {
    if (containerMenu != inventoryMenu) closeContainer()
}

/**
 * Position the player at an armor stand.
 */
fun GameTestHelper.positionAtArmorStand() {
    val stand = getEntity(EntityType.ARMOR_STAND)
    val player = level.randomPlayer ?: throw GameTestAssertException("Player does not exist")

    player.setupForTest()
    player.connection.teleport(stand.x, stand.y, stand.z, stand.yRot, stand.xRot)
}

/**
 * Position the player at a given coordinate.
 */
fun GameTestHelper.positionAt(pos: BlockPos, yRot: Float = 0.0f, xRot: Float = 0.0f) {
    val absolutePos = absolutePos(pos)
    val player = level.randomPlayer ?: throw GameTestAssertException("Player does not exist")

    player.setupForTest()
    player.connection.teleport(absolutePos.x + 0.5, absolutePos.y + 0.5, absolutePos.z + 0.5, yRot, xRot)
}

/**
 * The equivalent of a [GameTestHelper] on the client.
 */
class ClientTestHelper {
    val minecraft: Minecraft = Minecraft.getInstance()

    fun screenshot(name: String, callback: () -> Unit = {}) {
        Screenshot.grab(minecraft.gameDirectory, name, minecraft.mainRenderTarget) { callback() }
    }

    /**
     * Get the currently open [AbstractContainerMenu], ensuring it is of a specific type.
     */
    fun <T : AbstractContainerMenu> getOpenMenu(type: MenuType<T>): T {
        fun getName(type: MenuType<*>) = RegistryWrappers.MENU.getKey(type)

        val screen = minecraft.screen
        @Suppress("UNCHECKED_CAST")
        when {
            screen == null -> throw GameTestAssertException("Expected a ${getName(type)} menu, but no screen is open")
            screen !is MenuAccess<*> -> throw GameTestAssertException("Expected a ${getName(type)} menu, but a $screen is open")
            screen.menu.type != type -> throw GameTestAssertException("Expected a ${getName(type)} menu, but a ${getName(screen.menu.type)} is open")
            else -> return screen.menu as T
        }
    }
}

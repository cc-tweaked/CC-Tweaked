// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import com.mojang.authlib.GameProfile
import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.lua.Coerced
import dan200.computercraft.api.upgrades.UpgradeData
import dan200.computercraft.client.pocket.ClientPocketComputers
import dan200.computercraft.core.apis.TermAPI
import dan200.computercraft.gametest.api.*
import dan200.computercraft.impl.PocketUpgrades
import dan200.computercraft.mixin.gametest.GameTestHelperAccessor
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.computer.core.ComputerState
import dan200.computercraft.shared.platform.PlatformHelper
import dan200.computercraft.shared.pocket.items.PocketComputerItem
import dan200.computercraft.test.core.computer.getApi
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.GameTestSequence
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertNotNull
import java.util.*
import kotlin.random.Random

class Pocket_Computer_Test {
    /**
     * Asserts that ticking a pocket computer in the inventory turns it on.
     */
    @GameTest(template = Structures.DEFAULT)
    fun Turns_on_in_inventory(context: GameTestHelper) = context.immediate {
        val player = PlatformHelper.get().createFakePlayer(context.level, GAME_PROFILE)

        val stack = ModRegistry.Items.POCKET_COMPUTER_NORMAL.get().create(
            -1,
            null,
            -1,
            UpgradeData.ofDefault(PocketUpgrades.instance().get(MODEM)!!),
        )
        stack.orCreateTag.putBoolean(PocketComputerItem.NBT_ON, true)
        player.setItemInHand(InteractionHand.MAIN_HAND, stack)

        player.inventory.tick()

        assertNotNull(PocketComputerItem.getInstanceID(stack))
    }

    /**
     * Checks pocket computer state is synced to the holding player.
     */
    @ClientGameTest(template = Structures.DEFAULT)
    fun Sync_state(context: GameTestHelper) = context.sequence {
        // We use a unique label for each test run as computers from previous runs may not have been disposed yet.
        val unique = java.lang.Long.toHexString(Random.nextLong())

        // Give the player a pocket computer.
        thenExecute {
            context.positionAt(BlockPos(2, 2, 2))
            context.givePocketComputer(unique)
        }
        // Write some text to the computer.
        thenOnComputer(unique) { getApi<TermAPI>().write(Coerced("Hello, world!")) }
        // And ensure its synced to the client.
        thenIdle(4)
        thenOnClient {
            val pocketComputer = ClientPocketComputers.get(minecraft.player!!.mainHandItem)!!
            assertEquals(ComputerState.ON, pocketComputer.state)

            val term = pocketComputer.terminal!!
            assertEquals("Hello, world!", term.getLine(0).toString().trim(), "Terminal contents is synced")
        }
        // Update the terminal contents again.
        thenOnComputer(unique) {
            val term = getApi<TermAPI>()
            term.setCursorPos(1, 1)
            term.setCursorBlink(true)
            term.write(Coerced("Updated text :)"))
        }
        // And ensure the new computer state and terminal are sent.
        thenIdle(4)
        thenOnClient {
            val pocketComputer = ClientPocketComputers.get(minecraft.player!!.mainHandItem)!!
            assertEquals(ComputerState.BLINKING, pocketComputer.state)

            val term = pocketComputer.terminal!!
            assertEquals("Updated text :)", term.getLine(0).toString().trim(), "Terminal contents is synced")
        }
    }

    /**
     * Checks pocket computers are rendered when being held like a map.
     */
    @ClientGameTest(template = Structures.DEFAULT)
    fun Renders_map_view(context: GameTestHelper) = context.sequence {
        // We use a unique label for each test run as computers from previous runs may not have been disposed yet.
        val unique = java.lang.Long.toHexString(Random.nextLong())

        // Give the player a pocket computer.
        thenExecute {
            context.positionAt(BlockPos(2, 2, 2), xRot = 90.0f)
            context.givePocketComputer(unique)
        }
        thenOnComputer(unique) {
            val terminal = getApi<TermAPI>().terminal
            terminal.write("Hello, world!")
            terminal.setCursorPos(1, 2)
            terminal.textColour = 2
            terminal.backgroundColour = 3
            terminal.write("Some coloured text")
        }
        thenIdle(4)
        thenScreenshot(showGui = true)
    }

    /**
     * Give the current player a pocket computer, suitable to be controlled by [GameTestSequence.thenOnComputer].
     */
    private fun GameTestHelper.givePocketComputer(name: String? = null) {
        val player = level.randomPlayer!!
        player.inventory.clearContent()

        val testName = (this as GameTestHelperAccessor).testInfo.testName
        val label = testName + (if (name == null) "" else ".$name")

        val item = ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get().create(1, label, -1, null)
        item.getOrCreateTag().putBoolean(PocketComputerItem.NBT_ON, true)
        player.inventory.setItem(0, item)
    }

    companion object {
        private val MODEM = ResourceLocation(ComputerCraftAPI.MOD_ID, "wireless_modem_normal").toString()

        /** Fake game profile. This is the same as the one in [dan200.computercraft.shared.turtle.core.TurtlePlayer] */
        private val GAME_PROFILE =
            GameProfile(UUID.fromString("0d0c4ca0-4ff1-11e4-916c-0800200c9a66"), "[ComputerCraft]")
    }
}

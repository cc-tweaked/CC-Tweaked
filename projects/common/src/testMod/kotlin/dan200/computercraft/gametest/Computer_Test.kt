/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest

import dan200.computercraft.api.lua.ObjectArguments
import dan200.computercraft.client.gui.AbstractComputerScreen
import dan200.computercraft.core.apis.RedstoneAPI
import dan200.computercraft.core.apis.TermAPI
import dan200.computercraft.core.computer.ComputerSide
import dan200.computercraft.gametest.api.*
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.test.core.computer.getApi
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeverBlock
import net.minecraft.world.level.block.RedstoneLampBlock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.lwjgl.glfw.GLFW

class Computer_Test {
    /**
     * Ensures redstone signals do not travel through computers.
     *
     * @see [#548](https://github.com/cc-tweaked/CC-Tweaked/issues/548)
     */
    @GameTest
    fun No_through_signal(context: GameTestHelper) = context.sequence {
        val lamp = BlockPos(2, 2, 4)
        val lever = BlockPos(2, 2, 0)
        thenExecute {
            context.assertBlockHas(lamp, RedstoneLampBlock.LIT, false, "Lamp should not be lit")
            context.modifyBlock(lever) { x -> x.setValue(LeverBlock.POWERED, true) }
        }
        thenIdle(3)
        thenExecute { context.assertBlockHas(lamp, RedstoneLampBlock.LIT, false, "Lamp should still not be lit") }
    }

    /**
     * Similar to the above, but with a repeater before the computer
     */
    @GameTest
    fun No_through_signal_reverse(context: GameTestHelper) = context.sequence {
        val lamp = BlockPos(2, 2, 4)
        val lever = BlockPos(2, 2, 0)
        thenExecute {
            context.assertBlockHas(lamp, RedstoneLampBlock.LIT, false, "Lamp should not be lit")
            context.modifyBlock(lever) { x -> x.setValue(LeverBlock.POWERED, true) }
        }
        thenIdle(3)
        thenExecute { context.assertBlockHas(lamp, RedstoneLampBlock.LIT, false, "Lamp should still not be lit") }
    }

    /**
     * Check computers propagate redstone to surrounding blocks.
     */
    @GameTest
    fun Set_and_destroy(context: GameTestHelper) = context.sequence {
        val lamp = BlockPos(2, 2, 3)

        thenOnComputer { getApi<RedstoneAPI>().setOutput(ComputerSide.BACK, true) }
        thenIdle(3)
        thenExecute { context.assertBlockHas(lamp, RedstoneLampBlock.LIT, true, "Lamp should be lit") }
        thenExecute { context.setBlock(BlockPos(2, 2, 2), Blocks.AIR) }
        thenIdle(4)
        thenExecute { context.assertBlockHas(lamp, RedstoneLampBlock.LIT, false, "Lamp should not be lit") }
    }

    /**
     * Check computers and turtles expose peripherals.
     */
    @GameTest
    fun Computer_peripheral(context: GameTestHelper) = context.sequence {
        thenExecute {
            context.assertPeripheral(BlockPos(3, 2, 2), type = "computer")
            context.assertPeripheral(BlockPos(1, 2, 2), type = "turtle")
        }
    }

    /**
     * Check the client can open the computer UI and interact with it.
     */
    @ClientGameTest
    fun Open_on_client(context: GameTestHelper) = context.sequence {
        // Write "Hello, world!" and then print each event to the terminal.
        thenOnComputer { getApi<TermAPI>().write(ObjectArguments("Hello, world!")) }
        thenStartComputer {
            val term = getApi<TermAPI>().terminal
            while (true) {
                val event = pullEvent()
                if (term.cursorY >= term.height) {
                    term.scroll(1)
                    term.setCursorPos(0, term.height)
                } else {
                    term.setCursorPos(0, term.cursorY + 1)
                }

                term.write(event.contentToString())
            }
        }
        // Teleport the player to the computer and then open it.
        thenExecute {
            context.positionAt(BlockPos(2, 2, 1))
            val computer = context.getBlockEntity(BlockPos(2, 2, 2), ModRegistry.BlockEntities.COMPUTER_ADVANCED.get())
            computer.use(context.level.randomPlayer!!, InteractionHand.MAIN_HAND)
        }
        // Assert the terminal is synced to the client.
        thenIdle(2)
        thenOnClient {
            val menu = getOpenMenu(ModRegistry.Menus.COMPUTER.get())
            val term = menu.terminal
            assertEquals("Hello, world!", term.getLine(0).toString().trim(), "Terminal contents is synced")
            assertTrue(menu.isOn, "Computer is on")
        }
        // Press a key on the client
        thenOnClient {
            val screen = minecraft.screen as AbstractComputerScreen<*>
            screen.keyPressed(GLFW.GLFW_KEY_A, 0, 0)
            screen.keyReleased(GLFW.GLFW_KEY_A, 0, 0)
        }
        // And assert it is handled and sent back to the client
        thenIdle(2)
        thenOnClient {
            val term = getOpenMenu(ModRegistry.Menus.COMPUTER.get()).terminal
            assertEquals(
                "[key, ${GLFW.GLFW_KEY_A}, false]",
                term.getLine(1).toString().trim(),
                "Terminal contents is synced",
            )
            assertEquals(
                "[key_up, ${GLFW.GLFW_KEY_A}]",
                term.getLine(2).toString().trim(),
                "Terminal contents is synced",
            )
        }
    }
}

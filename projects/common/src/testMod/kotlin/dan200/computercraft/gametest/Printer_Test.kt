// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.assertBlockHas
import dan200.computercraft.gametest.api.assertExactlyItems
import dan200.computercraft.gametest.api.getBlockEntity
import dan200.computercraft.gametest.api.sequence
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.media.items.PrintoutData
import dan200.computercraft.shared.peripheral.printer.PrinterBlock
import dan200.computercraft.shared.util.DataComponentUtil
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.RedStoneWireBlock
import org.junit.jupiter.api.Assertions.assertEquals

class Printer_Test {
    /**
     * Check comparators can read the contents of the disk drive
     */
    @GameTest
    fun Comparator(helper: GameTestHelper) = helper.sequence {
        val printerPos = BlockPos(2, 1, 2)
        val dustPos = BlockPos(2, 1, 4)

        // Adding items should provide power
        thenExecute {
            val drive = helper.getBlockEntity(printerPos, ModRegistry.BlockEntities.PRINTER.get())
            drive.setItem(0, ItemStack(Items.BLACK_DYE))
            drive.setItem(1, ItemStack(Items.PAPER))
            drive.setChanged()
        }
        thenIdle(2)
        thenExecute { helper.assertBlockHas(dustPos, RedStoneWireBlock.POWER, 1) }

        // And removing them should reset power.
        thenExecute {
            val drive = helper.getBlockEntity(printerPos, ModRegistry.BlockEntities.PRINTER.get())
            drive.clearContent()
            drive.setChanged()
        }
        thenIdle(2)
        thenExecute { helper.assertBlockHas(dustPos, RedStoneWireBlock.POWER, 0) }
    }

    /**
     * Changing the inventory contents updates the block state
     */
    @GameTest
    fun Contents_updates_state(helper: GameTestHelper) = helper.sequence {
        val pos = BlockPos(2, 1, 2)

        thenExecute {
            val drive = helper.getBlockEntity(pos, ModRegistry.BlockEntities.PRINTER.get())

            drive.setItem(1, ItemStack(Items.PAPER))
            drive.setChanged()
            helper.assertBlockHas(pos, PrinterBlock.TOP, true, message = "One item in the top row")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, false, message = "One item in the top row")

            drive.setItem(7, ItemStack(Items.PAPER))
            drive.setChanged()
            helper.assertBlockHas(pos, PrinterBlock.TOP, true, message = "One item in each row")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, true, message = "One item in each row")

            drive.setItem(1, ItemStack.EMPTY)
            drive.setChanged()
            helper.assertBlockHas(pos, PrinterBlock.TOP, false, message = "One item in the bottom")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, true, message = "One item in the bottom row")

            drive.setItem(7, ItemStack.EMPTY)
            drive.setChanged()
            helper.assertBlockHas(pos, PrinterBlock.TOP, false, message = "Empty")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, false, message = "Empty")
        }
    }

    /**
     * When the block is broken, we drop the contents and an optionally named stack.
     */
    @GameTest
    fun Drops_contents(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            helper.level.destroyBlock(helper.absolutePos(BlockPos(2, 1, 2)), true)
            helper.assertExactlyItems(
                DataComponentUtil.createStack(ModRegistry.Items.PRINTER.get(), DataComponents.CUSTOM_NAME, Component.literal("My Printer")),
                ItemStack(Items.PAPER),
                ItemStack(Items.BLACK_DYE),
                message = "Breaking a printer should drop the contents",
            )
        }
    }

    /**
     * Loads a structure created on an older version of the game, and checks that data fixers have been applied.
     */
    @GameTest
    fun Data_fixers(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            val container = helper.getBlockEntity(BlockPos(2, 1, 2), ModRegistry.BlockEntities.PRINTER.get())
            val contents = container.getItem(1)
            assertEquals(ModRegistry.Items.PRINTED_PAGE.get(), contents.item)

            val printout = contents[ModRegistry.DataComponents.PRINTOUT.get()] ?: PrintoutData.EMPTY
            assertEquals("example.lua", printout.title)
            assertEquals("This is an example page  ", printout.lines[0].text)
            assertEquals("3333333333333333333333333", printout.lines[0].foreground)
        }
    }
}

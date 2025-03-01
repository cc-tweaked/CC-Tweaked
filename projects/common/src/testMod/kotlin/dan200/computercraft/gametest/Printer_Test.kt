// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.api.lua.Coerced
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.gametest.api.*
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.media.items.PrintoutData
import dan200.computercraft.shared.peripheral.printer.PrinterBlock
import dan200.computercraft.shared.peripheral.printer.PrinterPeripheral
import dan200.computercraft.shared.util.DataComponentUtil
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.RedStoneWireBlock
import org.junit.jupiter.api.Assertions.*
import java.util.*

class Printer_Test {
    /**
     * Check comparators can read the contents of the printer
     */
    @GameTest
    fun Comparator(helper: GameTestHelper) = helper.sequence {
        val printerPos = BlockPos(2, 2, 2)
        val dustPos = BlockPos(2, 2, 4)

        // Adding items should provide power
        thenExecute {
            val printer = helper.getBlockEntity(printerPos, ModRegistry.BlockEntities.PRINTER.get())
            printer.setItem(0, ItemStack(Items.BLACK_DYE))
            printer.setItem(1, ItemStack(Items.PAPER))
            printer.setChanged()
        }
        thenIdle(2)
        thenExecute { helper.assertBlockHas(dustPos, RedStoneWireBlock.POWER, 1) }

        // And removing them should reset power.
        thenExecute {
            val printer = helper.getBlockEntity(printerPos, ModRegistry.BlockEntities.PRINTER.get())
            printer.clearContent()
            printer.setChanged()
        }
        thenIdle(2)
        thenExecute { helper.assertBlockHas(dustPos, RedStoneWireBlock.POWER, 0) }
    }

    /**
     * Changing the inventory contents updates the block state
     */
    @GameTest(template = "printer_test.empty")
    fun Contents_updates_state(helper: GameTestHelper) = helper.sequence {
        val pos = BlockPos(2, 2, 2)

        thenExecute {
            val printer = helper.getBlockEntity(pos, ModRegistry.BlockEntities.PRINTER.get())

            printer.setItem(1, ItemStack(Items.PAPER))
            printer.setChanged()
            helper.assertBlockHas(pos, PrinterBlock.TOP, true, message = "One item in the top row")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, false, message = "One item in the top row")

            printer.setItem(7, ItemStack(Items.PAPER))
            printer.setChanged()
            helper.assertBlockHas(pos, PrinterBlock.TOP, true, message = "One item in each row")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, true, message = "One item in each row")

            printer.setItem(1, ItemStack.EMPTY)
            printer.setChanged()
            helper.assertBlockHas(pos, PrinterBlock.TOP, false, message = "One item in the bottom")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, true, message = "One item in the bottom row")

            printer.setItem(7, ItemStack.EMPTY)
            printer.setChanged()
            helper.assertBlockHas(pos, PrinterBlock.TOP, false, message = "Empty")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, false, message = "Empty")
        }
    }

    /**
     * Printing a page
     */
    @GameTest(template = "printer_test.empty")
    fun Print_page(helper: GameTestHelper) = helper.sequence {
        val pos = BlockPos(2, 2, 2)

        thenExecute {
            val printer = helper.getBlockEntity(pos, ModRegistry.BlockEntities.PRINTER.get())
            val peripheral = printer.peripheral() as PrinterPeripheral

            // Try to print with no pages
            assertFalse(peripheral.newPage(), "newPage fails with no items")

            // Try to print with just ink
            printer.setItem(0, ItemStack(Items.BLUE_DYE))
            printer.setChanged()
            assertFalse(peripheral.newPage(), "newPage fails with no paper")

            printer.clearContent()

            // Try to print with just paper
            printer.setItem(1, ItemStack(Items.PAPER))
            printer.setChanged()
            assertFalse(peripheral.newPage(), "newPage fails with no ink")

            printer.clearContent()

            // Try to print with both items
            printer.setItem(0, ItemStack(Items.BLUE_DYE))
            printer.setItem(1, ItemStack(Items.PAPER))
            printer.setChanged()
            assertTrue(peripheral.newPage(), "newPage succeeds")

            // newPage() should consume both items and update the block state
            helper.assertContainerEmpty(pos)
            helper.assertBlockHas(pos, PrinterBlock.TOP, false, message = "Empty")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, false, message = "Empty")

            assertFalse(peripheral.newPage(), "Cannot start a page when already printing")

            peripheral.setPageTitle(Optional.of("New Page"))
            peripheral.write(Coerced("Hello, world!"))
            peripheral.setCursorPos(5, 2)
            peripheral.write(Coerced("Second line"))

            // Try to finish the page
            assertTrue(peripheral.endPage(), "endPage prints item")

            // endPage() should
            helper.assertBlockHas(pos, PrinterBlock.TOP, false, message = "Empty")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, true, message = "Has pages")

            // And check the inventory matches
            val emptyLine = createEmptyLine('b')
            val lines = MutableList(PrintoutData.LINES_PER_PAGE) { emptyLine }
            lines[0] = lines[0].text("Hello, world!            ")
            lines[1] = lines[1].text("    Second line          ")

            helper.assertContainerExactly(
                pos,
                listOf(
                    // Ink
                    ItemStack.EMPTY,
                    // Paper
                    ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                    // Pages
                    DataComponentUtil.createStack(ModRegistry.Items.PRINTED_PAGE.get(), ModRegistry.DataComponents.PRINTOUT.get(), PrintoutData("New Page", lines)),
                    ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ),
            )

            val error = assertThrows(LuaException::class.java) { peripheral.endPage() }
            assertEquals("Page not started", error.message)
        }
    }

    /**
     * Can't print when full.
     */
    @GameTest
    fun No_print_when_full(helper: GameTestHelper) = helper.sequence {
        val pos = BlockPos(2, 2, 2)

        thenExecute {
            val printer = helper.getBlockEntity(pos, ModRegistry.BlockEntities.PRINTER.get())
            val peripheral = printer.peripheral() as PrinterPeripheral
            assertTrue(peripheral.newPage())
            assertFalse(peripheral.endPage(), "Cannot print when full")
        }
    }

    /**
     * When the block is broken, we drop the contents and an optionally named stack.
     */
    @GameTest
    fun Drops_contents(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            helper.level.destroyBlock(helper.absolutePos(BlockPos(2, 2, 2)), true)
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
            val container = helper.getBlockEntity(BlockPos(2, 2, 2), ModRegistry.BlockEntities.PRINTER.get())
            val contents = container.getItem(1)
            assertEquals(ModRegistry.Items.PRINTED_PAGE.get(), contents.item)

            val printout = contents[ModRegistry.DataComponents.PRINTOUT.get()] ?: PrintoutData.EMPTY
            assertEquals("example.lua", printout.title)
            assertEquals("This is an example page  ", printout.lines[0].text)
            assertEquals("3333333333333333333333333", printout.lines[0].foreground)
        }
    }

    private fun createEmptyLine(bg: Char): PrintoutData.Line {
        return PrintoutData.Line(" ".repeat(PrintoutData.LINE_LENGTH), bg.toString().repeat(PrintoutData.LINE_LENGTH))
    }

    fun PrintoutData.Line.text(text: String) = PrintoutData.Line(text, foreground)
}

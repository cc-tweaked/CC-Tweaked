// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.*
import dan200.computercraft.gametest.core.TestHooks
import dan200.computercraft.gametest.core.TestInstance
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.media.items.PrintoutData
import dan200.computercraft.shared.util.DataComponentUtil
import dan200.computercraft.test.shared.ItemStackMatcher.isStack
import net.minecraft.core.Holder
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.TestData
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.function.Supplier

class Printout_Test {
    /**
     * Test printouts render correctly
     */
    @GameTestGenerator
    fun Render_in_frame(): List<TestInstance> {
        val tests = mutableListOf<TestInstance>()

        fun addTest(label: String, time: Int = Times.NOON, tag: String = TestTags.CLIENT) {
            if (!TestTags.isEnabled(tag)) return

            val className = this::class.java.simpleName.lowercase()
            val testName = "$className.render_in_frame"

            tests.add(
                TestInstance(
                    "$testName.$label",
                    TestData(
                        Holder.direct(ClientTestEnvironment(time)),
                        Identifier.fromNamespaceAndPath(TestHooks.MOD_ID, testName),
                        Timeouts.DEFAULT,
                        0,
                        true,
                    ),
                ) { renderPrintout(it) },
            )
        }

        addTest("noon", Times.NOON)
        addTest("midnight", Times.MIDNIGHT)

        addTest("sodium", tag = "sodium")

        addTest("iris_noon", Times.NOON, tag = "iris")
        addTest("iris_midnight", Times.MIDNIGHT, tag = "iris")

        return tests
    }

    private fun renderPrintout(helper: GameTestHelper) = helper.sequence {
        thenExecute { helper.positionAtArmorStand() }
        thenScreenshot()
    }

    @GameTest(template = "default")
    fun Craft_pages(helper: GameTestHelper) = helper.immediate {
        // Assert that crafting with only one page fails
        helper.assertNotCraftable(ItemStack(ModRegistry.Items.PRINTED_PAGE.get()), ItemStack(Items.STRING))

        // Assert that crafting with no pages fails
        helper.assertNotCraftable(ItemStack(Items.PAPER), ItemStack(Items.PAPER), ItemStack(Items.STRING))

        // Assert that crafting with a book fails
        helper.assertNotCraftable(ItemStack(ModRegistry.Items.PRINTED_PAGE.get()), ItemStack(ModRegistry.Items.PRINTED_BOOK.get()), ItemStack(Items.STRING))

        assertThat(
            helper.craftItem(
                createPrintoutOf(ModRegistry.Items.PRINTED_PAGE, "First"),
                createPrintoutOf(ModRegistry.Items.PRINTED_PAGES, "First"),
                ItemStack(Items.STRING),
            ),
            isStack(createPrintoutOf(ModRegistry.Items.PRINTED_PAGES, "First", 2)),
        )
    }

    @GameTest(template = "default")
    fun Craft_book(helper: GameTestHelper) = helper.immediate {
        // Assert that crafting with no pages fails
        helper.assertNotCraftable(ItemStack(Items.PAPER), ItemStack(Items.PAPER), ItemStack(Items.STRING), ItemStack(Items.LEATHER))

        // Assert that crafting with only one page works
        assertEquals(
            ModRegistry.Items.PRINTED_BOOK.get(),
            helper.craftItem(ItemStack(ModRegistry.Items.PRINTED_PAGE.get()), ItemStack(Items.STRING), ItemStack(Items.LEATHER)).item,
        )

        assertThat(
            helper.craftItem(
                createPrintoutOf(ModRegistry.Items.PRINTED_PAGE, "First"),
                createPrintoutOf(ModRegistry.Items.PRINTED_PAGES, "First"),
                ItemStack(Items.STRING),
                ItemStack(Items.LEATHER),
            ),
            isStack(createPrintoutOf(ModRegistry.Items.PRINTED_BOOK, "First", 2)),
        )
    }

    private fun createPrintoutOf(item: Supplier<out Item>, title: String, pages: Int = 1): ItemStack {
        val line =
            PrintoutData.Line(" ".repeat(PrintoutData.LINE_LENGTH), 'b'.toString().repeat(PrintoutData.LINE_LENGTH))
        val lines = List(PrintoutData.LINES_PER_PAGE * pages) { line }
        return DataComponentUtil.createStack(item.get(), ModRegistry.DataComponents.PRINTOUT.get(), PrintoutData(title, lines))
    }
}

// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.lua.Coerced
import dan200.computercraft.api.pocket.IPocketUpgrade
import dan200.computercraft.api.upgrades.UpgradeData
import dan200.computercraft.client.pocket.ClientPocketComputers
import dan200.computercraft.core.apis.TermAPI
import dan200.computercraft.gametest.api.*
import dan200.computercraft.impl.PocketUpgrades
import dan200.computercraft.mixin.gametest.GameTestHelperAccessor
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.computer.core.ComputerState
import dan200.computercraft.shared.util.DataComponentUtil
import dan200.computercraft.shared.util.NonNegativeId
import dan200.computercraft.test.core.computer.getApi
import dan200.computercraft.test.shared.ItemStackMatcher.isStack
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.GameTestSequence
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.random.Random

class Pocket_Computer_Test {
    /**
     * Checks pocket computer state is synced to the holding player.
     */
    @GameTest(template = Structures.DEFAULT, tag = TestTags.CLIENT)
    fun Sync_state(context: GameTestHelper) = context.sequence {
        // We use a unique label for each test run as computers from previous runs may not have been disposed yet.
        val unique = java.lang.Long.toHexString(Random.nextLong())

        // Give the player a pocket computer.
        thenExecute {
            context.positionAt(BlockPos(2, 1, 2))
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
    @GameTest(template = Structures.DEFAULT, tag = TestTags.CLIENT)
    fun Renders_map_view(context: GameTestHelper) = context.sequence {
        // We use a unique label for each test run as computers from previous runs may not have been disposed yet.
        val unique = java.lang.Long.toHexString(Random.nextLong())

        // Give the player a pocket computer.
        thenExecute {
            context.positionAt(BlockPos(2, 1, 2), xRot = 90.0f)
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

        val label = (this as GameTestHelperAccessor).testInfo.getComputerLabel(name)

        val item = ItemStack(ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get())
        item.set(DataComponents.CUSTOM_NAME, Component.literal(label))
        item.set(ModRegistry.DataComponents.ON.get(), true)
        player.inventory.setItem(0, item)
    }

    /**
     * Loads a structure created on an older version of the game, and checks that data fixers have been applied.
     */
    @GameTest
    fun Data_fixers(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            val upgrade = helper.level.registryAccess().lookupOrThrow(IPocketUpgrade.REGISTRY)
                .get(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "wireless_modem_normal"))
                .orElseThrow()

            helper.assertContainerExactly(
                BlockPos(2, 1, 2),
                listOf(
                    ItemStack(ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get()).also {
                        DataComponentUtil.setCustomName(it, "Test")
                        it.applyComponents(
                            DataComponentPatch.builder()
                                .set(ModRegistry.DataComponents.COMPUTER_ID.get(), NonNegativeId.Computer(123))
                                .set(ModRegistry.DataComponents.BACK_POCKET_UPGRADE.get(), UpgradeData.ofDefault(upgrade))
                                .build(),
                        )
                    },
                ),
            )
        }
    }

    /**
     * Test that turtles can be crafted with upgrades.
     */
    @GameTest(template = Structures.DEFAULT)
    fun Can_upgrades_be_crafted(helper: GameTestHelper) = helper.immediate {
        fun pocket(back: UpgradeData<IPocketUpgrade>? = null, bottom: UpgradeData<IPocketUpgrade>? = null): ItemStack {
            val item = ItemStack(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get())
            item.set(ModRegistry.DataComponents.BACK_POCKET_UPGRADE.get(), back)
            item.set(ModRegistry.DataComponents.BOTTOM_POCKET_UPGRADE.get(), bottom)
            return item
        }

        val registries = helper.level.registryAccess()
        val speaker = PocketUpgrades.instance().get(registries, ItemStack(ModRegistry.Items.SPEAKER.get()))!!
        val modem =
            PocketUpgrades.instance().get(registries, ItemStack(ModRegistry.Items.WIRELESS_MODEM_NORMAL.get()))!!

        // Check we can craft with upgrades
        assertThat(
            "Craft with item below",
            helper.craftItem(
                ItemStack.EMPTY, pocket(), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack(ModRegistry.Items.SPEAKER.get()), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ),
            isStack(pocket(bottom = speaker)),
        )
        assertThat(
            "Craft with item above",
            helper.craftItem(
                ItemStack.EMPTY, ItemStack(ModRegistry.Items.SPEAKER.get()), ItemStack.EMPTY,
                ItemStack.EMPTY, pocket(), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ),
            isStack(pocket(back = speaker)),
        )
        assertThat(
            "Craft with two items",
            helper.craftItem(
                ItemStack.EMPTY, ItemStack(ModRegistry.Items.SPEAKER.get()), ItemStack.EMPTY,
                ItemStack.EMPTY, pocket(), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack(ModRegistry.Items.WIRELESS_MODEM_NORMAL.get()), ItemStack.EMPTY,
            ),
            isStack(pocket(back = speaker, bottom = modem)),
        )
        assertThat(
            "Maintains upgrades",
            helper.craftItem(
                ItemStack.EMPTY, pocket(back = speaker), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack(ModRegistry.Items.WIRELESS_MODEM_NORMAL.get()), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ),
            isStack(pocket(back = speaker, bottom = modem)),
        )

        // Cannot craft when already have item
        helper.assertNotCraftable(
            ItemStack.EMPTY, ItemStack(ModRegistry.Items.SPEAKER.get()), ItemStack.EMPTY,
            ItemStack.EMPTY, pocket(back = modem), ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
        )

        // Cannot craft with an invalid upgrade
        helper.assertNotCraftable(
            ItemStack.EMPTY, pocket(), ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack(Items.DIRT), ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
        )

        // Cannot craft with extra items in the inventory
        helper.assertNotCraftable(
            ItemStack(Items.DIRT), ItemStack(ModRegistry.Items.SPEAKER.get()), ItemStack.EMPTY,
            ItemStack.EMPTY, pocket(), ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
        )
        helper.assertNotCraftable(
            ItemStack.EMPTY, ItemStack(ModRegistry.Items.SPEAKER.get()), ItemStack.EMPTY,
            ItemStack.EMPTY, pocket(), ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack(Items.DIRT),
        )
    }
}

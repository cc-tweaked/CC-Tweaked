// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import com.mojang.authlib.GameProfile
import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.lua.Coerced
import dan200.computercraft.api.pocket.IPocketUpgrade
import dan200.computercraft.client.pocket.ClientPocketComputers
import dan200.computercraft.core.apis.TermAPI
import dan200.computercraft.gametest.api.*
import dan200.computercraft.impl.PocketUpgrades
import dan200.computercraft.impl.UpgradeData
import dan200.computercraft.mixin.gametest.GameTestHelperAccessor
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.computer.core.ComputerState
import dan200.computercraft.shared.computer.core.ServerContext
import dan200.computercraft.shared.platform.PlatformHelper
import dan200.computercraft.shared.pocket.core.PocketServerComputer
import dan200.computercraft.shared.pocket.core.PocketSide
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
import net.minecraft.resources.ResourceKey
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertNotNull
import java.util.*
import kotlin.random.Random

class Pocket_Computer_Test {
    /**
     * Asserts that ticking a pocket computer in the inventory turns it on.
     */
    @GameTest(template = Structures.DEFAULT)
    fun Turns_on_in_inventory(context: GameTestHelper) = context.sequence {
        val player = PlatformHelper.get().createFakePlayer(context.level, GAME_PROFILE)
        val upgrade = UpgradeData.ofDefault(context.level.registryAccess().getOrThrow(MODEM))

        thenExecute {
            // Create our item and give it to the player
            val stack = context.createPocketComputer(upgrade = upgrade)
            stack.set(ModRegistry.DataComponents.ON.get(), true)
            player.setItemInHand(InteractionHand.MAIN_HAND, stack)
        }
        thenWaitUntil {
            // Now we update the inventory, and wait for the computer to turn on.
            player.inventory.tick()

            val stack = player.getItemInHand(InteractionHand.MAIN_HAND)

            val instanceId = stack.get(ModRegistry.DataComponents.COMPUTER.get())
            assertNotNull(instanceId, "Pocket computer should have an instance")

            val computer = instanceId.get(ServerContext.get(context.level.server).registry()) as PocketServerComputer
            assertEquals(computer.brain.getUpgrade(PocketSide.BACK), upgrade, "Upgrade should be set")
            context.assertTrue(computer.isOn, "Pocket computer should be on")
        }
    }

    /**
     * Tests crafting to apply an upgrade.
     */
    @GameTest(template = Structures.DEFAULT)
    fun Craft_apply_upgrade(context: GameTestHelper) = context.immediate {
        val player = PlatformHelper.get().createFakePlayer(context.level, GAME_PROFILE)

        // Set up our stack and ensure the computer is created
        val stack = context.createPocketComputer()
        player.setItemInHand(InteractionHand.MAIN_HAND, stack)
        player.inventory.tick()

        val instanceId = stack.get(ModRegistry.DataComponents.COMPUTER.get())
        assertNotNull(instanceId)
        val computer = instanceId.get(ServerContext.get(context.level.server).registry()) as PocketServerComputer

        // Remove the stack from the player's inventory and "craft" it with a new upgrade.
        val upgradedStack = stack.copy()
        val upgradeData = UpgradeData.ofDefault(context.level.registryAccess().getOrThrow(MODEM))
        upgradedStack.set(ModRegistry.DataComponents.BACK_POCKET_UPGRADE.get(), upgradeData)
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)

        upgradedStack.onCraftedBy(player, 1)

        assertEquals(instanceId, stack.get(ModRegistry.DataComponents.COMPUTER.get()), "Instance ID should not change")
        assertEquals(upgradeData, computer.brain.getUpgrade(PocketSide.BACK), "Upgrade should be updated")
    }

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

    /** Create a pocket computer, suitable to be controlled by [GameTestSequence.thenOnComputer]. */
    private fun GameTestHelper.createPocketComputer(name: String? = null, upgrade: UpgradeData<IPocketUpgrade>? = null): ItemStack {
        val label = (this as GameTestHelperAccessor).testInfo.getComputerLabel(name)
        val item = ItemStack(ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get())
        item.set(DataComponents.CUSTOM_NAME, Component.literal(label))
        item.set(ModRegistry.DataComponents.BACK_POCKET_UPGRADE.get(), upgrade)
        return item
    }

    /**
     * Give the current player a pocket computer, suitable to be controlled by [GameTestSequence.thenOnComputer].
     */
    private fun GameTestHelper.givePocketComputer(name: String? = null) {
        val player = level.randomPlayer!!
        player.inventory.clearContent()

        val item = createPocketComputer(name)
        item.set(ModRegistry.DataComponents.ON.get(), true)
        player.inventory.setItem(0, item)
    }

    /**
     * Loads a structure created on an older version of the game, and checks that data fixers have been applied.
     */
    @GameTest
    fun Data_fixers(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            val upgrade = UpgradeData.of(
                helper.level.registryAccess().getOrThrow(
                    ResourceKey.create(IPocketUpgrade.REGISTRY, Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "wireless_modem_advanced")),
                ),
                DataComponentUtil.createTemplate(ModRegistry.Items.WIRELESS_MODEM_ADVANCED.get(), DataComponents.CUSTOM_NAME, Component.literal("My favourite modem")),
                DataComponentPatch.EMPTY,
            )

            helper.assertContainerExactly(
                BlockPos(2, 1, 2),
                listOf(
                    ItemStack(ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get()).also {
                        DataComponentUtil.setCustomName(it, "Test")
                        it.applyComponents(
                            DataComponentPatch.builder()
                                .set(ModRegistry.DataComponents.COMPUTER_ID.get(), NonNegativeId.Computer(123))
                                .set(ModRegistry.DataComponents.BACK_POCKET_UPGRADE.get(), upgrade)
                                .set(ModRegistry.DataComponents.ON.get(), false)
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
        fun pocket(top: UpgradeData<IPocketUpgrade>? = null, back: UpgradeData<IPocketUpgrade>? = null): ItemStack {
            val item = ItemStack(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get())
            item.set(ModRegistry.DataComponents.TOP_POCKET_UPGRADE.get(), top)
            item.set(ModRegistry.DataComponents.BACK_POCKET_UPGRADE.get(), back)
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
            isStack(pocket(back = speaker)),
        )
        assertThat(
            "Craft with item above",
            helper.craftItem(
                ItemStack.EMPTY, ItemStack(ModRegistry.Items.SPEAKER.get()), ItemStack.EMPTY,
                ItemStack.EMPTY, pocket(), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ),
            isStack(pocket(top = speaker)),
        )
        assertThat(
            "Craft with two items",
            helper.craftItem(
                ItemStack.EMPTY, ItemStack(ModRegistry.Items.SPEAKER.get()), ItemStack.EMPTY,
                ItemStack.EMPTY, pocket(), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack(ModRegistry.Items.WIRELESS_MODEM_NORMAL.get()), ItemStack.EMPTY,
            ),
            isStack(pocket(top = speaker, back = modem)),
        )
        assertThat(
            "Maintains upgrades",
            helper.craftItem(
                ItemStack.EMPTY, pocket(top = speaker), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack(ModRegistry.Items.WIRELESS_MODEM_NORMAL.get()), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ),
            isStack(pocket(top = speaker, back = modem)),
        )

        // Cannot craft when already have item
        helper.assertNotCraftable(
            ItemStack.EMPTY, ItemStack(ModRegistry.Items.SPEAKER.get()), ItemStack.EMPTY,
            ItemStack.EMPTY, pocket(top = modem), ItemStack.EMPTY,
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

    companion object {
        private fun id(key: Identifier): ResourceKey<IPocketUpgrade> =
            ResourceKey.create(IPocketUpgrade.REGISTRY, key)

        private val MODEM = id(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "wireless_modem_normal"))

        /** Fake game profile. This is the same as the one in [dan200.computercraft.shared.turtle.core.TurtlePlayer] */
        private val GAME_PROFILE =
            GameProfile(UUID.fromString("0d0c4ca0-4ff1-11e4-916c-0800200c9a66"), "[ComputerCraft]")
    }
}

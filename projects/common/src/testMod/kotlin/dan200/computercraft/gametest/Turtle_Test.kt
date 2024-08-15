// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.api.detail.BasicItemDetailProvider
import dan200.computercraft.api.detail.VanillaDetailRegistries
import dan200.computercraft.api.lua.ObjectArguments
import dan200.computercraft.api.turtle.ITurtleUpgrade
import dan200.computercraft.api.turtle.TurtleSide
import dan200.computercraft.api.upgrades.UpgradeData
import dan200.computercraft.core.apis.PeripheralAPI
import dan200.computercraft.gametest.api.*
import dan200.computercraft.gametest.core.TestHooks
import dan200.computercraft.impl.TurtleUpgrades
import dan200.computercraft.mixin.gametest.GameTestHelperAccessor
import dan200.computercraft.mixin.gametest.GameTestInfoAccessor
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.media.items.PrintoutItem
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock
import dan200.computercraft.shared.peripheral.modem.wired.CableModemVariant
import dan200.computercraft.shared.peripheral.monitor.MonitorBlock
import dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState
import dan200.computercraft.shared.turtle.apis.TurtleAPI
import dan200.computercraft.shared.util.WaterloggableHelpers
import dan200.computercraft.test.core.assertArrayEquals
import dan200.computercraft.test.core.computer.LuaTaskContext
import dan200.computercraft.test.core.computer.getApi
import dan200.computercraft.test.shared.ItemStackMatcher.isStack
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FenceBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

class Turtle_Test {
    @GameTest
    fun Unequip_refreshes_peripheral(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            getApi<PeripheralAPI>().getType("right").assertArrayEquals("modem", message = "Starts with a modem")
            turtle.equipRight().await()
            getApi<PeripheralAPI>().getType("right").assertArrayEquals("drive", message = "Unequipping gives a drive")
        }
    }

    /**
     * Checks turtles can sheer sheep (and drop items)
     *
     * @see [#537](https://github.com/cc-tweaked/CC-Tweaked/issues/537)
     */
    @GameTest
    fun Shears_sheep(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            turtle.placeDown(ObjectArguments()).await()
                .assertArrayEquals(true, message = "Shears the sheep")

            assertEquals("minecraft:white_wool", getTurtleItemDetail(2)["name"])
        }
    }

    /**
     * Checks turtles can place lava.
     *
     * @see [#518](https://github.com/cc-tweaked/CC-Tweaked/issues/518)
     */
    @GameTest
    fun Place_lava(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            turtle.placeDown(ObjectArguments()).await()
                .assertArrayEquals(true, message = "Placed lava")
        }
        thenExecute { helper.assertBlockPresent(Blocks.LAVA, BlockPos(2, 2, 2)) }
    }

    /**
     * Checks turtles can write to signs.
     *
     * @see [#1611](https://github.com/cc-tweaked/CC-Tweaked/issues/1611)
     */
    @GameTest
    fun Place_sign(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            turtle.place(ObjectArguments("Test\nmessage")).await()
                .assertArrayEquals(true, message = "Placed sign")
        }
        thenExecute {
            val sign = helper.getBlockEntity(BlockPos(2, 2, 1), BlockEntityType.SIGN)
            val lines = listOf("", "Test", "message", "")
            for ((i, line) in lines.withIndex()) {
                assertEquals(line, sign.frontText.getMessage(i, false).string, "Line $i")
            }
        }
    }

    /**
     * Checks that calling [net.minecraft.world.item.Item.use] will not place blocks too far away.
     *
     * This is caused by items using [net.minecraft.world.item.Item.getPlayerPOVHitResult] to perform a ray trace, which
     * ignores turtle's reduced reach distance.
     *
     * @see [#1497](https://github.com/cc-tweaked/CC-Tweaked/issues/1497)
     */
    @GameTest
    fun Place_use_reach_limit(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            turtle.placeDown(ObjectArguments()).await()
                .assertArrayEquals(true, message = "Placed water")
        }
        thenExecute {
            helper.assertBlockPresent(Blocks.AIR, BlockPos(2, 2, 2))
            helper.assertBlockHas(BlockPos(2, 5, 2), BlockStateProperties.WATERLOGGED, true)
        }
    }

    /**
     * Checks turtles can place when waterlogged.
     *
     * @see [#385](https://github.com/cc-tweaked/CC-Tweaked/issues/385)
     */
    @GameTest
    fun Place_waterlogged(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            turtle.place(ObjectArguments()).await()
                .assertArrayEquals(true, message = "Placed oak fence")
        }
        thenExecute {
            helper.assertBlockIs(BlockPos(2, 2, 2)) { it.block == Blocks.OAK_FENCE && it.getValue(FenceBlock.WATERLOGGED) }
        }
    }

    /**
     * Checks turtles can pick up lava
     *
     * @see [#297](https://github.com/cc-tweaked/CC-Tweaked/issues/297)
     */
    @GameTest
    fun Gather_lava(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            turtle.placeDown(ObjectArguments()).await()
                .assertArrayEquals(true, message = "Picked up lava")

            assertEquals("minecraft:lava_bucket", getTurtleItemDetail()["name"])
        }
        thenExecute { helper.assertBlockPresent(Blocks.AIR, BlockPos(2, 2, 2)) }
    }

    /**
     * Checks turtles can hoe dirt.
     *
     * @see [#258](https://github.com/cc-tweaked/CC-Tweaked/issues/258)
     */
    @GameTest
    fun Hoe_dirt(helper: GameTestHelper) = helper.sequence {
        thenOnComputer { turtle.dig(Optional.empty()).await().assertArrayEquals(true, message = "Dug with hoe") }
        thenExecute { helper.assertBlockPresent(Blocks.FARMLAND, BlockPos(1, 2, 1)) }
    }

    /**
     * Checks turtles can hoe dirt with a block gap below them.
     *
     * @see [#1527](https://github.com/cc-tweaked/CC-Tweaked/issues/1527)
     */
    @GameTest
    fun Hoe_dirt_below(helper: GameTestHelper) = helper.sequence {
        thenOnComputer { turtle.digDown(Optional.empty()).await().assertArrayEquals(true, message = "Dug with hoe") }
        thenExecute { helper.assertBlockPresent(Blocks.FARMLAND, BlockPos(1, 1, 1)) }
    }

    /**
     * Checks turtles cannot hoe dirt with a block gap in front of them.
     */
    @GameTest
    fun Hoe_dirt_distant(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            turtle.dig(Optional.empty()).await()
                .assertArrayEquals(false, "Nothing to dig here", message = "Dug with hoe")
        }
        thenExecute { helper.assertBlockPresent(Blocks.DIRT, BlockPos(1, 2, 2)) }
    }

    /**
     * Checks turtles break cables in two parts.
     */
    @GameTest
    fun Break_cable(helper: GameTestHelper) = helper.sequence {
        thenOnComputer { turtle.dig(Optional.empty()).await() }
        thenExecute {
            helper.assertBlockIs(BlockPos(2, 2, 3)) {
                it.block == ModRegistry.Blocks.CABLE.get() && !it.getValue(CableBlock.CABLE) && it.getValue(CableBlock.MODEM) == CableModemVariant.DownOff
            }

            helper.assertContainerExactly(BlockPos(2, 2, 2), listOf(ItemStack(ModRegistry.Items.CABLE.get())))
        }
        thenOnComputer { turtle.dig(Optional.empty()).await().assertArrayEquals(true) }
        thenExecute {
            helper.assertBlockPresent(Blocks.AIR, BlockPos(2, 2, 3))

            helper.assertContainerExactly(
                BlockPos(2, 2, 2),
                listOf(
                    ItemStack(ModRegistry.Items.CABLE.get()),
                    ItemStack(ModRegistry.Items.WIRED_MODEM.get()),
                ),
            )
        }
    }

    /**
     * Digging using a pickaxe with `{"consumesDurability": "always"}`, consumes durability.
     */
    @GameTest
    fun Dig_consume_durability(helper: GameTestHelper) = helper.sequence {
        thenOnComputer { turtle.dig(Optional.empty()).await() }
        thenExecute {
            helper.assertBlockPresent(Blocks.AIR, BlockPos(2, 2, 3))
            helper.assertContainerExactly(BlockPos(2, 2, 2), listOf(ItemStack(Items.COBBLESTONE)))

            val turtle = helper.getBlockEntity(BlockPos(2, 2, 2), ModRegistry.BlockEntities.TURTLE_NORMAL.get()).access
            val upgrade = turtle.getUpgrade(TurtleSide.LEFT)
            assertEquals(TurtleUpgrades.instance().get("cctest:wooden_pickaxe"), upgrade, "Upgrade is a wooden pickaxe")

            val item = ItemStack(Items.WOODEN_PICKAXE)
            item.damageValue = 1
            helper.assertUpgradeItem(item, turtle.getUpgradeWithData(TurtleSide.LEFT)!!)
        }
    }

    /**
     * Digging using a pickaxe with `{"consumesDurability": "always"}` and no durability removes the tool.
     */
    @GameTest
    fun Dig_breaks_tool(helper: GameTestHelper) = helper.sequence {
        thenOnComputer { turtle.dig(Optional.empty()).await() }
        thenExecute {
            helper.assertBlockPresent(Blocks.AIR, BlockPos(2, 2, 3))
            helper.assertContainerExactly(BlockPos(2, 2, 2), listOf(ItemStack(Items.COBBLESTONE)))

            val turtle = helper.getBlockEntity(BlockPos(2, 2, 2), ModRegistry.BlockEntities.TURTLE_NORMAL.get()).access
            val upgrade = turtle.getUpgrade(TurtleSide.LEFT)
            assertEquals(null, upgrade, "Upgrade broke")

            helper.assertUpgradeItem(
                ItemStack(Items.WOODEN_PICKAXE),
                UpgradeData.ofDefault(TurtleUpgrades.instance().get("cctest:wooden_pickaxe")),
            )
        }
    }

    /**
     * Digging using a silk-touch enchanted pickaxe with `{"consumesDurability": "when_enchanted"}`, consumes durability
     * uses silk touch.
     */
    @GameTest
    fun Dig_enchanted_consume_durability(helper: GameTestHelper) = helper.sequence {
        thenOnComputer { turtle.dig(Optional.empty()).await() }
        thenExecute {
            helper.assertBlockPresent(Blocks.AIR, BlockPos(2, 2, 3))
            helper.assertContainerExactly(BlockPos(2, 2, 2), listOf(ItemStack(Items.STONE)))

            val turtle = helper.getBlockEntity(BlockPos(2, 2, 2), ModRegistry.BlockEntities.TURTLE_NORMAL.get()).access
            val upgrade = turtle.getUpgrade(TurtleSide.LEFT)
            assertEquals(
                TurtleUpgrades.instance().get("cctest:netherite_pickaxe"),
                upgrade,
                "Upgrade is a netherite pickaxe",
            )

            val item = ItemStack(Items.NETHERITE_PICKAXE)
            item.damageValue = 1
            item.enchant(Enchantments.SILK_TOUCH, 1)
            item.setRepairCost(1)

            helper.assertUpgradeItem(item, turtle.getUpgradeWithData(TurtleSide.LEFT)!!)
        }
    }

    private fun GameTestHelper.assertUpgradeItem(expected: ItemStack, upgrade: UpgradeData<ITurtleUpgrade>) {
        if (!ItemStack.matches(expected, upgrade.upgradeItem)) {
            fail("Invalid upgrade item\n Expected => ${expected.tag}\n    Actual => ${upgrade.upgradeItem.tag}")
        }

        if (!ItemStack.matches(ItemStack(expected.item), upgrade.upgrade.craftingItem)) {
            fail("Original upgrade item has changed (is now ${upgrade.upgrade.craftingItem})")
        }
    }

    /**
     * Checks turtles can place monitors
     *
     * @see [#691](https://github.com/cc-tweaked/CC-Tweaked/issues/691)
     */
    @GameTest
    fun Place_monitor(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            turtle.place(ObjectArguments()).await()
                .assertArrayEquals(true, message = "Block was placed")
        }
        thenIdle(1)
        thenExecute { helper.assertBlockHas(BlockPos(1, 2, 3), MonitorBlock.STATE, MonitorEdgeState.LR) }
    }

    /**
     * Checks turtles can place into compostors. These are non-typical inventories, so
     * worth testing.
     */
    @GameTest
    fun Use_compostors(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            turtle.dropDown(Optional.empty()).await()
                .assertArrayEquals(true, message = "Item was dropped")
            assertEquals(63, turtle.getItemCount(Optional.of(1)), "Only dropped one item")
        }
    }

    /**
     * Checks turtles can be cleaned in cauldrons.
     */
    @GameTest
    fun Cleaned_with_cauldrons(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            val details = getTurtleItemDetail(1, true)
            turtle.place(ObjectArguments()).await()
                .assertArrayEquals(true, message = "Used item on cauldron")
            val newDetails = getTurtleItemDetail(1, true)

            assertEquals("computercraft:turtle_normal", newDetails["name"], "Still a turtle")
            assertNotEquals(details["nbt"], newDetails["nbt"], "Colour should have changed")
        }
    }

    /**
     * Checks turtles can use IDetailProviders by getting details for a printed page.
     */
    @GameTest
    fun Item_detail_provider(helper: GameTestHelper) = helper.sequence {
        // Register a dummy provider for printout items
        thenExecute {
            VanillaDetailRegistries.ITEM_STACK.addProvider(
                object :
                    BasicItemDetailProvider<PrintoutItem>("printout", PrintoutItem::class.java) {
                    override fun provideDetails(data: MutableMap<in String, Any>, stack: ItemStack, item: PrintoutItem) {
                        data["type"] = item.type.toString().lowercase()
                    }
                },
            )
        }
        thenOnComputer {
            val details = getTurtleItemDetail(detailed = true)
            assertEquals(mapOf("type" to "page"), details["printout"]) {
                "Printout information is returned (whole map is $details)"
            }
        }
    }

    /**
     * Advanced turtles resist all explosions but normal ones don't.
     */
    @GameTest
    fun Resists_explosions(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            val pos = helper.absolutePos(BlockPos(2, 2, 2))
            val tnt = PrimedTnt(helper.level, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5, null)
            tnt.fuse = 1
            helper.level.addFreshEntity(tnt)
        }
        thenWaitUntil { helper.assertEntityNotPresent(EntityType.TNT) }
        thenExecute {
            helper.assertBlockPresent(ModRegistry.Blocks.TURTLE_ADVANCED.get(), BlockPos(2, 2, 2))
            helper.assertBlockPresent(Blocks.AIR, BlockPos(2, 2, 1))
        }
    }

    /**
     * Turtles resist mob explosions
     */
    @GameTest
    fun Resists_entity_explosions(helper: GameTestHelper) = helper.sequence {
        thenExecute { helper.getEntity(EntityType.CREEPER).ignite() }
        thenWaitUntil { helper.assertEntityNotPresent(EntityType.CREEPER) }
        thenExecute {
            helper.assertBlockPresent(ModRegistry.Blocks.TURTLE_ADVANCED.get(), BlockPos(2, 2, 2))
            helper.assertBlockPresent(ModRegistry.Blocks.TURTLE_NORMAL.get(), BlockPos(2, 2, 1))
        }
    }

    /**
     * Test calling `turtle.drop` into an inventory.
     */
    @GameTest
    fun Drop_into_chest(helper: GameTestHelper) = helper.sequence {
        val turtlePos = BlockPos(2, 2, 2)
        val chest = BlockPos(2, 2, 3)

        thenOnComputer {
            turtle.drop(Optional.of(32)).await()
                .assertArrayEquals(true, message = "Could not drop items")
        }
        thenExecute {
            helper.assertContainerExactly(turtlePos, listOf(ItemStack(Blocks.DIRT, 32), ItemStack.EMPTY, ItemStack(Blocks.DIRT, 32)))
            helper.assertContainerExactly(chest, listOf(ItemStack(Blocks.DIRT, 48)))
        }
    }

    /**
     * Test calling `turtle.drop` into an entity with an inventory
     */
    @GameTest
    fun Drop_into_entity(helper: GameTestHelper) = helper.sequence {
        // When running /test runthis, the previous items pop from the chest. Remove them first!
        thenExecute { for (it in helper.getEntities(EntityType.ITEM)) it.discard() }

        thenOnComputer {
            turtle.drop(Optional.of(32)).await()
                .assertArrayEquals(true, message = "Could not drop items")
        }
        thenExecute {
            helper.assertContainerExactly(BlockPos(2, 2, 2), listOf(ItemStack(Blocks.DIRT, 32)))
            helper.assertContainerExactly(helper.getEntity(EntityType.CHEST_MINECART), listOf(ItemStack(Blocks.DIRT, 48)))
            helper.assertEntityNotPresent(EntityType.ITEM)
        }
    }

    /**
     * Test calling `turtle.refuel` on solid fuels (coal, blaze powder)
     */
    @GameTest
    fun Refuel_basic(helper: GameTestHelper) = helper.sequence {
        val turtlePos = BlockPos(2, 2, 2)

        // Test refueling from slot 1 with no limit.
        thenOnComputer {
            assertEquals(0, turtle.fuelLevel)
            turtle.refuel(Optional.empty()).await().assertArrayEquals(true)
            assertEquals(160, turtle.fuelLevel)
        }
        thenExecute {
            helper.assertContainerExactly(turtlePos, listOf(ItemStack.EMPTY, ItemStack(Items.BLAZE_ROD, 2)))
        }

        // Test refueling from slot 2 with a limit.
        thenOnComputer {
            turtle.select(2)
            turtle.refuel(Optional.of(1)).await().assertArrayEquals(true)
            assertEquals(280, turtle.fuelLevel)
        }
        thenExecute {
            helper.assertContainerExactly(turtlePos, listOf(ItemStack.EMPTY, ItemStack(Items.BLAZE_ROD, 1)))
        }
    }

    /**
     * Test calling `turtle.refuel` on non-fuels
     */
    @GameTest
    fun Refuel_fail(helper: GameTestHelper) = helper.sequence {
        val turtlePos = BlockPos(2, 2, 2)

        thenOnComputer {
            assertEquals(0, turtle.fuelLevel)
            turtle.refuel(Optional.empty()).await().assertArrayEquals(false, "Items not combustible")
            assertEquals(0, turtle.fuelLevel)
        }
        thenExecute {
            helper.assertContainerExactly(turtlePos, listOf(ItemStack(Items.DIRT, 32)))
        }
    }

    /**
     * Test calling `turtle.refuel` with a bucket of lava
     */
    @GameTest
    fun Refuel_container(helper: GameTestHelper) = helper.sequence {
        val turtlePos = BlockPos(2, 2, 2)

        // Test refueling from slot 1 with no limit.
        thenOnComputer {
            assertEquals(0, turtle.fuelLevel)
            turtle.refuel(Optional.empty()).await().assertArrayEquals(true)
            assertEquals(1000, turtle.fuelLevel)
        }
        thenExecute {
            helper.assertContainerExactly(turtlePos, listOf(ItemStack(Items.BUCKET)))
        }
    }

    /**
     * Test moving a turtle forwards preserves the turtle's inventory.
     *
     * @see [#1276](https://github.com/cc-tweaked/CC-Tweaked/pull/1276)
     */
    @GameTest
    fun Move_preserves_state(helper: GameTestHelper) = helper.sequence {
        thenOnComputer { turtle.forward().await().assertArrayEquals(true, message = "Turtle moved forward") }
        thenExecute {
            helper.assertContainerExactly(BlockPos(2, 2, 3), listOf(ItemStack(Items.DIRT, 32)))

            val turtle = helper.getBlockEntity(BlockPos(2, 2, 3), ModRegistry.BlockEntities.TURTLE_NORMAL.get())
            assertEquals(1, turtle.computerID)
            assertEquals("turtle_test.move_preserves_state", turtle.label)
            assertEquals(79, turtle.access.fuelLevel)

            helper.assertEntityNotPresent(EntityType.ITEM)
        }
    }

    /**
     * Test turtles are not obstructed by plants and instead replace them.
     */
    @GameTest
    fun Move_replace(helper: GameTestHelper) = helper.sequence {
        thenOnComputer { turtle.forward().await().assertArrayEquals(true, message = "Turtle moved forward") }
        thenExecute { helper.assertBlockPresent(ModRegistry.Blocks.TURTLE_NORMAL.get(), BlockPos(2, 2, 3)) }
    }

    /**
     * Test turtles become waterlogged when moving through liquid.
     */
    @GameTest
    fun Move_water(helper: GameTestHelper) = helper.sequence {
        thenOnComputer { turtle.forward().await().assertArrayEquals(true, message = "Turtle moved forward") }
        thenExecute {
            // Assert we're waterlogged.
            helper.assertBlockHas(BlockPos(2, 2, 2), WaterloggableHelpers.WATERLOGGED, true)
        }
        thenOnComputer { turtle.forward().await().assertArrayEquals(true, message = "Turtle moved forward") }
        thenExecute {
            // Assert we're no longer waterlogged and we've left a source block.
            helper.assertBlockIs(BlockPos(2, 2, 2)) { it.block == Blocks.WATER && it.fluidState.isSource }
            helper.assertBlockHas(BlockPos(2, 2, 3), WaterloggableHelpers.WATERLOGGED, false)
        }
    }

    /**
     * Test turtles can't move through solid blocks.
     */
    @GameTest
    fun Move_obstruct(helper: GameTestHelper) = helper.sequence {
        thenOnComputer { turtle.forward().await().assertArrayEquals(false, "Movement obstructed") }
        thenExecute {
            helper.assertBlockPresent(ModRegistry.Blocks.TURTLE_NORMAL.get(), BlockPos(2, 2, 2))
            helper.assertBlockPresent(Blocks.DIRT, BlockPos(2, 2, 3))
        }
    }

    /**
     * Test a turtle can attack an entity and capture its drops.
     */
    @GameTest
    fun Attack_entity(helper: GameTestHelper) = helper.sequence {
        val turtlePos = BlockPos(2, 2, 2)
        thenOnComputer {
            turtle.attack(Optional.empty()).await().assertArrayEquals(true, message = "Attacked entity")
        }
        thenExecute {
            helper.assertEntityNotPresent(EntityType.SHEEP)
            val count = helper.getBlockEntity(turtlePos, ModRegistry.BlockEntities.TURTLE_NORMAL.get())
                .countItem(Items.WHITE_WOOL)
            if (count == 0) helper.fail("Expected turtle to have white wool", turtlePos)
        }
    }

    /**
     * Test a turtle can be destroyed while performing an action.
     *
     * @see [#585](https://github.com/cc-tweaked/CC-Tweaked/issues/585)
     */
    @GameTest
    fun Attack_entity_destroy(helper: GameTestHelper) = helper.sequence {
        thenStartComputer { turtle.attack(Optional.empty()) }
        thenWaitUntil { helper.assertBlockPresent(Blocks.AIR, BlockPos(2, 2, 2)) }
    }

    /**
     * Ensure a turtle never sees itself as a peripheral.
     *
     * @see <https://github.com/dan200/ComputerCraft/issues/131>
     */
    @GameTest
    fun No_ghost_peripheral(helper: GameTestHelper) = helper.sequence {
        val events = mutableListOf<String>()
        thenOnComputer {
            for (i in 0 until 3) {
                if ((i % 2) == 0) turtle.up() else turtle.down()

                do {
                    val event = pullEvent()[0] as String
                    events.add(event)
                } while (event != "turtle_response")
            }
        }
    }

    /**
     * Ensure a turtle attaches and detaches peripherals as it moves.
     */
    @GameTest
    fun Peripheral_change(helper: GameTestHelper) = helper.sequence {
        val testInfo = (helper as GameTestHelperAccessor).testInfo as GameTestInfoAccessor

        val events = CopyOnWriteArrayList<Pair<String, String>>()
        var running = false
        thenStartComputer("listen") {
            running = true
            while (true) {
                val event = pullEvent()
                TestHooks.LOG.info("[{}] Got event {} at tick {}", testInfo, event[0], testInfo.`computercraft$getTick`())
                if (event[0] == "peripheral" || event[0] == "peripheral_detach") {
                    events.add((event[0] as String) to (event[1] as String))
                }
            }
        }
        thenOnComputer("turtle") {
            while (!running) sleep(10.milliseconds)

            turtle.forward().await().assertArrayEquals(true, message = "Moved turtle forward")
            turtle.back().await().assertArrayEquals(true, message = "Moved turtle forward")
            TestHooks.LOG.info("[{}] Finished turtle at {}", testInfo, testInfo.`computercraft$getTick`())
        }
        thenWaitUntil {
            val expected = listOf(
                "peripheral_detach" to "right",
                "peripheral" to "right",
            )

            if (events != expected) helper.fail("Expected $expected, but received $events")
        }
    }

    /**
     * `turtle.suck` only pulls for the current side.
     */
    @GameTest
    fun Sided_suck(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            turtle.suckUp(Optional.empty()).await().assertArrayEquals(true)
            turtle.getItemDetail(context, Optional.empty(), Optional.empty()).await().assertArrayEquals(
                mapOf("name" to "minecraft:iron_ingot", "count" to 8),
            )

            turtle.suckUp(Optional.empty()).await().assertArrayEquals(false, "No items to take")
        }
    }

    /**
     * `turtle.craft` works as expected
     */
    @GameTest
    fun Craft(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            callPeripheral("left", "craft", 1).assertArrayEquals(true)
        }
        thenExecute {
            val turtle = helper.getBlockEntity(BlockPos(2, 2, 2), ModRegistry.BlockEntities.TURTLE_NORMAL.get())
            assertThat(
                "Inventory is as expected.",
                turtle.contents,
                contains(
                    isStack(Items.DIAMOND, 1), isStack(Items.DIAMOND, 1), isStack(Items.DIAMOND, 1), isStack(Items.DIAMOND_PICKAXE, 1),
                    isStack(ItemStack.EMPTY), isStack(Items.STICK, 1), isStack(ItemStack.EMPTY), isStack(ItemStack.EMPTY),
                    isStack(ItemStack.EMPTY), isStack(Items.STICK, 1), isStack(ItemStack.EMPTY), isStack(ItemStack.EMPTY),
                    isStack(ItemStack.EMPTY), isStack(ItemStack.EMPTY), isStack(ItemStack.EMPTY), isStack(ItemStack.EMPTY),
                ),
            )
        }
    }

    /**
     * `turtle.equipLeft` equips a tool.
     */
    @GameTest
    fun Equip_tool(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            turtle.equipLeft().await().assertArrayEquals(true)
        }
        thenExecute {
            val turtle = helper.getBlockEntity(BlockPos(2, 2, 2), ModRegistry.BlockEntities.TURTLE_NORMAL.get())
            assertEquals(TurtleUpgrades.instance().get("minecraft:diamond_pickaxe"), turtle.getUpgrade(TurtleSide.LEFT))
        }
    }

    /**
     * Tests a turtle can break a block that explodes, causing the turtle itself to explode.
     *
     * @see [#585](https://github.com/cc-tweaked/CC-Tweaked/issues/585).
     */
    @GameTest
    fun Breaks_exploding_block(context: GameTestHelper) = context.sequence {
        thenOnComputer { turtle.dig(Optional.empty()) }
        thenIdle(2)
        thenExecute {
            context.assertItemEntityCountIs(ModRegistry.Items.TURTLE_NORMAL.get(), BlockPos(2, 2, 2), 1.0, 1)
            context.assertItemEntityCountIs(Items.BONE_BLOCK, BlockPos(2, 2, 2), 1.0, 65)
        }
    }

    /**
     * Render turtles as an item.
     */
    @ClientGameTest
    fun Render_turtle_items(helper: GameTestHelper) = helper.sequence {
        thenExecute { helper.positionAtArmorStand() }
        thenScreenshot()
    }

    /**
     * Render turtles as a block entity.
     */
    @ClientGameTest
    fun Render_turtle_blocks(helper: GameTestHelper) = helper.sequence {
        thenExecute { helper.positionAtArmorStand() }
        thenScreenshot()
    }
}

private val LuaTaskContext.turtle get() = getApi<TurtleAPI>()

private suspend fun LuaTaskContext.getTurtleItemDetail(slot: Int = 1, detailed: Boolean = false): Map<String, *> {
    val item = turtle.getItemDetail(context, Optional.of(slot), Optional.of(detailed)).await()
    assertThat("Returns details", item, array(instanceOf(Map::class.java)))

    @Suppress("UNCHECKED_CAST")
    return item!![0] as Map<String, *>
}

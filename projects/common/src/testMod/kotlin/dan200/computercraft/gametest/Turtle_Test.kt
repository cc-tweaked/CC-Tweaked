/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest

import dan200.computercraft.api.detail.BasicItemDetailProvider
import dan200.computercraft.api.detail.VanillaDetailRegistries
import dan200.computercraft.api.lua.ObjectArguments
import dan200.computercraft.core.apis.PeripheralAPI
import dan200.computercraft.gametest.api.*
import dan200.computercraft.gametest.core.TestHooks
import dan200.computercraft.mixin.gametest.GameTestHelperAccessor
import dan200.computercraft.mixin.gametest.GameTestInfoAccessor
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.media.items.PrintoutItem
import dan200.computercraft.shared.peripheral.monitor.MonitorBlock
import dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState
import dan200.computercraft.shared.turtle.apis.TurtleAPI
import dan200.computercraft.shared.util.WaterloggableHelpers
import dan200.computercraft.test.core.assertArrayEquals
import dan200.computercraft.test.core.computer.LuaTaskContext
import dan200.computercraft.test.core.computer.getApi
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FenceBlock
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import java.util.*
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
            helper.assertBlockIs(BlockPos(2, 2, 2), { it.block == Blocks.OAK_FENCE && it.getValue(FenceBlock.WATERLOGGED) })
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
        thenOnComputer {
            turtle.dig(Optional.empty()).await()
                .assertArrayEquals(true, message = "Dug with hoe")
        }
        thenExecute { helper.assertBlockPresent(Blocks.FARMLAND, BlockPos(1, 2, 1)) }
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
     *
     * Currently not required as turtles can no longer right-click cauldrons.
     */
    @GameTest(required = false)
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
            helper.assertBlockIs(BlockPos(2, 2, 2), { it.block == Blocks.WATER && it.fluidState.isSource })
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

        val events = mutableListOf<Pair<String, String>>()
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
        thenIdle(4) // Should happen immediately, but computers might be slow.
        thenExecute {
            assertEquals(
                listOf(
                    "peripheral_detach" to "right",
                    "peripheral" to "right",
                ),
                events,
            )
        }
    }

    // TODO: Turtle sucking from items
}

private val LuaTaskContext.turtle get() = getApi<TurtleAPI>()

private suspend fun LuaTaskContext.getTurtleItemDetail(slot: Int = 1, detailed: Boolean = false): Map<String, *> {
    val item = turtle.getItemDetail(context, Optional.of(slot), Optional.of(detailed)).await()
    assertThat("Returns details", item, array(instanceOf(Map::class.java)))

    @Suppress("UNCHECKED_CAST")
    return item!![0] as Map<String, *>
}

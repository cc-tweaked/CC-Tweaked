/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest

import dan200.computercraft.api.detail.BasicItemDetailProvider
import dan200.computercraft.api.detail.DetailRegistries
import dan200.computercraft.api.lua.ObjectArguments
import dan200.computercraft.core.apis.PeripheralAPI
import dan200.computercraft.gametest.api.*
import dan200.computercraft.shared.Registry
import dan200.computercraft.shared.media.items.ItemPrintout
import dan200.computercraft.shared.peripheral.monitor.BlockMonitor
import dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState
import dan200.computercraft.shared.turtle.apis.TurtleAPI
import dan200.computercraft.test.core.assertArrayEquals
import dan200.computercraft.test.core.computer.LuaTaskContext
import dan200.computercraft.test.core.computer.getApi
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FenceBlock
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.array
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import java.util.*

@GameTestHolder
class Turtle_Test {
    @GameTest
    fun Unequip_refreshes_peripheral(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            getApi<PeripheralAPI>().getType("right").assertArrayEquals("modem", message = "Starts with a modem")
            getApi<TurtleAPI>().equipRight().await()
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
            getApi<TurtleAPI>().placeDown(ObjectArguments()).await()
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
            getApi<TurtleAPI>().placeDown(ObjectArguments()).await()
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
            getApi<TurtleAPI>().place(ObjectArguments()).await()
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
            getApi<TurtleAPI>().placeDown(ObjectArguments()).await()
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
            getApi<TurtleAPI>().dig(Optional.empty()).await()
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
            getApi<TurtleAPI>().place(ObjectArguments()).await()
                .assertArrayEquals(true, message = "Block was placed")
        }
        thenIdle(1)
        thenExecute { helper.assertBlockHas(BlockPos(1, 2, 3), BlockMonitor.STATE, MonitorEdgeState.LR) }
    }

    /**
     * Checks turtles can place into compostors. These are non-typical inventories, so
     * worth testing.
     */
    @GameTest
    fun Use_compostors(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            getApi<TurtleAPI>().dropDown(Optional.empty()).await()
                .assertArrayEquals(true, message = "Item was dropped")
            assertEquals(63, getApi<TurtleAPI>().getItemCount(Optional.of(1)), "Only dropped one item")
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
            getApi<TurtleAPI>().place(ObjectArguments()).await()
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
            DetailRegistries.ITEM_STACK.addProvider(
                object :
                    BasicItemDetailProvider<ItemPrintout>("printout", ItemPrintout::class.java) {
                    override fun provideDetails(data: MutableMap<in String, Any>, stack: ItemStack, item: ItemPrintout) {
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
            helper.assertBlockPresent(Registry.ModBlocks.TURTLE_ADVANCED.get(), BlockPos(2, 2, 2))
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
            helper.assertBlockPresent(Registry.ModBlocks.TURTLE_ADVANCED.get(), BlockPos(2, 2, 2))
            helper.assertBlockPresent(Registry.ModBlocks.TURTLE_NORMAL.get(), BlockPos(2, 2, 1))
        }
    }

    /**
     * Test calling `turtle.drop` into an inventory.
     */
    @GameTest
    fun Drop_to_chest(helper: GameTestHelper) = helper.sequence {
        val turtle = BlockPos(2, 2, 2)
        val chest = BlockPos(2, 2, 3)

        thenOnComputer {
            getApi<TurtleAPI>().drop(Optional.of(32)).await()
                .assertArrayEquals(true, message = "Could not drop items")
        }
        thenExecute {
            helper.assertContainerExactly(turtle, listOf(ItemStack(Blocks.DIRT, 32), ItemStack.EMPTY, ItemStack(Blocks.DIRT, 32)))
            helper.assertContainerExactly(chest, listOf(ItemStack(Blocks.DIRT, 48)))
        }
    }

    // TODO: Ghost peripherals?
    // TODO: Dropping into minecarts
    // TODO: Turtle sucking from items
}

private suspend fun LuaTaskContext.getTurtleItemDetail(slot: Int = 1, detailed: Boolean = false): Map<String, *> {
    val item = getApi<TurtleAPI>().getItemDetail(context, Optional.of(slot), Optional.of(detailed)).await()
    assertThat("Returns details", item, array(instanceOf(Map::class.java)))

    @Suppress("UNCHECKED_CAST")
    return item!![0] as Map<String, *>
}

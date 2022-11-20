package dan200.computercraft.gametest

import dan200.computercraft.api.lua.ObjectArguments
import dan200.computercraft.core.apis.PeripheralAPI
import dan200.computercraft.gametest.api.assertContainerExactly
import dan200.computercraft.gametest.api.sequence
import dan200.computercraft.gametest.api.thenOnComputer
import dan200.computercraft.test.core.assertArrayEquals
import dan200.computercraft.test.core.computer.getApi
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class Inventory_Test {
    /**
     * Ensures inventory methods check an item is valid before moving it.
     *
     * @see <https://github.com/cc-tweaked/cc-restitched/issues/121>
     */
    @GameTest(required = false)
    fun Checks_valid_item(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            getApi<PeripheralAPI>().call(
                context,
                ObjectArguments(
                    "back",
                    "callRemote",
                    "minecraft:barrel_0",
                    "pushItems",
                    "minecraft:shulker_box_0",
                    1,
                ),
            ).await().assertArrayEquals(0, message = "Does not move items")
        }
        thenExecute {
            helper.assertContainerExactly(BlockPos(1, 2, 2), listOf())
            helper.assertContainerExactly(BlockPos(3, 2, 2), listOf(ItemStack(Items.SHULKER_BOX)))
        }
    }

    /**
     * Ensures inventory methods check an item is valid before moving it.
     *
     * @see <https://github.com/cc-tweaked/cc-restitched/issues/121>
     */
    @GameTest
    fun Fails_on_full(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            getApi<PeripheralAPI>().call(
                context,
                ObjectArguments(
                    "back",
                    "callRemote",
                    "minecraft:furnace_0",
                    "pushItems",
                    "minecraft:barrel_0",
                    1,
                ),
            ).await().assertArrayEquals(0, message = "Does not move items")
        }
        thenExecute {
            helper.assertContainerExactly(BlockPos(1, 2, 2), listOf(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack(Items.IRON_INGOT)))
            helper.assertContainerExactly(BlockPos(3, 2, 2), NonNullList.withSize(27, ItemStack(Items.POLISHED_ANDESITE)))
        }
    }
}

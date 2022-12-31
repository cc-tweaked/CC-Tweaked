package dan200.computercraft.gametest

import dan200.computercraft.api.detail.VanillaDetailRegistries
import dan200.computercraft.gametest.api.Structures
import dan200.computercraft.gametest.api.sequence
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.assertEquals

class Details_Test {
    @GameTest(template = Structures.DEFAULT)
    fun Has_item_groups(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            val details = VanillaDetailRegistries.ITEM_STACK.getDetails(ItemStack(Items.DIRT))
            assertEquals(
                listOf(
                    mapOf(
                        "displayName" to "Natural Blocks",
                        "id" to "minecraft:natural",
                    ),
                ),
                details["itemGroups"],
            )
        }
    }
}

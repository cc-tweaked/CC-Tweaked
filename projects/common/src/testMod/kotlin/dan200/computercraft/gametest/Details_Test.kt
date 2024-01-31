// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.api.detail.VanillaDetailRegistries
import dan200.computercraft.gametest.api.Structures
import dan200.computercraft.gametest.api.sequence
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Test our detail providers ([VanillaDetailRegistries]).
 */
class Details_Test {
    /**
     * Assert that items have their creative tabs availab.e
     */
    @GameTest(template = Structures.DEFAULT)
    fun Has_item_groups(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            val details = VanillaDetailRegistries.ITEM_STACK.getDetails(ItemStack(Items.DIRT))
            assertEquals(
                listOf(
                    mapOf(
                        "displayName" to "Natural Blocks",
                        "id" to "minecraft:natural_blocks",
                    ),
                ),
                details["itemGroups"],
            )
        }
    }
}

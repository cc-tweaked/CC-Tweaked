// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.core.util.Colour
import dan200.computercraft.gametest.api.GameTest
import dan200.computercraft.gametest.api.craftItem
import dan200.computercraft.gametest.api.immediate
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.util.DataComponentUtil
import dan200.computercraft.test.shared.ItemStackMatcher.isStack
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.hamcrest.MatcherAssert.assertThat

class Disk_Test {
    /**
     * Ensure disks
     */
    @GameTest(template = "default")
    fun Can_craft_disk(helper: GameTestHelper) = helper.immediate {
        assertThat(
            "Disk without dye",
            helper.craftItem(ItemStack(Items.REDSTONE), ItemStack(Items.PAPER)),
            isStack(DataComponentUtil.createDyedStack(ModRegistry.Items.DISK.get(), Colour.BLUE.hex)),
        )

        assertThat(
            "Disk with dye",
            helper.craftItem(ItemStack(Items.REDSTONE), ItemStack(Items.PAPER), ItemStack(Items.GREEN_DYE)),
            isStack(DataComponentUtil.createDyedStack(ModRegistry.Items.DISK.get(), Colour.GREEN.hex)),
        )
    }
}

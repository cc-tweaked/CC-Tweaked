// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import com.mojang.authlib.GameProfile
import dan200.computercraft.gametest.api.GameTest
import dan200.computercraft.gametest.api.Structures
import dan200.computercraft.gametest.api.craftItem
import dan200.computercraft.gametest.api.sequence
import dan200.computercraft.shared.ModRegistry
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*

class Recipe_Test {
    /**
     * Test that crafting results contain NBT data.
     *
     * Mostly useful for Fabric, where we need a mixin for this.
     */
    @GameTest(template = Structures.DEFAULT)
    fun Craft_result_has_nbt(context: GameTestHelper) = context.sequence {
        thenExecute {
            val result = context.craftItem(
                ItemStack(Items.SKELETON_SKULL),
                ItemStack(ModRegistry.Items.COMPUTER_ADVANCED.get()),
            )

            val profile = GameProfile(UUID.fromString("f3c8d69b-0776-4512-8434-d1b2165909eb"), "dan200")

            val tag =
                DataComponentPatch.builder().set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile))
                    .build()
            assertEquals(tag, result.componentsPatch, "Expected NBT tags to be the same")
        }
    }
}

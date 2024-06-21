// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import com.mojang.authlib.GameProfile
import dan200.computercraft.gametest.api.Structures
import dan200.computercraft.gametest.api.sequence
import dan200.computercraft.shared.ModRegistry
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType
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
            val items = NonNullList.withSize(3 * 3, ItemStack.EMPTY)
            items[0] = ItemStack(Items.SKELETON_SKULL)
            items[1] = ItemStack(ModRegistry.Items.COMPUTER_ADVANCED.get())
            val container = CraftingInput.of(3, 3, items)

            val recipe = context.level.server.recipeManager
                .getRecipeFor(RecipeType.CRAFTING, container, context.level)
                .orElseThrow { GameTestAssertException("No recipe matches") }

            val result = recipe.value.assemble(container, context.level.registryAccess())

            val profile = GameProfile(UUID.fromString("f3c8d69b-0776-4512-8434-d1b2165909eb"), "dan200")

            val tag = DataComponentPatch.builder().set(DataComponents.PROFILE, ResolvableProfile(profile)).build()
            assertEquals(tag, result.componentsPatch, "Expected NBT tags to be the same")
        }
    }
}

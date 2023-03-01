// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.Structures
import dan200.computercraft.gametest.api.sequence
import dan200.computercraft.shared.ModRegistry
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingRecipe
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
            val container = CraftingContainer(DummyMenu, 3, 3)
            container.setItem(0, ItemStack(Items.SKELETON_SKULL))
            container.setItem(1, ItemStack(ModRegistry.Items.COMPUTER_ADVANCED.get()))

            val recipe: Optional<CraftingRecipe> = context.level.server.recipeManager
                .getRecipeFor(RecipeType.CRAFTING, container, context.level)
            if (!recipe.isPresent) throw GameTestAssertException("No recipe matches")

            val result = recipe.get().assemble(container)

            val owner = CompoundTag()
            owner.putString("Name", "dan200")
            owner.putString("Id", "f3c8d69b-0776-4512-8434-d1b2165909eb")
            val tag = CompoundTag()
            tag.put("SkullOwner", owner)

            assertEquals(tag, result.tag, "Expected NBT tags to be the same")
        }
    }

    object DummyMenu : AbstractContainerMenu(MenuType.GENERIC_9x1, 0) {
        override fun quickMoveStack(player: Player, slot: Int): ItemStack = ItemStack.EMPTY
        override fun stillValid(p0: Player): Boolean = true
    }
}

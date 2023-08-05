// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration.emi;

import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.Comparison;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiPredicate;

@EmiEntrypoint
public class EMIComputerCraft implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.setDefaultComparison(ModRegistry.Items.TURTLE_NORMAL.get(), turtleComparison);
        registry.setDefaultComparison(ModRegistry.Items.TURTLE_ADVANCED.get(), turtleComparison);

        registry.setDefaultComparison(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get(), pocketComparison);
        registry.setDefaultComparison(ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get(), pocketComparison);
    }

    private static final Comparison turtleComparison = compareStacks((left, right) ->
        left.getItem() instanceof TurtleItem turtle
            && turtle.getUpgrade(left, TurtleSide.LEFT) == turtle.getUpgrade(right, TurtleSide.LEFT)
            && turtle.getUpgrade(left, TurtleSide.RIGHT) == turtle.getUpgrade(right, TurtleSide.RIGHT));

    private static final Comparison pocketComparison = compareStacks((left, right) ->
        left.getItem() instanceof PocketComputerItem && PocketComputerItem.getUpgrade(left) == PocketComputerItem.getUpgrade(right));

    private static Comparison compareStacks(BiPredicate<ItemStack, ItemStack> test) {
        return Comparison.of((left, right) -> {
            ItemStack leftStack = left.getItemStack(), rightStack = right.getItemStack();
            return leftStack.getItem() == rightStack.getItem() && test.test(leftStack, rightStack);
        });
    }
}

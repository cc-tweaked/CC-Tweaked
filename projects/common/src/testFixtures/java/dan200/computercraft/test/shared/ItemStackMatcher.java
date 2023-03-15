// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ItemStackMatcher extends TypeSafeMatcher<ItemStack> {
    private final ItemStack stack;

    public ItemStackMatcher(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    protected boolean matchesSafely(ItemStack item) {
        return ItemStack.isSameItemSameTags(item, stack) && item.getCount() == stack.getCount();
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(stack).appendValue(stack.getTag());
    }

    public static Matcher<ItemStack> isStack(ItemStack stack) {
        return new ItemStackMatcher(stack);
    }

    public static Matcher<ItemStack> isStack(Item item, int size) {
        return new ItemStackMatcher(new ItemStack(item, size));
    }
}

/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.support;

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
        description.appendValue(stack);
    }

    public static Matcher<ItemStack> isStack(ItemStack stack) {
        return new ItemStackMatcher(stack);
    }
}

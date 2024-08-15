// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.items;

import com.google.common.base.Strings;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.lectern.CustomLecternBlock;
import dan200.computercraft.shared.media.PrintoutMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;

import javax.annotation.Nullable;
import java.util.List;

public class PrintoutItem extends Item {
    private static final String NBT_TITLE = "Title";
    private static final String NBT_PAGES = "Pages";
    private static final String NBT_LINE_TEXT = "Text";
    private static final String NBT_LINE_COLOUR = "Color";

    public static final int LINES_PER_PAGE = 21;
    public static final int LINE_MAX_LENGTH = 25;
    public static final int MAX_PAGES = 16;

    public enum Type {
        PAGE,
        PAGES,
        BOOK
    }

    private final Type type;

    public PrintoutItem(Properties settings, Type type) {
        super(settings);
        this.type = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag options) {
        var title = getTitle(stack);
        if (title != null && !title.isEmpty()) list.add(Component.literal(title));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var blockPos = context.getClickedPos();
        var blockState = level.getBlockState(blockPos);
        if (blockState.is(Blocks.LECTERN) && !blockState.getValue(LecternBlock.HAS_BOOK)) {
            // If we have an empty lectern, place our book into it.
            if (!level.isClientSide) {
                CustomLecternBlock.replaceLectern(level, blockPos, blockState, context.getItemInHand());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            var title = getTitle(stack);
            var displayTitle = Strings.isNullOrEmpty(title) ? stack.getDisplayName() : Component.literal(title);
            player.openMenu(new SimpleMenuProvider((id, playerInventory, p) -> PrintoutMenu.createInHand(id, p, hand), displayTitle));
        }
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }

    private ItemStack createFromTitleAndText(@Nullable String title, @Nullable String[] text, @Nullable String[] colours) {
        var stack = new ItemStack(this);

        // Build NBT
        if (title != null) stack.getOrCreateTag().putString(NBT_TITLE, title);
        if (text != null) {
            var tag = stack.getOrCreateTag();
            tag.putInt(NBT_PAGES, text.length / LINES_PER_PAGE);
            for (var i = 0; i < text.length; i++) {
                if (text[i] != null) tag.putString(NBT_LINE_TEXT + i, text[i]);
            }
        }
        if (colours != null) {
            var tag = stack.getOrCreateTag();
            for (var i = 0; i < colours.length; i++) {
                if (colours[i] != null) tag.putString(NBT_LINE_COLOUR + i, colours[i]);
            }
        }


        return stack;
    }

    public static ItemStack createSingleFromTitleAndText(@Nullable String title, @Nullable String[] text, @Nullable String[] colours) {
        return ModRegistry.Items.PRINTED_PAGE.get().createFromTitleAndText(title, text, colours);
    }

    public static ItemStack createMultipleFromTitleAndText(@Nullable String title, @Nullable String[] text, @Nullable String[] colours) {
        return ModRegistry.Items.PRINTED_PAGES.get().createFromTitleAndText(title, text, colours);
    }

    public static ItemStack createBookFromTitleAndText(@Nullable String title, @Nullable String[] text, @Nullable String[] colours) {
        return ModRegistry.Items.PRINTED_BOOK.get().createFromTitleAndText(title, text, colours);
    }

    public Type getType() {
        return type;
    }

    public static String getTitle(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_TITLE) ? nbt.getString(NBT_TITLE) : "";
    }

    public static int getPageCount(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_PAGES) ? nbt.getInt(NBT_PAGES) : 1;
    }

    public static String[] getText(ItemStack stack) {
        return getLines(stack, NBT_LINE_TEXT);
    }

    public static String[] getColours(ItemStack stack) {
        return getLines(stack, NBT_LINE_COLOUR);
    }

    private static String[] getLines(ItemStack stack, String prefix) {
        var nbt = stack.getTag();
        var numLines = getPageCount(stack) * LINES_PER_PAGE;
        var lines = new String[numLines];
        for (var i = 0; i < lines.length; i++) {
            lines[i] = nbt != null ? nbt.getString(prefix + i) : "";
        }
        return lines;
    }
}

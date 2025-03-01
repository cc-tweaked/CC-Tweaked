// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.items;

import com.google.common.base.Strings;
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
import net.minecraft.world.level.Level;

import java.util.List;

public class PrintoutItem extends Item {
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag options) {
        var title = PrintoutData.getOrEmpty(stack).title();
        if (!title.isEmpty()) list.add(Component.literal(title));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            var title = PrintoutData.getOrEmpty(stack).title();
            var displayTitle = Strings.isNullOrEmpty(title) ? stack.getDisplayName() : Component.literal(title);
            player.openMenu(new SimpleMenuProvider((id, playerInventory, p) -> PrintoutMenu.createInHand(id, p, hand), displayTitle));
        }
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }

    public Type getType() {
        return type;
    }
}

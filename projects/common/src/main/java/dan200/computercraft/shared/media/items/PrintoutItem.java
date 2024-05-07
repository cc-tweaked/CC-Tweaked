// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.items;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.common.HeldItemMenu;
import dan200.computercraft.shared.network.container.HeldItemContainerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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
        if (!world.isClientSide) {
            new HeldItemContainerData(hand)
                .open(player, new HeldItemMenu.Factory(ModRegistry.Menus.PRINTOUT.get(), player.getItemInHand(hand), hand));
        }
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), player.getItemInHand(hand));
    }

    public Type getType() {
        return type;
    }
}

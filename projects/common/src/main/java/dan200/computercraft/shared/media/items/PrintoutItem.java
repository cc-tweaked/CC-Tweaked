// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.items;

import com.google.common.base.Strings;
import dan200.computercraft.shared.media.PrintoutMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;


public class PrintoutItem extends Item {
    public PrintoutItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            var title = PrintoutData.getOrEmpty(stack).title();
            var displayTitle = Strings.isNullOrEmpty(title) ? stack.getDisplayName() : Component.literal(title);
            player.openMenu(new SimpleMenuProvider((id, playerInventory, p) -> PrintoutMenu.createInHand(id, p, hand), displayTitle));
        }
        return InteractionResult.SUCCESS;
    }
}

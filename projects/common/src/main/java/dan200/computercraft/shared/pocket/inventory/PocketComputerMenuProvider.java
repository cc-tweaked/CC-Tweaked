// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.pocket.inventory;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

public class PocketComputerMenuProvider implements MenuProvider {
    private final ServerComputer computer;
    private final Component name;
    private final PocketComputerItem item;
    private final InteractionHand hand;
    private final boolean isTypingOnly;

    public PocketComputerMenuProvider(ServerComputer computer, ItemStack stack, PocketComputerItem item, InteractionHand hand, boolean isTypingOnly) {
        this.computer = computer;
        name = stack.getHoverName();
        this.item = item;
        this.hand = hand;
        this.isTypingOnly = isTypingOnly;
    }


    @Override
    public Component getDisplayName() {
        return name;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player entity) {
        return new ComputerMenuWithoutInventory(
            isTypingOnly ? ModRegistry.Menus.POCKET_COMPUTER_NO_TERM.get() : ModRegistry.Menus.COMPUTER.get(), id, inventory,
            p -> {
                var stack = p.getItemInHand(hand);
                return stack.getItem() == item && PocketComputerItem.getServerComputer(assertNonNull(entity.level().getServer()), stack) == computer;
            },
            computer
        );
    }
}

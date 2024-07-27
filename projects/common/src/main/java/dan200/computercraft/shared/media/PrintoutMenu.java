// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.media;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.container.InvisibleSlot;
import dan200.computercraft.shared.media.items.PrintoutItem;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * The menus for {@linkplain PrintoutItem printouts}.
 * <p>
 * This is a somewhat similar design to {@link LecternMenu}, which is used to read written books.
 * <p>
 * This holds a single slot (containing the printout), and a single data slot ({@linkplain #DATA_CURRENT_PAGE holding
 * the current page}). The page is set by the client by sending a {@linkplain #clickMenuButton(Player, int) button
 * press} with an index of {@link #PAGE_BUTTON_OFFSET} plus the current page.
 * <p>
 * The client-side screen uses {@linkplain ContainerListener container listeners} to subscribe to item and page changes.
 * However, listeners aren't fired on the client, so we copy {@link LecternMenu}'s hack and call
 * {@link #broadcastChanges()} whenever an item or data value are changed.
 */
public class PrintoutMenu extends AbstractContainerMenu {
    public static final int DATA_CURRENT_PAGE = 0;
    private static final int DATA_SIZE = 1;

    public static final int PAGE_BUTTON_OFFSET = 100;

    private final Predicate<Player> valid;
    private final ContainerData currentPage;

    public PrintoutMenu(
        int containerId, Container container, int slotIdx, Predicate<Player> valid, ContainerData currentPage
    ) {
        super(ModRegistry.Menus.PRINTOUT.get(), containerId);
        this.valid = valid;
        this.currentPage = currentPage;

        addSlot(new InvisibleSlot(container, slotIdx) {
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(container); // Trigger listeners on the client.
            }
        });
        addDataSlots(currentPage);
    }

    /**
     * Create {@link PrintoutMenu} for use a remote (client).
     *
     * @param containerId The current container id.
     * @return The constructed container.
     */
    public static PrintoutMenu createRemote(int containerId) {
        return new PrintoutMenu(containerId, new SimpleContainer(1), 0, p -> true, new SimpleContainerData(DATA_SIZE));
    }

    /**
     * Create a {@link PrintoutMenu} for the printout in the current player's hand.
     *
     * @param containerId The current container id.
     * @param player      The player to open the container.
     * @param hand        The hand containing the item.
     * @return The constructed container.
     */
    public static PrintoutMenu createInHand(int containerId, Player player, InteractionHand hand) {
        var currentStack = player.getItemInHand(hand);
        var currentItem = currentStack.getItem();

        var slot = switch (hand) {
            case MAIN_HAND -> player.getInventory().selected;
            case OFF_HAND -> Inventory.SLOT_OFFHAND;
        };
        return new PrintoutMenu(
            containerId, player.getInventory(), slot,
            p -> player.getItemInHand(hand).getItem() == currentItem, new SimpleContainerData(DATA_SIZE)
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return valid.test(player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= PAGE_BUTTON_OFFSET) {
            var page = Mth.clamp(id - PAGE_BUTTON_OFFSET, 0, PrintoutItem.getPageCount(getPrintout()) - 1);
            setData(DATA_CURRENT_PAGE, page);
            return true;
        }

        return super.clickMenuButton(player, id);
    }

    /**
     * Get the current printout.
     *
     * @return The current printout.
     */
    public ItemStack getPrintout() {
        return getSlot(0).getItem();
    }

    /**
     * Get the current page.
     *
     * @return The current page.
     */
    public int getPage() {
        return currentPage.get(DATA_CURRENT_PAGE);
    }

    @Override
    public void setData(int id, int data) {
        super.setData(id, data);
        broadcastChanges(); // Trigger listeners on the client.
    }
}

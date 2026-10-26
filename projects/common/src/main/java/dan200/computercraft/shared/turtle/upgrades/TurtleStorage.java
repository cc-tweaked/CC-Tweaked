// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.api.upgrades.UpgradeHandle;
import dan200.computercraft.api.upgrades.UpgradeType;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.peripheral.generic.methods.AbstractInventoryMethods;
import dan200.computercraft.shared.platform.ItemContainer;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Turtle storage upgrade.
 */
public final class TurtleStorage extends AbstractTurtleUpgrade.StackAware {
    public static final String ADJECTIVE = "upgrade.computercraft.storage.adjective";

    public static final String DATA_PACK_NAME = "turtle_storage";
    public static final String DATA_PACK_TRANSLATION = "dataPack." + DATA_PACK_NAME + ".description";

    public TurtleStorage(Ingredient item) {
        super(TurtleUpgradeType.PERIPHERAL, ADJECTIVE, item);
    }

    @Override
    public IPeripheral createPeripheral(ITurtleAccess turtle, TurtleSide side) {
        return new Peripheral(turtle, side);
    }

    @Override
    public UpgradeType<TurtleStorage> getType() {
        return ModRegistry.TurtleUpgradeTypes.STORAGE.get();
    }

    /**
     * A {@link Peripheral} allows you to equip a shulker or other storage items with a turtle and access its contents.
     * <p>
     * This has largely the same methods and shape as {@link AbstractInventoryMethods}, with the exception that
     * {@link #pullItems(Optional, Optional, Optional)} and {@link #pushItems(int, Optional, Optional)} transfer to/from
     * the turtle's inventory, rather than taking an inventory name.
     *
     * @cc.module turtle_storage
     * @cc.since 1.121.0
     */
    public static final class Peripheral implements IPeripheral {
        private final ITurtleAccess turtle;
        private final ItemContainer turtleInventory;
        private final TurtleSide side;
        private final InventoryMethods inventoryMethods;

        private Peripheral(ITurtleAccess turtle, TurtleSide side) {
            this.turtle = turtle;
            this.turtleInventory = PlatformHelper.get().wrapContainer(turtle.getInventory());
            this.side = side;
            this.inventoryMethods = new InventoryMethods(turtle.getLevel().registryAccess());
        }

        private UpgradeHandle<ITurtleUpgrade> getHandle() throws LuaException {
            var upgrade = turtle.getUpgradeHandle(side);
            if (upgrade == null || !(upgrade.getUpgrade() instanceof TurtleStorage)) {
                throw new LuaException("Upgrade is not equipped");
            }
            return upgrade;
        }

        private ItemStack getStack() throws LuaException {
            return getHandle().getItem().create();
        }

        private static ItemContainer getContainer(ItemStack stack) throws LuaException {
            var container = PlatformHelper.get().getContainer(stack);
            if (container == null) throw new LuaException("Upgrade is not a container");
            return container;
        }

        @Override
        public String getType() {
            return "turtle_storage";
        }

        /**
         * Get the size of this inventory.
         *
         * @return The number of slots in this inventory.
         */
        @LuaFunction(mainThread = true)
        public int size() throws LuaException {
            return inventoryMethods.size(getContainer(getStack()));
        }

        /**
         * List all items in this inventory. This returns a table, with an entry for each slot.
         * <p>
         * This returns data in the same format as {@link AbstractInventoryMethods#list(Object)}.
         *
         * @return Basic information about all items in this inventory.
         * @cc.treturn { (table|nil)... } Basic information about all items in this inventory.
         * @cc.see item_details
         * @see AbstractInventoryMethods#list(Object)
         */
        @LuaFunction(mainThread = true)
        public Map<Integer, Map<String, ?>> list() throws LuaException {
            return inventoryMethods.list(getContainer(getStack()));
        }

        /**
         * Get [detailed information][`item_details`] about an item.
         *
         * @param slot The slot to get information about.
         * @return Information about the item in this slot, or {@code nil} if it is empty.
         * @throws LuaException If the slot is out of range.
         * @cc.see item_details
         * @see AbstractInventoryMethods#getItemDetail(Object, int)
         */
        @Nullable
        @LuaFunction(mainThread = true)
        public Map<?, ?> getItemDetail(int slot) throws LuaException {
            return inventoryMethods.getItemDetail(getContainer(getStack()), slot);
        }

        /**
         * Push items from this inventory to the turtle.
         *
         * @param fromSlot The slot in the current inventory to move items to.
         * @param limit    The maximum number of items to move. Defaults to the current stack limit.
         * @param toSlot   The slot in the turtle to move to. If not given, the item will be inserted into any slot.
         * @return The number of transferred items.
         * @throws LuaException If either source or destination slot is out of range.
         */
        @LuaFunction(mainThread = true)
        public int pushItems(int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot) throws LuaException {
            var handle = getHandle();
            var stack = handle.getItem().create();
            return moveAndUpdate(handle, stack, getContainer(stack), fromSlot, turtleInventory, toSlot, limit);
        }

        /**
         * Pull items from the turtle into this inventory.
         *
         * @param fromSlot The slot in the turtle inventory to move items from, defaults to the currently selected slot.
         * @param limit    The maximum number of items to move. Defaults to the current stack limit.
         * @param toSlot   The slot in current inventory to move to. If not given, the item will be inserted into any slot.
         * @return The number of transferred items.
         * @throws LuaException If either source or destination slot is out of range.
         */
        @LuaFunction(mainThread = true)
        public int pullItems(Optional<Integer> fromSlot, Optional<Integer> limit, Optional<Integer> toSlot) throws LuaException {
            var handle = getHandle();
            var stack = handle.getItem().create();
            return moveAndUpdate(handle, stack, turtleInventory, fromSlot.orElse(turtle.getSelectedSlot() + 1), getContainer(stack), toSlot, limit);
        }

        private int moveAndUpdate(UpgradeHandle<ITurtleUpgrade> handle, ItemStack stack, ItemContainer from, int fromSlot, ItemContainer to, Optional<Integer> toSlot, Optional<Integer> limit) throws LuaException {
            var moved = AbstractInventoryMethods.move(from, fromSlot, to, toSlot, limit);
            if (moved > 0) handle.setItem(stack);
            return moved;
        }

        @Override
        public boolean equals(@Nullable IPeripheral other) {
            return other instanceof Peripheral o && turtle == o.turtle && side == o.side;
        }
    }

    private static final class InventoryMethods extends AbstractInventoryMethods<ItemContainer> {
        InventoryMethods(HolderLookup.Provider registries) {
            super(registries);
        }

        @Override
        protected ItemContainer getContainer(ItemContainer inventory) {
            return inventory;
        }

        @Override
        protected @Nullable ItemContainer getContainer(IPeripheral peripheral) {
            return null;
        }
    }
}

// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import com.mojang.datafixers.util.Unit;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

/**
 * {@link Storage} implementation for shulker boxes in item form.
 */
@SuppressWarnings("UnstableApiUsage")
final class ShulkerItemStorage extends BasicSlottedStorage {
    private final ItemStack stack;
    private final NonNullList<ItemStack> items;
    private final @Nullable SlotImpl[] slots;
    private final SnapshotParticipantImpl snapshotParticipant = new SnapshotParticipantImpl();

    ShulkerItemStorage(ItemStack stack, int size) {
        this.stack = stack;
        items = NonNullList.withSize(size, ItemStack.EMPTY);
        slots = new SlotImpl[size];

        var tag = BlockItem.getBlockEntityData(stack);
        if (tag != null && tag.contains("Items", Tag.TAG_LIST)) ContainerHelper.loadAllItems(tag, items);
    }

    @Override
    public int getSlotCount() {
        return items.size();
    }

    @Override
    public SingleSlotStorage<ItemVariant> getSlot(int slot) {
        var slotInfo = slots[slot];
        return slotInfo == null ? slots[slot] = new SlotImpl(slot) : slotInfo;
    }

    private final class SlotImpl extends SingleStackStorage {
        private final int slot;

        private SlotImpl(int slot) {
            this.slot = slot;
        }

        @Override
        protected ItemStack getStack() {
            return items.get(slot);
        }

        @Override
        protected void setStack(ItemStack stack) {
            items.set(slot, stack);
        }

        @Override
        protected boolean canInsert(ItemVariant itemVariant) {
            return itemVariant.getItem().canFitInsideContainerItems();
        }

        @Override
        public void updateSnapshots(TransactionContext transaction) {
            super.updateSnapshots(transaction);
            snapshotParticipant.updateSnapshots(transaction);
        }
    }

    private final class SnapshotParticipantImpl extends SnapshotParticipant<Unit> {
        @Override
        protected Unit createSnapshot() {
            return Unit.INSTANCE;
        }

        @Override
        protected void readSnapshot(Unit snapshot) {
        }

        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();

            var tag = BlockItem.getBlockEntityData(stack);
            if (tag == null) BlockItem.setBlockEntityData(stack, BlockEntityType.SHULKER_BOX, tag = new CompoundTag());
            ContainerHelper.saveAllItems(tag, items);
        }
    }
}

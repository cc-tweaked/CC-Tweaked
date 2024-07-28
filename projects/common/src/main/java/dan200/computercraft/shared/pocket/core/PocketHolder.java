// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.pocket.core;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

/**
 * An object that holds a pocket computer item.
 */
public sealed interface PocketHolder permits PocketHolder.EntityHolder {
    /**
     * The level this holder is in.
     *
     * @return The holder's level.
     */
    ServerLevel level();

    /**
     * The position of this holder.
     *
     * @return The position of this holder.
     */
    Vec3 pos();

    /**
     * The block position of this holder.
     *
     * @return The position of this holder.
     */
    BlockPos blockPos();

    /**
     * Determine if this holder is still valid for a particular computer.
     *
     * @param computer The current computer.
     * @return Whether this holder is valid.
     */
    boolean isValid(ServerComputer computer);

    /**
     * Mark the pocket computer item as having changed.
     */
    void setChanged();

    /**
     * An {@link Entity} holding a pocket computer.
     */
    sealed interface EntityHolder extends PocketHolder permits PocketHolder.PlayerHolder, PocketHolder.ItemEntityHolder {
        /**
         * Get the entity holding this pocket computer.
         *
         * @return The holding entity.
         */
        Entity entity();

        @Override
        default ServerLevel level() {
            return (ServerLevel) entity().level();
        }

        @Override
        default Vec3 pos() {
            return entity().getEyePosition();
        }

        @Override
        default BlockPos blockPos() {
            return entity().blockPosition();
        }
    }

    /**
     * A pocket computer in a player's slot.
     *
     * @param entity The current player.
     * @param slot   The slot the pocket computer is in.
     */
    record PlayerHolder(ServerPlayer entity, int slot) implements EntityHolder {
        @Override
        public boolean isValid(ServerComputer computer) {
            return entity().isAlive() && PocketComputerItem.isServerComputer(computer, entity().getInventory().getItem(this.slot()));
        }

        @Override
        public void setChanged() {
            entity.getInventory().setChanged();
        }
    }

    /**
     * A pocket computer in an {@link ItemEntity}.
     *
     * @param entity The item entity.
     */
    record ItemEntityHolder(ItemEntity entity) implements EntityHolder {
        @Override
        public boolean isValid(ServerComputer computer) {
            return entity().isAlive() && PocketComputerItem.isServerComputer(computer, this.entity().getItem());
        }

        @Override
        public void setChanged() {
            entity.setItem(entity.getItem().copy());
        }
    }
}

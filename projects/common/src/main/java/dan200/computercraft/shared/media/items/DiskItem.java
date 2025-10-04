// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.media.items;

import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;


/**
 * An item that can be shift-right-clicked into a {@link DiskDriveBlock}.
 */
public class DiskItem extends Item {
    public DiskItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return DiskDriveBlock.defaultUseItemOn(context);
    }
}

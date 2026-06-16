// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import dan200.computercraft.impl.client.ExtendedOrderedSubmitNodeCollector;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Monitor support for {@link OrderedSubmitNodeCollector}.
 */
@Mixin(OrderedSubmitNodeCollector.class)
interface OrderedSubmitNodeCollectorMixin extends ExtendedOrderedSubmitNodeCollector {
}

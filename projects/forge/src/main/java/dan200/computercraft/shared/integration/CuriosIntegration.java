// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.pocket.core.PocketHolder;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/// Integration with [Curios](https://github.com/TheIllusiveC4/Curios/).
public final class CuriosIntegration {
    public static final String ID = CuriosApi.MODID;

    public static void setup() {
        CuriosApi.registerCurio(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get(), PocketComputerCurio.INSTANCE);
        CuriosApi.registerCurio(ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get(), PocketComputerCurio.INSTANCE);
    }

    private static final class PocketComputerCurio implements ICurioItem {
        private static final PocketComputerCurio INSTANCE = new PocketComputerCurio();

        @Override
        public void curioTick(SlotContext slotContext, ItemStack stack) {
            if (!(stack.getItem() instanceof PocketComputerItem pocket)) return;
            if (!(slotContext.entity() instanceof ServerPlayer player)) return;

            var inventory = CuriosApi.getCuriosInventory(slotContext.entity());
            if (!inventory.isPresent()) return;
            var handler = inventory.orElseThrow(NullPointerException::new)
                .getStacksHandler(slotContext.identifier()).orElse(null);
            if (handler == null) return;

            var stacks = slotContext.cosmetic() ? handler.getCosmeticStacks() : handler.getStacks();
            pocket.tickAndKeepAlive(stack, new PlayerPocketHolder(player, stacks, slotContext.index()));
        }
    }

    private record PlayerPocketHolder(
        ServerPlayer entity, IDynamicStackHandler handler, int index
    ) implements PocketHolder.PlayerHolder {
        @Override
        public ItemStack getStack(ServerComputer computer) {
            return PocketHolder.ensureServerComputer(computer, handler().getStackInSlot(index()));
        }

        @Override
        public void setChanged() {
        }
    }
}

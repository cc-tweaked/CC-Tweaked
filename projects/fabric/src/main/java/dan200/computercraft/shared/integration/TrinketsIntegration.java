// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.pocket.core.PocketHolder;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/// Integration with [Trinkets](https://github.com/emilyploszaj/trinkets/).
public final class TrinketsIntegration {
    public static final String ID = TrinketsMain.MOD_ID;

    private TrinketsIntegration() {
    }

    public static void setup() {
        TrinketsApi.registerTrinket(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get(), PocketComputerTrinket.INSTANCE);
        TrinketsApi.registerTrinket(ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get(), PocketComputerTrinket.INSTANCE);
    }

    private static final class PocketComputerTrinket implements Trinket {
        private static final PocketComputerTrinket INSTANCE = new PocketComputerTrinket();

        @Override
        public void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
            if (!(stack.getItem() instanceof PocketComputerItem pocket)) return;
            if (!(entity instanceof ServerPlayer player)) return;

            pocket.tickAndKeepAlive(stack, new PlayerPocketHolder(player, slot));
        }
    }

    private record PlayerPocketHolder(ServerPlayer entity, SlotReference slot) implements PocketHolder.PlayerHolder {
        @Override
        public ItemStack getStack(ServerComputer computer) {
            return PocketHolder.ensureServerComputer(computer, slot().inventory().getItem(slot().index()));
        }

        @Override
        public void setChanged() {
            slot().inventory().setChanged();
        }
    }
}

// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.pocket.peripherals;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.AbstractPocketUpgrade;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.upgrades.UpgradeType;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.Nullable;

public class PocketModem extends AbstractPocketUpgrade {
    public static final MapCodec<PocketModem> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC.fieldOf("item").forGetter(AbstractPocketUpgrade::getCraftingIngredient),
        Codec.BOOL.fieldOf("advanced").forGetter(x -> x.advanced)
    ).apply(instance, PocketModem::new));
    private final boolean advanced;

    public PocketModem(Ingredient item, boolean advanced) {
        super(advanced ? WirelessModemPeripheral.ADVANCED_ADJECTIVE : WirelessModemPeripheral.NORMAL_ADJECTIVE, item);
        this.advanced = advanced;
    }

    @Nullable
    @Override
    public IPeripheral createPeripheral(IPocketAccess access) {
        return new PocketModemPeripheral(advanced, access);
    }

    @Override
    public void update(IPocketAccess access, @Nullable IPeripheral peripheral) {
        if (!(peripheral instanceof PocketModemPeripheral modem)) return;

        var state = modem.getModemState();
        if (state.pollChanged()) access.setLight(state.isOpen() ? 0xBA0000 : -1);
    }

    @Override
    public UpgradeType<PocketModem> getType() {
        return ModRegistry.PocketUpgradeTypes.WIRELESS_MODEM.get();
    }
}

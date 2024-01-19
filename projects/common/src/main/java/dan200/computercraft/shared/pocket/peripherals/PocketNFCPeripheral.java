package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;

import javax.annotation.Nullable;

public class PocketNFCPeripheral extends PocketModemPeripheral {

    public PocketNFCPeripheral(IPocketAccess access) {
        super(false, access);
    }

    public void update(IPocketAccess access) {
        setLocation(access);

        var state = getModemState();
        if (state.pollChanged()) access.setLight(state.isOpen() ? 0xf39d20 : -1);
    }

    @Override
    public double getRange() {
        return 0;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof PocketNFCPeripheral;
    }
}

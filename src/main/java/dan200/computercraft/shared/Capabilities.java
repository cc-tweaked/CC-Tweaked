/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared;

import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

public final class Capabilities
{
    @CapabilityInject( IPeripheral.class )
    public static Capability<IPeripheral> CAPABILITY_PERIPHERAL = null;

    @CapabilityInject( IWiredElement.class )
    public static Capability<IWiredElement> CAPABILITY_WIRED_ELEMENT = null;

    private Capabilities()
    {
    }
}

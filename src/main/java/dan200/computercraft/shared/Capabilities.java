/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class Capabilities
{
    public static final Capability<IPeripheral> CAPABILITY_PERIPHERAL = CapabilityManager.get( new CapabilityToken<>() {} );

    public static final Capability<IWiredElement> CAPABILITY_WIRED_ELEMENT = CapabilityManager.get( new CapabilityToken<>() {} );

    private Capabilities()
    {
    }
}

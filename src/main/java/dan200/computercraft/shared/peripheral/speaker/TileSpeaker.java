/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.common.TilePeripheralBase;
import net.minecraft.util.EnumFacing;

public class TileSpeaker extends TilePeripheralBase
{
    // Statics
    public static final int MIN_TICKS_BETWEEN_SOUNDS = 1;

    // Members
    private final SpeakerPeripheral m_peripheral;

    public TileSpeaker()
    {
        super();
        m_peripheral = new SpeakerPeripheral( this );
    }

    @Override
    public void update()
    {
        m_peripheral.update();
    }

    // IPeripheralTile implementation

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        return m_peripheral;
    }
}

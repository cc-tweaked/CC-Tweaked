/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.AbstractPocketUpgrade;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PocketModem extends AbstractPocketUpgrade
{
    private final boolean advanced;

    public PocketModem( ResourceLocation id, ItemStack stack, boolean advanced )
    {
        super( id, advanced ? WirelessModemPeripheral.ADVANCED_ADJECTIVE : WirelessModemPeripheral.NORMAL_ADJECTIVE, stack );
        this.advanced = advanced;
    }

    @Nullable
    @Override
    public IPeripheral createPeripheral( @Nonnull IPocketAccess access )
    {
        return new PocketModemPeripheral( advanced );
    }

    @Override
    public void update( @Nonnull IPocketAccess access, @Nullable IPeripheral peripheral )
    {
        if( !(peripheral instanceof PocketModemPeripheral modem) ) return;

        Entity entity = access.getEntity();

        if( entity != null ) modem.setLocation( entity.getCommandSenderWorld(), entity.getEyePosition( 1 ) );

        ModemState state = modem.getModemState();
        if( state.pollChanged() ) access.setLight( state.isOpen() ? 0xBA0000 : -1 );
    }
}

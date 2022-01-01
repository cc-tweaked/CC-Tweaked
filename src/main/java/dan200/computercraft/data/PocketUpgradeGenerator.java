/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.PocketUpgradeDataProvider;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

import static dan200.computercraft.shared.Registry.ModItems;
import static dan200.computercraft.shared.Registry.ModPocketUpgradeSerialisers;

class PocketUpgradeGenerator extends PocketUpgradeDataProvider
{
    PocketUpgradeGenerator( DataGenerator generator )
    {
        super( generator );
    }

    @Override
    protected void addUpgrades( @Nonnull Consumer<Upgrade<PocketUpgradeSerialiser<?>>> addUpgrade )
    {
        addUpgrade.accept( simpleWithCustomItem( id( "speaker" ), ModPocketUpgradeSerialisers.SPEAKER.get(), ModItems.SPEAKER.get() ) );
        simpleWithCustomItem( id( "wireless_modem_normal" ), ModPocketUpgradeSerialisers.WIRELESS_MODEM_NORMAL.get(), ModItems.WIRELESS_MODEM_NORMAL.get() ).add( addUpgrade );
        simpleWithCustomItem( id( "wireless_modem_advanced" ), ModPocketUpgradeSerialisers.WIRELESS_MODEM_ADVANCED.get(), ModItems.WIRELESS_MODEM_ADVANCED.get() ).add( addUpgrade );
    }

    @Nonnull
    private static ResourceLocation id( @Nonnull String id )
    {
        return new ResourceLocation( ComputerCraft.MOD_ID, id );
    }
}

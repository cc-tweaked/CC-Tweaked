/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared;

import com.google.common.base.Preconditions;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PocketUpgrades
{
    private static final Map<String, IPocketUpgrade> upgrades = new HashMap<>();

    public static synchronized void register( @Nonnull IPocketUpgrade upgrade )
    {
        Preconditions.checkNotNull( upgrade, "upgrade cannot be null" );

        String id = upgrade.getUpgradeID().toString();
        IPocketUpgrade existing = upgrades.get( id );
        if( existing != null )
        {
            throw new IllegalStateException( "Error registering '" + upgrade.getUnlocalisedAdjective() + " pocket computer'. UpgradeID '" + id + "' is already registered by '" + existing.getUnlocalisedAdjective() + " pocket computer'" );
        }

        upgrades.put( id, upgrade );
    }


    public static IPocketUpgrade get( String id )
    {
        return upgrades.get( id );
    }

    public static IPocketUpgrade get( @Nonnull ItemStack stack )
    {
        if( stack.isEmpty() ) return null;

        for( IPocketUpgrade upgrade : upgrades.values() )
        {
            ItemStack craftingStack = upgrade.getCraftingItem();
            if( !craftingStack.isEmpty() && InventoryUtil.areItemsSimilar( stack, craftingStack ) )
            {
                return upgrade;
            }
        }

        return null;
    }

    public static Iterable<IPocketUpgrade> getVanillaUpgrades()
    {
        List<IPocketUpgrade> vanilla = new ArrayList<>();
        vanilla.add( ComputerCraft.PocketUpgrades.wirelessModemNormal );
        vanilla.add( ComputerCraft.PocketUpgrades.wirelessModemAdvanced );
        vanilla.add( ComputerCraft.PocketUpgrades.speaker );
        return vanilla;
    }
}

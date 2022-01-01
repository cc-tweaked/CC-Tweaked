/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public final class PocketUpgrades
{
    private static final Map<String, IPocketUpgrade> upgrades = new HashMap<>();
    private static final Map<IPocketUpgrade, String> upgradeOwners = new Object2ObjectLinkedOpenCustomHashMap<>( Util.identityStrategy() );

    private PocketUpgrades() {}

    public static synchronized void register( @Nonnull IPocketUpgrade upgrade )
    {
        Objects.requireNonNull( upgrade, "upgrade cannot be null" );

        String id = upgrade.getUpgradeID().toString();
        IPocketUpgrade existing = upgrades.get( id );
        if( existing != null )
        {
            throw new IllegalStateException( "Error registering '" + upgrade.getUnlocalisedAdjective() + " pocket computer'. UpgradeID '" + id + "' is already registered by '" + existing.getUnlocalisedAdjective() + " pocket computer'" );
        }

        upgrades.put( id, upgrade );

        ModContainer mc = ModLoadingContext.get().getActiveContainer();
        if( mc != null && mc.getModId() != null ) upgradeOwners.put( upgrade, mc.getModId() );
    }

    public static IPocketUpgrade get( String id )
    {
        // Fix a typo in the advanced modem upgrade's name. I'm sorry, I realise this is horrible.
        if( id.equals( "computercraft:advanved_modem" ) ) id = "computercraft:advanced_modem";

        return upgrades.get( id );
    }

    public static IPocketUpgrade get( @Nonnull ItemStack stack )
    {
        if( stack.isEmpty() ) return null;

        for( IPocketUpgrade upgrade : upgrades.values() )
        {
            ItemStack craftingStack = upgrade.getCraftingItem();
            if( !craftingStack.isEmpty() && craftingStack.getItem() == stack.getItem() && upgrade.isItemSuitable( stack ) )
            {
                return upgrade;
            }
        }

        return null;
    }

    @Nullable
    public static String getOwner( IPocketUpgrade upgrade )
    {
        return upgradeOwners.get( upgrade );
    }

    public static Iterable<IPocketUpgrade> getVanillaUpgrades()
    {
        List<IPocketUpgrade> vanilla = new ArrayList<>();
        vanilla.add( ComputerCraft.PocketUpgrades.wirelessModemNormal );
        vanilla.add( ComputerCraft.PocketUpgrades.wirelessModemAdvanced );
        vanilla.add( ComputerCraft.PocketUpgrades.speaker );
        return vanilla;
    }

    public static Iterable<IPocketUpgrade> getUpgrades()
    {
        return Collections.unmodifiableCollection( upgrades.values() );
    }
}

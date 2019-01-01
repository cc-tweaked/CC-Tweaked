/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared;

import com.google.common.base.Preconditions;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.util.InventoryUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TurtleUpgrades
{
    private static final Map<String, ITurtleUpgrade> upgrades = new HashMap<>();
    private static final Int2ObjectMap<ITurtleUpgrade> legacyUpgrades = new Int2ObjectOpenHashMap<>();

    public static void register( @Nonnull ITurtleUpgrade upgrade )
    {
        Preconditions.checkNotNull( upgrade, "upgrade cannot be null" );

        int id = upgrade.getLegacyUpgradeID();
        if( id >= 0 && id < 64 )
        {
            String message = getMessage( upgrade, "Legacy UpgradeID '" + id + "' is reserved by ComputerCraft" );
            ComputerCraft.log.error( message );
            throw new RuntimeException( message );
        }

        registerInternal( upgrade );
    }

    public static void registerInternal( ITurtleUpgrade upgrade )
    {
        Preconditions.checkNotNull( upgrade, "upgrade cannot be null" );

        // Check conditions
        int legacyId = upgrade.getLegacyUpgradeID();
        if( legacyId >= 0 )
        {
            if( legacyId >= Short.MAX_VALUE )
            {
                String message = getMessage( upgrade, "UpgradeID '" + legacyId + "' is out of range" );
                ComputerCraft.log.error( message );
                throw new RuntimeException( message );
            }

            ITurtleUpgrade existing = legacyUpgrades.get( legacyId );
            if( existing != null )
            {
                String message = getMessage( upgrade, "UpgradeID '" + legacyId + "' is already registered by '" + existing.getUnlocalisedAdjective() + " Turtle'" );
                ComputerCraft.log.error( message );
                throw new RuntimeException( message );
            }
        }

        String id = upgrade.getUpgradeID().toString();
        ITurtleUpgrade existing = upgrades.get( id );
        if( existing != null )
        {
            String message = getMessage( upgrade, "UpgradeID '" + id + "' is already registered by '" + existing.getUnlocalisedAdjective() + " Turtle'" );
            ComputerCraft.log.error( message );
            throw new RuntimeException( message );
        }

        // Register
        if( legacyId >= 0 ) legacyUpgrades.put( legacyId, upgrade );
        upgrades.put( id, upgrade );
    }

    private static String getMessage( ITurtleUpgrade upgrade, String rest )
    {
        return "Error registering '" + upgrade.getUnlocalisedAdjective() + " Turtle'. " + rest;
    }

    public static ITurtleUpgrade get( String id )
    {
        return upgrades.get( id );
    }

    public static ITurtleUpgrade get( int id )
    {
        return legacyUpgrades.get( id );
    }

    public static ITurtleUpgrade get( @Nonnull ItemStack stack )
    {
        if( stack.isEmpty() ) return null;

        for( ITurtleUpgrade upgrade : upgrades.values() )
        {
            ItemStack craftingStack = upgrade.getCraftingItem();
            if( !craftingStack.isEmpty() && InventoryUtil.areItemsStackable( stack, craftingStack ) )
            {
                return upgrade;
            }
        }

        return null;
    }

    public static Iterable<ITurtleUpgrade> getVanillaUpgrades()
    {
        List<ITurtleUpgrade> vanilla = new ArrayList<>();
        vanilla.add( ComputerCraft.Upgrades.diamondPickaxe );
        vanilla.add( ComputerCraft.Upgrades.diamondAxe );
        vanilla.add( ComputerCraft.Upgrades.diamondSword );
        vanilla.add( ComputerCraft.Upgrades.diamondShovel );
        vanilla.add( ComputerCraft.Upgrades.diamondHoe );
        vanilla.add( ComputerCraft.Upgrades.craftingTable );
        vanilla.add( ComputerCraft.Upgrades.wirelessModem );
        vanilla.add( ComputerCraft.Upgrades.advancedModem );
        vanilla.add( ComputerCraft.Upgrades.turtleSpeaker );
        return vanilla;
    }

    public static boolean suitableForFamily( ComputerFamily family, ITurtleUpgrade upgrade )
    {
        return true;
    }
}

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
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public final class TurtleUpgrades
{
    private static final Map<String, ITurtleUpgrade> upgrades = new HashMap<>();
    private static final Int2ObjectMap<ITurtleUpgrade> legacyUpgrades = new Int2ObjectOpenHashMap<>();
    private static final IdentityHashMap<ITurtleUpgrade, String> upgradeOwners = new IdentityHashMap<>();

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

    static void registerInternal( ITurtleUpgrade upgrade )
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

        ModContainer mc = Loader.instance().activeModContainer();
        if( mc != null && mc.getModId() != null ) upgradeOwners.put( upgrade, mc.getModId() );
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
            if( !craftingStack.isEmpty() && InventoryUtil.areItemsSimilar( stack, craftingStack ) )
            {
                return upgrade;
            }
        }

        return null;
    }

    public static Iterable<ITurtleUpgrade> getVanillaUpgrades()
    {
        List<ITurtleUpgrade> vanilla = new ArrayList<>();
        vanilla.add( ComputerCraft.TurtleUpgrades.diamondPickaxe );
        vanilla.add( ComputerCraft.TurtleUpgrades.diamondAxe );
        vanilla.add( ComputerCraft.TurtleUpgrades.diamondSword );
        vanilla.add( ComputerCraft.TurtleUpgrades.diamondShovel );
        vanilla.add( ComputerCraft.TurtleUpgrades.diamondHoe );
        vanilla.add( ComputerCraft.TurtleUpgrades.craftingTable );
        vanilla.add( ComputerCraft.TurtleUpgrades.wirelessModem );
        vanilla.add( ComputerCraft.TurtleUpgrades.advancedModem );
        vanilla.add( ComputerCraft.TurtleUpgrades.speaker );
        return vanilla;
    }

    @Nullable
    public static String getOwner( @Nonnull ITurtleUpgrade upgrade )
    {
        return upgradeOwners.get( upgrade );
    }

    public static Iterable<ITurtleUpgrade> getUpgrades()
    {
        return Collections.unmodifiableCollection( upgrades.values() );
    }

    public static boolean suitableForFamily( ComputerFamily family, ITurtleUpgrade upgrade )
    {
        return true;
    }
}

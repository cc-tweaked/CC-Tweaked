/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public final class TurtleUpgrades
{
    private static final Map<String, ITurtleUpgrade> upgrades = new HashMap<>();
    private static final IdentityHashMap<ITurtleUpgrade, String> upgradeOwners = new IdentityHashMap<>();

    private TurtleUpgrades() {}

    public static void register( @Nonnull ITurtleUpgrade upgrade )
    {
        Objects.requireNonNull( upgrade, "upgrade cannot be null" );

        String id = upgrade.getUpgradeID().toString();
        ITurtleUpgrade existing = upgrades.get( id );
        if( existing != null )
        {
            throw new IllegalStateException( "Error registering '" + upgrade.getUnlocalisedAdjective() + " Turle'. UpgradeID '" + id + "' is already registered by '" + existing.getUnlocalisedAdjective() + " Turtle'" );
        }

        upgrades.put( id, upgrade );

        ModContainer mc = ModLoadingContext.get().getActiveContainer();
        if( mc != null && mc.getModId() != null ) upgradeOwners.put( upgrade, mc.getModId() );
    }


    public static ITurtleUpgrade get( String id )
    {
        return upgrades.get( id );
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


        // ComputerCraft upgrades
        vanilla.add( ComputerCraft.TurtleUpgrades.wirelessModemNormal );
        vanilla.add( ComputerCraft.TurtleUpgrades.wirelessModemAdvanced );
        vanilla.add( ComputerCraft.TurtleUpgrades.speaker );

        // Vanilla Minecraft upgrades
        vanilla.add( ComputerCraft.TurtleUpgrades.diamondPickaxe );
        vanilla.add( ComputerCraft.TurtleUpgrades.diamondAxe );
        vanilla.add( ComputerCraft.TurtleUpgrades.diamondSword );
        vanilla.add( ComputerCraft.TurtleUpgrades.diamondShovel );
        vanilla.add( ComputerCraft.TurtleUpgrades.diamondHoe );
        vanilla.add( ComputerCraft.TurtleUpgrades.craftingTable );
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

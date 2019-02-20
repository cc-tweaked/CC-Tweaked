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
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TurtleUpgrades
{
    private static final Map<String, ITurtleUpgrade> upgrades = new HashMap<>();

    public static void register( @Nonnull ITurtleUpgrade upgrade )
    {
        Preconditions.checkNotNull( upgrade, "upgrade cannot be null" );

        String id = upgrade.getUpgradeId().toString();
        ITurtleUpgrade existing = upgrades.get( id );
        if( existing != null )
        {
            throw new IllegalStateException( "Error registering '" + upgrade.getUnlocalisedAdjective() + " Turle'. UpgradeID '" + id + "' is already registered by '" + existing.getUnlocalisedAdjective() + " Turtle'" );
        }

        upgrades.put( id, upgrade );
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

    public static boolean suitableForFamily( ComputerFamily family, ITurtleUpgrade upgrade )
    {
        return true;
    }
}

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
import java.util.stream.Stream;

public final class TurtleUpgrades
{
    public static class Wrapper
    {
        final ITurtleUpgrade upgrade;
        final int legacyId;
        final String id;
        final String modId;
        boolean enabled;

        public Wrapper( ITurtleUpgrade upgrade )
        {
            ModContainer mc = ModLoadingContext.get().getActiveContainer();

            this.upgrade = upgrade;
            this.id = upgrade.getUpgradeID().toString();
            this.modId = mc != null && mc.getModId() != null ? mc.getModId() : null;
            this.enabled = true;
        }
    }

    private static ITurtleUpgrade[] vanilla;

    private static final Map<String, ITurtleUpgrade> upgrades = new HashMap<>();
    private static final IdentityHashMap<ITurtleUpgrade, Wrapper> wrappers = new IdentityHashMap<>();

    private TurtleUpgrades() {}

    public static void register( @Nonnull ITurtleUpgrade upgrade )
    {
        Objects.requireNonNull( upgrade, "upgrade cannot be null" );

        Wrapper wrapper = new Wrapper( upgrade );
        String id = wrapper.id;
        ITurtleUpgrade existing = upgrades.get( id );
        if( existing != null )
        {
            throw new IllegalStateException( "Error registering '" + upgrade.getUnlocalisedAdjective() + " Turle'. UpgradeID '" + id + "' is already registered by '" + existing.getUnlocalisedAdjective() + " Turtle'" );
        }

        upgrades.put( id, upgrade );
        wrappers.put( upgrade, wrapper );
    }

    @Nullable
    public static ITurtleUpgrade get( String id )
    {
        return upgrades.get( id );
    }

    @Nullable
    public static ITurtleUpgrade get( int id )
    {
        return legacyUpgrades.get( id );
    }

    @Nullable
    public static String getOwner( @Nonnull ITurtleUpgrade upgrade )
    {
        Wrapper wrapper = wrappers.get( upgrade );
        return wrapper != null ? wrapper.modId : null;
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

    public static Stream<ITurtleUpgrade> getVanillaUpgrades()
    {
        if( vanilla == null )
        {
            vanilla = new ITurtleUpgrade[] {
                // ComputerCraft upgrades
                ComputerCraft.TurtleUpgrades.wirelessModemNormal,
                ComputerCraft.TurtleUpgrades.wirelessModemAdvanced,
                ComputerCraft.TurtleUpgrades.speaker,

                // Vanilla Minecraft upgrades
                ComputerCraft.TurtleUpgrades.diamondPickaxe,
                ComputerCraft.TurtleUpgrades.diamondAxe,
                ComputerCraft.TurtleUpgrades.diamondSword,
                ComputerCraft.TurtleUpgrades.diamondShovel,
                ComputerCraft.TurtleUpgrades.diamondHoe,
                ComputerCraft.TurtleUpgrades.craftingTable,
            };
        }

        return Arrays.stream( vanilla ).filter( x -> x != null && wrappers.get( x ).enabled );
    }

    public static Iterable<ITurtleUpgrade> getUpgrades()
    {
        return Collections.unmodifiableCollection( upgrades.values() );
    }

    public static boolean suitableForFamily( ComputerFamily family, ITurtleUpgrade upgrade )
    {
        return true;
    }

    public static void disable( ITurtleUpgrade upgrade )
    {
        Wrapper wrapper = wrappers.get( upgrade );
        if( !wrapper.enabled ) return;

        wrapper.enabled = false;
        upgrades.remove( wrapper.id );
        if( wrapper.legacyId >= 0 ) legacyUpgrades.remove( wrapper.legacyId );
    }
}

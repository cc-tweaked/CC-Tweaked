/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.pocket;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.upgrades.IUpgradeBase;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A peripheral which can be equipped to the back side of a pocket computer.
 *
 * Pocket upgrades are defined in two stages. First, on creates a {@link IPocketUpgrade} subclass and corresponding
 * {@link PocketUpgradeSerialiser} instance, which are then registered in a Forge registry.
 *
 * You then write a JSON file in your mod's {@literal data/} folder. This is then parsed when the world is loaded, and
 * the upgrade registered internally. See the documentation in {@link PocketUpgradeSerialiser} for details on this process
 * and where files should be located.
 *
 * @see PocketUpgradeSerialiser For how to register a pocket computer upgrade.
 */
public interface IPocketUpgrade extends IUpgradeBase
{
    /**
     * Creates a peripheral for the pocket computer.
     *
     * The peripheral created will be stored for the lifetime of the upgrade, will be passed an argument to
     * {@link #update(IPocketAccess, IPeripheral)} and will be attached, detached and have methods called in the same
     * manner as an ordinary peripheral.
     *
     * @param access The access object for the pocket item stack.
     * @return The newly created peripheral.
     * @see #update(IPocketAccess, IPeripheral)
     */
    @Nullable
    IPeripheral createPeripheral( @Nonnull IPocketAccess access );

    /**
     * Called when the pocket computer item stack updates.
     *
     * @param access     The access object for the pocket item stack.
     * @param peripheral The peripheral for this upgrade.
     * @see #createPeripheral(IPocketAccess)
     */
    default void update( @Nonnull IPocketAccess access, @Nullable IPeripheral peripheral )
    {
    }

    /**
     * Called when the pocket computer is right clicked.
     *
     * @param world      The world the computer is in.
     * @param access     The access object for the pocket item stack.
     * @param peripheral The peripheral for this upgrade.
     * @return {@code true} to stop the GUI from opening, otherwise false. You should always provide some code path
     * which returns {@code false}, such as requiring the player to be sneaking - otherwise they will be unable to
     * access the GUI.
     * @see #createPeripheral(IPocketAccess)
     */
    default boolean onRightClick( @Nonnull Level world, @Nonnull IPocketAccess access, @Nullable IPeripheral peripheral )
    {
        return false;
    }
}

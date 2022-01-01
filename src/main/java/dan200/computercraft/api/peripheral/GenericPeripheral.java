/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.peripheral;

import dan200.computercraft.api.lua.GenericSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;

/**
 * A {@link GenericSource} which provides methods for a peripheral.
 *
 * Unlike a {@link GenericSource}, all methods <strong>should</strong> target the same type, for instance a
 * {@link BlockEntity} subclass or a capability interface. This is not currently enforced.
 */
public interface GenericPeripheral extends GenericSource
{
    /**
     * Get the type of the exposed peripheral.
     *
     * Unlike normal {@link IPeripheral}s, {@link GenericPeripheral} do not have to have a type. By default, the
     * resulting peripheral uses the resource name of the wrapped {@link BlockEntity} (for instance {@code minecraft:chest}).
     *
     * However, in some cases it may be more appropriate to specify a more readable name. Overriding this method allows
     * you to do so.
     *
     * When multiple {@link GenericPeripheral}s return a non-empty peripheral type for a single tile entity, the
     * lexicographically smallest will be chosen. In order to avoid this conflict, this method should only be
     * implemented when your peripheral targets a single tile entity <strong>AND</strong> it's likely that you're the
     * only mod to do so. Similarly this should <strong>NOT</strong> be implemented when your methods target a
     * capability or other interface (i.e. {@link IItemHandler}).
     *
     * @return The type of this peripheral or {@link PeripheralType#untyped()}.
     * @see IPeripheral#getType()
     */
    @Nonnull
    default PeripheralType getType()
    {
        return PeripheralType.untyped();
    }
}

/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.pocket;

import dan200.computercraft.api.upgrades.UpgradeDataProvider;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * A data provider to generate pocket computer upgrades.
 *
 * This should be subclassed and registered to a {@link DataGenerator}. Override the {@link #addUpgrades(Consumer)} function,
 * construct each upgrade, and pass them off to the provided consumer to generate them.
 *
 * @see GatherDataEvent To register your data provider
 * @see PocketUpgradeSerialiser
 */
public abstract class PocketUpgradeDataProvider extends UpgradeDataProvider<IPocketUpgrade, PocketUpgradeSerialiser<?>>
{
    public PocketUpgradeDataProvider( @Nonnull DataGenerator generator )
    {
        super( generator, "Pocket Computer Upgrades", "computercraft/pocket_upgrades", PocketUpgradeSerialiser.REGISTRY_ID );
    }
}

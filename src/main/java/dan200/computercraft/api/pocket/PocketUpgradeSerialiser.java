/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.pocket;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.upgrades.IUpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import dan200.computercraft.internal.upgrades.SerialiserWithCraftingItem;
import dan200.computercraft.internal.upgrades.SimpleSerialiser;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.RegistryManager;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Reads a {@link IPocketUpgrade} from disk and reads/writes it to a network packet.
 *
 * This follows the same format as {@link dan200.computercraft.api.turtle.TurtleUpgradeSerialiser} - consult the
 * documentation there for more information.
 *
 * @param <T> The type of pocket computer upgrade this is responsible for serialising.
 * @see IPocketUpgrade
 * @see PocketUpgradeDataProvider
 */
public interface PocketUpgradeSerialiser<T extends IPocketUpgrade> extends UpgradeSerialiser<T, PocketUpgradeSerialiser<?>>
{
    /**
     * The ID for the associated registry.
     *
     * This is largely intended for use with Forge Registry methods/classes, such as {@link DeferredRegister} and
     * {@link RegistryManager#getRegistry(ResourceKey)}.
     */
    ResourceKey<Registry<PocketUpgradeSerialiser<?>>> REGISTRY_ID = ResourceKey.createRegistryKey( new ResourceLocation( ComputerCraft.MOD_ID, "pocket_upgrade_serialiser" ) );

    /**
     * A convenient base class to inherit to implement {@link PocketUpgradeSerialiser}.
     *
     * @param <T> The type of the upgrade created by this serialiser.
     */
    abstract class Base<T extends IPocketUpgrade> extends ForgeRegistryEntry<PocketUpgradeSerialiser<?>> implements PocketUpgradeSerialiser<T>
    {
    }

    /**
     * Create an upgrade serialiser for a simple upgrade. This is similar to a {@link SimpleRecipeSerializer}, but for
     * upgrades.
     *
     * If you might want to vary the item, it's suggested you use {@link #simpleWithCustomItem(BiFunction)} instead.
     *
     * @param factory Generate a new upgrade with a specific ID.
     * @param <T>     The type of the generated upgrade.
     * @return The serialiser for this upgrade
     */
    @Nonnull
    static <T extends IPocketUpgrade> PocketUpgradeSerialiser<T> simple( @Nonnull Function<ResourceLocation, T> factory )
    {
        class Impl extends SimpleSerialiser<T, PocketUpgradeSerialiser<?>> implements PocketUpgradeSerialiser<T>
        {
            private Impl( Function<ResourceLocation, T> constructor )
            {
                super( constructor );
            }
        }

        return new Impl( factory );
    }

    /**
     * Create an upgrade serialiser for a simple upgrade whose crafting item can be specified.
     *
     * @param factory Generate a new upgrade with a specific ID and crafting item. The returned upgrade's
     *                {@link IUpgradeBase#getCraftingItem()} <strong>MUST</strong> equal the provided item.
     * @param <T>     The type of the generated upgrade.
     * @return The serialiser for this upgrade.
     * @see #simple(Function)  For upgrades whose crafting stack should not vary.
     */
    @Nonnull
    static <T extends IPocketUpgrade> PocketUpgradeSerialiser<T> simpleWithCustomItem( @Nonnull BiFunction<ResourceLocation, ItemStack, T> factory )
    {
        class Impl extends SerialiserWithCraftingItem<T, PocketUpgradeSerialiser<?>> implements PocketUpgradeSerialiser<T>
        {
            private Impl( BiFunction<ResourceLocation, ItemStack, T> factory )
            {
                super( factory );
            }
        }

        return new Impl( factory );
    }
}

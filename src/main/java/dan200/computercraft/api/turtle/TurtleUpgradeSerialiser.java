/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.turtle;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.upgrades.IUpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import dan200.computercraft.internal.upgrades.SerialiserWithCraftingItem;
import dan200.computercraft.internal.upgrades.SimpleSerialiser;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Reads a {@link ITurtleUpgrade} from disk and reads/writes it to a network packet.
 *
 * These should be registered in a {@link IForgeRegistry} while the game is loading, much like {@link RecipeSerializer}s.
 * It is suggested you use a {@link DeferredRegister}.
 *
 * If your turtle upgrade doesn't have any associated configurable parameters (like most upgrades), you can use
 * {@link #simple(Function)} or {@link #simpleWithCustomItem(BiFunction)} to create a basic upgrade serialiser.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * static final DeferredRegister<TurtleUpgradeSerialiser<?>> SERIALISERS = DeferredRegister.create( TurtleUpgradeSerialiser.TYPE, "my_mod" );
 *
 * // Register a new upgrade serialiser called "my_upgrade".
 * public static final RegistryObject<TurtleUpgradeSerialiser<MyUpgrade>> MY_UPGRADE =
 *     SERIALISERS.register( "my_upgrade", () -> TurtleUpgradeSerialiser.simple( MyUpgrade::new ) );
 *
 * // Then in your constructor
 * SERIALISERS.register( bus );
 * }</pre>
 *
 * We can then define a new upgrade using JSON by placing the following in
 * {@literal data/<my_mod>/computercraft/turtle_upgrades/<my_upgrade_id>.json}}.
 *
 * <pre>{@code
 * {
 *     "type": my_mod:my_upgrade",
 * }
 * }</pre>
 *
 * {@link TurtleUpgradeDataProvider} provides a data provider to aid with generating these JSON files.
 *
 * @param <T> The type of turtle upgrade this is responsible for serialising.
 * @see ITurtleUpgrade
 * @see TurtleUpgradeDataProvider
 */
public interface TurtleUpgradeSerialiser<T extends ITurtleUpgrade> extends UpgradeSerialiser<T, TurtleUpgradeSerialiser<?>>
{
    /**
     * The ID for the associated registry.
     *
     * This is largely intended for use with Forge Registry methods/classes, such as {@link DeferredRegister} and
     * {@link RegistryManager#getRegistry(ResourceKey)}.
     */
    ResourceKey<Registry<TurtleUpgradeSerialiser<?>>> REGISTRY_ID = ResourceKey.createRegistryKey( new ResourceLocation( ComputerCraft.MOD_ID, "turtle_upgrade_serialiser" ) );

    /**
     * A convenient base class to inherit to implement {@link TurtleUpgradeSerialiser}.
     *
     * @param <T> The type of the upgrade created by this serialiser.
     */
    abstract class Base<T extends ITurtleUpgrade> extends ForgeRegistryEntry<TurtleUpgradeSerialiser<?>> implements TurtleUpgradeSerialiser<T>
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
    static <T extends ITurtleUpgrade> TurtleUpgradeSerialiser<T> simple( @Nonnull Function<ResourceLocation, T> factory )
    {
        class Impl extends SimpleSerialiser<T, TurtleUpgradeSerialiser<?>> implements TurtleUpgradeSerialiser<T>
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
    static <T extends ITurtleUpgrade> TurtleUpgradeSerialiser<T> simpleWithCustomItem( @Nonnull BiFunction<ResourceLocation, ItemStack, T> factory )
    {
        class Impl extends SerialiserWithCraftingItem<T, TurtleUpgradeSerialiser<?>> implements TurtleUpgradeSerialiser<T>
        {
            private Impl( BiFunction<ResourceLocation, ItemStack, T> factory )
            {
                super( factory );
            }
        }

        return new Impl( factory );
    }
}

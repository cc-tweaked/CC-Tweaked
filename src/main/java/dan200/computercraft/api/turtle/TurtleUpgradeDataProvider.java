/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.turtle;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.upgrades.UpgradeDataProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A data provider to generate turtle upgrades.
 *
 * This should be subclassed and registered to a {@link DataGenerator}. Override the {@link #addUpgrades(Consumer)} function,
 * construct each upgrade, and pass them off to the provided consumer to generate them.
 *
 * @see GatherDataEvent To register your data provider
 * @see TurtleUpgradeSerialiser
 */
public abstract class TurtleUpgradeDataProvider extends UpgradeDataProvider<ITurtleUpgrade, TurtleUpgradeSerialiser<?>>
{
    public TurtleUpgradeDataProvider( DataGenerator generator )
    {
        super( generator, "Turtle Upgrades", "computercraft/turtle_upgrades", TurtleUpgradeSerialiser.TYPE );
    }

    @Nonnull
    public final ToolBuilder tool( @Nonnull ToolType type, @Nonnull ResourceLocation id, @Nonnull Item item )
    {
        ResourceLocation itemId = Objects.requireNonNull( item.getRegistryName(), "Item has not been registered" );
        return new ToolBuilder( id, existingSerialiser( type.getSerialiser() ), item );
    }

    public enum ToolType
    {
        GENERIC( new ResourceLocation( ComputerCraftAPI.MOD_ID, "tool" ) ),

        AXE( new ResourceLocation( ComputerCraftAPI.MOD_ID, "axe" ) ),
        HOE( new ResourceLocation( ComputerCraftAPI.MOD_ID, "hoe" ) ),
        SHOVEL( new ResourceLocation( ComputerCraftAPI.MOD_ID, "shovel" ) ),
        SWORD( new ResourceLocation( ComputerCraftAPI.MOD_ID, "sword" ) );

        private final ResourceLocation serialiser;

        ToolType( @Nonnull ResourceLocation serialiser )
        {
            this.serialiser = serialiser;
        }

        @Nonnull
        public ResourceLocation getSerialiser()
        {
            return serialiser;
        }
    }

    /**
     * A builder for custom turtle tool upgrades.
     *
     * @see #tool(ToolType, ResourceLocation, Item)
     */
    public static class ToolBuilder
    {
        private final ResourceLocation id;
        private final TurtleUpgradeSerialiser<?> serialiser;
        private final Item toolItem;
        private String adjective;
        private Item craftingItem;

        ToolBuilder( ResourceLocation id, TurtleUpgradeSerialiser<?> serialiser, Item toolItem )
        {
            this.id = id;
            this.serialiser = serialiser;
            this.toolItem = toolItem;
            craftingItem = null;
        }

        /**
         * Specify a custom adjective for this tool. By default this takes its adjective from the tool item.
         *
         * @param adjective The new adjective to use.
         * @return The tool builder, for further use.
         */
        @Nonnull
        public ToolBuilder withAdjective( @Nonnull String adjective )
        {
            this.adjective = adjective;
            return this;
        }

        /**
         * Specify a custom item which is used to craft this upgrade. By default this is the same as the provided tool
         * item, but you may wish to override it.
         *
         * @param craftingItem The item used to craft this upgrade.
         * @return The tool builder, for further use.
         */
        @Nonnull
        public ToolBuilder withCraftingItem( @Nonnull Item craftingItem )
        {
            this.craftingItem = craftingItem;
            return this;
        }

        /**
         * Register this as an upgrade.
         *
         * @param add The callback given to {@link #addUpgrades(Consumer)}.
         */
        public void add( @Nonnull Consumer<Upgrade<TurtleUpgradeSerialiser<?>>> add )
        {
            add.accept( new Upgrade<>( id, serialiser, s -> {
                s.addProperty( "item", toolItem.getRegistryName().toString() );
                if( adjective != null ) s.addProperty( "adjective", adjective );
                if( craftingItem != null ) s.addProperty( "craftItem", craftingItem.getRegistryName().toString() );
            } ) );
        }
    }
}

/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.turtle;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.api.upgrades.UpgradeDataProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

import javax.annotation.Nonnull;
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
    private static final ResourceLocation TOOL_ID = new ResourceLocation( ComputerCraftAPI.MOD_ID, "tool" );

    public TurtleUpgradeDataProvider( DataGenerator generator )
    {
        super( generator, "Turtle Upgrades", "computercraft/turtle_upgrades", TurtleUpgradeSerialiser.REGISTRY_ID );
    }

    /**
     * Create a new turtle tool upgrade, such as a pickaxe or shovel.
     *
     * @param id   The ID of this tool.
     * @param item The item used for tool actions. Note, this doesn't inherit all properties of the tool, you may need
     *             to specify {@link ToolBuilder#damageMultiplier(float)} and {@link ToolBuilder#breakable(TagKey)}.
     * @return A tool builder,
     */
    @Nonnull
    public final ToolBuilder tool( @Nonnull ResourceLocation id, @Nonnull Item item )
    {
        return new ToolBuilder( id, existingSerialiser( TOOL_ID ), item );
    }

    /**
     * A builder for custom turtle tool upgrades.
     *
     * @see #tool(ResourceLocation, Item)
     */
    public static class ToolBuilder
    {
        private final ResourceLocation id;
        private final TurtleUpgradeSerialiser<?> serialiser;
        private final Item toolItem;
        private String adjective;
        private Item craftingItem;
        private Float damageMultiplier = null;
        private TagKey<Block> breakable;

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
        public ToolBuilder adjective( @Nonnull String adjective )
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
        public ToolBuilder craftingItem( @Nonnull Item craftingItem )
        {
            this.craftingItem = craftingItem;
            return this;
        }

        /**
         * The amount of damage a swing of this tool will do. This is multiplied by {@link Attributes#ATTACK_DAMAGE} to
         * get the final damage.
         *
         * @param damageMultiplier The damage multiplier.
         * @return The tool builder, for futher use.
         */
        public ToolBuilder damageMultiplier( float damageMultiplier )
        {
            this.damageMultiplier = damageMultiplier;
            return this;
        }

        /**
         * Provide a list of breakable blocks. If not given, the tool can break all blocks. If given, only blocks
         * in this tag, those in {@link ComputerCraftTags.Blocks#TURTLE_ALWAYS_BREAKABLE} and "insta-mine" ones can
         * be broken.
         *
         * @param breakable The tag containing all blocks breakable by this item.
         * @return The tool builder, for further use.
         * @see ComputerCraftTags.Blocks
         */
        public ToolBuilder breakable( @Nonnull TagKey<Block> breakable )
        {
            this.breakable = breakable;
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
                if( damageMultiplier != null ) s.addProperty( "damageMultiplier", damageMultiplier );
                if( breakable != null ) s.addProperty( "breakable", breakable.location().toString() );
            } ) );
        }
    }
}

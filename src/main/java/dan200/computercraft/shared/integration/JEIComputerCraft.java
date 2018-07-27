package dan200.computercraft.shared.integration;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.turtle.items.ITurtleItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.ISubtypeRegistry.ISubtypeInterpreter;
import mezz.jei.api.JEIPlugin;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

@JEIPlugin
public class JEIComputerCraft implements IModPlugin
{
    @Override
    public void registerItemSubtypes( ISubtypeRegistry subtypeRegistry )
    {
        subtypeRegistry.registerSubtypeInterpreter( Item.getItemFromBlock( ComputerCraft.Blocks.turtleNormal ), turtleSubtype );
        subtypeRegistry.registerSubtypeInterpreter( Item.getItemFromBlock( ComputerCraft.Blocks.turtleAdvanced ), turtleSubtype );

        subtypeRegistry.registerSubtypeInterpreter( ComputerCraft.Items.pocketComputerNormal, pocketSubtype );
        subtypeRegistry.registerSubtypeInterpreter( ComputerCraft.Items.pocketComputerAdvanced, pocketSubtype );

        subtypeRegistry.registerSubtypeInterpreter( ComputerCraft.Items.disk, diskSubtype );
    }

    @Override
    public void register( IModRegistry registry )
    {
        // Hide treasure disks from the ingredient list
        registry.getJeiHelpers().getIngredientBlacklist()
            .addIngredientToBlacklist( new ItemStack( ComputerCraft.Items.treasureDisk, OreDictionary.WILDCARD_VALUE ) );
    }

    /**
     * Distinguishes turtles by upgrades and family
     */
    private static final ISubtypeInterpreter turtleSubtype = stack -> {
        Item item = stack.getItem();
        if( !(item instanceof ITurtleItem) ) return "";

        ITurtleItem turtle = (ITurtleItem) item;
        StringBuilder name = new StringBuilder();

        name.append( turtle.getFamily( stack ).toString() );

        // Add left and right upgrades to the identifier
        ITurtleUpgrade left = turtle.getUpgrade( stack, TurtleSide.Left );
        name.append( '|' );
        if( left != null ) name.append( left.getUpgradeID() );

        ITurtleUpgrade right = turtle.getUpgrade( stack, TurtleSide.Right );
        name.append( '|' );
        if( right != null ) name.append( '|' ).append( right.getUpgradeID() );

        return name.toString();
    };

    /**
     * Distinguishes pocket computers by upgrade and family
     */
    private static final ISubtypeInterpreter pocketSubtype = stack -> {
        Item item = stack.getItem();
        if( !(item instanceof ItemPocketComputer) ) return "";

        ItemPocketComputer pocket = (ItemPocketComputer) item;
        StringBuilder name = new StringBuilder();

        name.append( pocket.getFamily( stack ).toString() );

        // Add the upgrade to the identifier
        IPocketUpgrade upgrade = pocket.getUpgrade( stack );
        name.append( '|' );
        if( upgrade != null ) name.append( upgrade.getUpgradeID() );

        return name.toString();
    };

    /**
     * Distinguishes disks by colour
     */
    private static final ISubtypeInterpreter diskSubtype = stack -> {
        Item item = stack.getItem();
        if( !(item instanceof ItemDisk) ) return "";

        ItemDisk disk = (ItemDisk) item;

        int colour = disk.getColour( stack );
        return colour == -1 ? "" : String.format( "%06x", colour );
    };
}

/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.TurtleUpgradeDataProvider;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

import static dan200.computercraft.shared.Registry.ModItems;
import static dan200.computercraft.shared.Registry.ModTurtleSerialisers;

class TurtleUpgradeGenerator extends TurtleUpgradeDataProvider
{
    TurtleUpgradeGenerator( DataGenerator generator )
    {
        super( generator );
    }

    @Override
    protected void addUpgrades( @Nonnull Consumer<Upgrade<TurtleUpgradeSerialiser<?>>> addUpgrade )
    {
        simpleWithCustomItem( id( "speaker" ), ModTurtleSerialisers.SPEAKER.get(), ModItems.SPEAKER.get() ).add( addUpgrade );
        simpleWithCustomItem( vanilla( "crafting_table" ), ModTurtleSerialisers.WORKBENCH.get(), Items.CRAFTING_TABLE ).add( addUpgrade );
        simpleWithCustomItem( id( "wireless_modem_normal" ), ModTurtleSerialisers.WIRELESS_MODEM_NORMAL.get(), ModItems.WIRELESS_MODEM_NORMAL.get() ).add( addUpgrade );
        simpleWithCustomItem( id( "wireless_modem_advanced" ), ModTurtleSerialisers.WIRELESS_MODEM_ADVANCED.get(), ModItems.WIRELESS_MODEM_ADVANCED.get() ).add( addUpgrade );

        tool( ToolType.AXE, vanilla( "diamond_axe" ), Items.DIAMOND_AXE ).add( addUpgrade );
        tool( ToolType.GENERIC, vanilla( "diamond_pickaxe" ), Items.DIAMOND_PICKAXE ).add( addUpgrade );
        tool( ToolType.HOE, vanilla( "diamond_hoe" ), Items.DIAMOND_HOE ).add( addUpgrade );
        tool( ToolType.SHOVEL, vanilla( "diamond_shovel" ), Items.DIAMOND_SHOVEL ).add( addUpgrade );
        tool( ToolType.SWORD, vanilla( "diamond_sword" ), Items.DIAMOND_SWORD ).add( addUpgrade );
    }

    @Nonnull
    private static ResourceLocation id( @Nonnull String id )
    {
        return new ResourceLocation( ComputerCraft.MOD_ID, id );
    }

    @Nonnull
    private static ResourceLocation vanilla( @Nonnull String id )
    {
        // Naughty, please don't do this. Mostly here for some semblance of backwards compatibility.
        return new ResourceLocation( "minecraft", id );
    }
}

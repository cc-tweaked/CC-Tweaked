/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;

public class TurtleCraftingTable implements ITurtleUpgrade
{
    private ResourceLocation m_id;
    private int m_legacyID;
    private ItemStack m_item;

    @SideOnly( Side.CLIENT )
    private ModelResourceLocation m_leftModel;

    @SideOnly( Side.CLIENT )
    private ModelResourceLocation m_rightModel;

    public TurtleCraftingTable( int legacyId )
    {
        m_id = new ResourceLocation( "minecraft", "crafting_table" );
        m_legacyID = legacyId;
        m_item = new ItemStack( Blocks.CRAFTING_TABLE, 1, 0 );
    }

    @Nonnull
    @Override
    public ResourceLocation getUpgradeID()
    {
        return m_id;
    }

    @Override
    public int getLegacyUpgradeID()
    {
        return m_legacyID;
    }

    @Nonnull
    @Override
    public String getUnlocalisedAdjective()
    {
        return "upgrade.minecraft:crafting_table.adjective";
    }

    @Nonnull
    @Override
    public TurtleUpgradeType getType()
    {
        return TurtleUpgradeType.Peripheral;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingItem()
    {
        return m_item;
    }

    @Override
    public IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return new CraftingTablePeripheral( turtle );
    }

    @SideOnly( Side.CLIENT )
    private void loadModelLocations()
    {
        if( m_leftModel == null )
        {
            m_leftModel = new ModelResourceLocation( "computercraft:turtle_crafting_table_left", "inventory" );
            m_rightModel = new ModelResourceLocation( "computercraft:turtle_crafting_table_right", "inventory" );
        }
    }

    @Nonnull
    @Override
    @SideOnly( Side.CLIENT )
    public Pair<IBakedModel, Matrix4f> getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        loadModelLocations();

        Matrix4f transform = null;
        Minecraft mc = Minecraft.getMinecraft();
        ModelManager modelManager = mc.getRenderItem().getItemModelMesher().getModelManager();
        if( side == TurtleSide.Left )
        {
            return Pair.of( modelManager.getModel( m_leftModel ), transform );
        }
        else
        {
            return Pair.of( modelManager.getModel( m_rightModel ), transform );
        }
    }
}

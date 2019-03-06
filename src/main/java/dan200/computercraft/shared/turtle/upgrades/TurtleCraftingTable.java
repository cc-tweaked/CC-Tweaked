/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;

public class TurtleCraftingTable extends AbstractTurtleUpgrade
{
    @Environment( EnvType.CLIENT )
    private ModelIdentifier m_leftModel;

    @Environment( EnvType.CLIENT )
    private ModelIdentifier m_rightModel;

    public TurtleCraftingTable( Identifier id )
    {
        super( id, TurtleUpgradeType.Peripheral, Blocks.CRAFTING_TABLE );
    }

    @Override
    public IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return new CraftingTablePeripheral( turtle );
    }

    @Environment( EnvType.CLIENT )
    private void loadModelLocations()
    {
        if( m_leftModel == null )
        {
            m_leftModel = new ModelIdentifier( "computercraft:turtle_crafting_table_left", "inventory" );
            m_rightModel = new ModelIdentifier( "computercraft:turtle_crafting_table_right", "inventory" );
        }
    }

    @Nonnull
    @Override
    @Environment( EnvType.CLIENT )
    public Pair<BakedModel, Matrix4f> getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        loadModelLocations();

        Matrix4f transform = null;
        BakedModelManager modelManager = MinecraftClient.getInstance().getItemRenderer().getModels().getModelManager();
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

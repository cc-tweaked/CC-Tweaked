/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2016. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */


package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.PeripheralItemFactory;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;

public class TurtleSpeaker implements ITurtleUpgrade
{
    private static class Peripheral extends SpeakerPeripheral
    {
        // Members
        ITurtleAccess m_turtle;

        public Peripheral(ITurtleAccess turtle)
        {
            super();
            m_turtle = turtle;
        }

        @Override
        public void update()
        {
            super.update();
        }

        @Override
        public World getWorld()
        {
            return m_turtle.getWorld();
        }

        @Override
        public BlockPos getPos()
        {
            return m_turtle.getPosition();
        }

        @Override
        public boolean equals(IPeripheral other)
        {
            if (other instanceof Peripheral)
            {
                Peripheral otherPeripheral = (Peripheral) other;
                return otherPeripheral.m_turtle == m_turtle;
            }

            return false;
        }
    }

    // Members
    private ResourceLocation m_id;

    @SideOnly( Side.CLIENT )
    private ModelResourceLocation m_leftModel;

    @SideOnly( Side.CLIENT )
    private ModelResourceLocation m_rightModel;

    public TurtleSpeaker( ResourceLocation id )
    {
        m_id = id;
    }

    @Nonnull
    @Override
    public ResourceLocation getUpgradeID()
    {
        return m_id;
    }

    @Nonnull
    @Override
    public String getUnlocalisedAdjective()
    {
        return "upgrade.computercraft:speaker.adjective";
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
        return PeripheralItemFactory.create( PeripheralType.Speaker, null, 1 );
    }

    @Override
    public IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return new TurtleSpeaker.Peripheral(turtle);
    }

    @Nonnull
    @Override
    public TurtleCommandResult useTool( @Nonnull ITurtleAccess turtleAccess, @Nonnull TurtleSide turtleSide, @Nonnull TurtleVerb verb, @Nonnull EnumFacing direction )
    {
        return TurtleCommandResult.failure();
    }

    @SideOnly( Side.CLIENT )
    private void loadModelLocations()
    {
        if( m_leftModel == null )
        {
            m_leftModel = new ModelResourceLocation( "computercraft:turtle_speaker_upgrade_left", "inventory" );
            m_rightModel = new ModelResourceLocation( "computercraft:turtle_speaker_upgrade_right", "inventory" );
        }
    }

    @Nonnull
    @Override
    @SideOnly( Side.CLIENT )
    public Pair<IBakedModel, Matrix4f> getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        loadModelLocations();
        ModelManager modelManager = Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getModelManager();

        if( side == TurtleSide.Left )
        {
            return Pair.of( modelManager.getModel( m_leftModel ), null );
        }
        else
        {
            return Pair.of( modelManager.getModel( m_rightModel ), null );
        }
    }

    @Override
    public void update( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide turtleSide )
    {
        IPeripheral turtlePeripheral = turtle.getPeripheral( turtleSide );
        if ( turtlePeripheral instanceof Peripheral )
        {
            Peripheral peripheral = (Peripheral) turtlePeripheral;
            peripheral.update();
        }
    }
}

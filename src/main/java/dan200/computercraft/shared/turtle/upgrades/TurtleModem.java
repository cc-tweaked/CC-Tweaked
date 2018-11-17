/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.PeripheralItemFactory;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.WirelessModemPeripheral;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;

public class TurtleModem implements ITurtleUpgrade
{
    private static class Peripheral extends WirelessModemPeripheral
    {
        private final ITurtleAccess m_turtle;

        public Peripheral( ITurtleAccess turtle, boolean advanced )
        {
            super( new ModemState(), advanced );
            m_turtle = turtle;
        }

        @Nonnull
        @Override
        public World getWorld()
        {
            return m_turtle.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos turtlePos = m_turtle.getPosition();
            return new Vec3d(
                turtlePos.getX(),
                turtlePos.getY(),
                turtlePos.getZ()
            );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            if( other instanceof Peripheral )
            {
                Peripheral otherModem = (Peripheral)other;
                return otherModem.m_turtle == m_turtle;
            }
            return false;
        }
    }

    private boolean m_advanced;
    private ResourceLocation m_id;
    private int m_legacyID;

    @SideOnly( Side.CLIENT )
    private ModelResourceLocation m_leftOffModel;

    @SideOnly( Side.CLIENT )
    private ModelResourceLocation m_rightOffModel;

    @SideOnly( Side.CLIENT )
    private ModelResourceLocation m_leftOnModel;

    @SideOnly( Side.CLIENT )
    private ModelResourceLocation m_rightOnModel;

    public TurtleModem( boolean advanced, ResourceLocation id, int legacyId )
    {
        m_advanced = advanced;
        m_id = id;
        m_legacyID = legacyId;
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
        if( m_advanced )
        {
            return "upgrade.computercraft:advanced_modem.adjective";
        }
        else
        {
            return "upgrade.computercraft:wireless_modem.adjective";
        }
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
        if( m_advanced )
        {
            return PeripheralItemFactory.create( PeripheralType.AdvancedModem, null, 1 );
        }
        else
        {
            return PeripheralItemFactory.create( PeripheralType.WirelessModem, null, 1 );
        }
    }

    @Override
    public IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return new Peripheral( turtle, m_advanced );
    }

    @Nonnull
    @Override
    public TurtleCommandResult useTool( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull EnumFacing dir )
    {
        return TurtleCommandResult.failure();
    }

    @SideOnly( Side.CLIENT )
    private void loadModelLocations()
    {
        if( m_leftOffModel == null )
        {
            if( m_advanced )
            {
                m_leftOffModel = new ModelResourceLocation( "computercraft:advanced_turtle_modem_off_left", "inventory" );
                m_rightOffModel = new ModelResourceLocation( "computercraft:advanced_turtle_modem_off_right", "inventory" );
                m_leftOnModel = new ModelResourceLocation( "computercraft:advanced_turtle_modem_on_left", "inventory" );
                m_rightOnModel = new ModelResourceLocation( "computercraft:advanced_turtle_modem_on_right", "inventory" );
            }
            else
            {
                m_leftOffModel = new ModelResourceLocation( "computercraft:turtle_modem_off_left", "inventory" );
                m_rightOffModel = new ModelResourceLocation( "computercraft:turtle_modem_off_right", "inventory" );
                m_leftOnModel = new ModelResourceLocation( "computercraft:turtle_modem_on_left", "inventory" );
                m_rightOnModel = new ModelResourceLocation( "computercraft:turtle_modem_on_right", "inventory" );
            }
        }
    }

    @Nonnull
    @Override
    @SideOnly( Side.CLIENT )
    public Pair<IBakedModel, Matrix4f> getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        loadModelLocations();

        boolean active = false;
        if( turtle != null )
        {
            NBTTagCompound turtleNBT = turtle.getUpgradeNBTData( side );
            if( turtleNBT.hasKey( "active" ) )
            {
                active = turtleNBT.getBoolean( "active" );
            }
        }

        Matrix4f transform = null;
        Minecraft mc = Minecraft.getMinecraft();
        ModelManager modelManager = mc.getRenderItem().getItemModelMesher().getModelManager();
        if( side == TurtleSide.Left )
        {
            return Pair.of(
                active ? modelManager.getModel( m_leftOnModel ) : modelManager.getModel( m_leftOffModel ),
                transform
            );
        }
        else
        {
            return Pair.of(
                active ? modelManager.getModel( m_rightOnModel ) : modelManager.getModel( m_rightOffModel ),
                transform
            );
        }
    }

    @Override
    public void update( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        // Advance the modem
        if( !turtle.getWorld().isRemote )
        {
            IPeripheral peripheral = turtle.getPeripheral( side );
            if( peripheral instanceof Peripheral )
            {
                ModemState state = ((Peripheral) peripheral).getModemState();
                if( state.pollChanged() )
                {
                    turtle.getUpgradeNBTData( side ).setBoolean( "active", state.isOpen() );
                    turtle.updateUpgradeNBTData( side );
                }
            }
        }
    }
}

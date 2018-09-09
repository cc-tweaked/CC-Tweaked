/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import com.google.common.base.Objects;
import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaFunction;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.shared.computer.blocks.ComputerProxy;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.*;

public class TurtleBrain implements ITurtleAccess
{
    private static int s_nextInstanceID = 0;
    private static Map<Integer, WeakReference<TurtleBrain>> s_allClientBrains = new HashMap<>();

    public static int assignInstanceID()
    {
        return s_nextInstanceID++;
    }

    public static TurtleBrain getClientBrain( int instanceID )
    {
        if( instanceID >= 0 )
        {
            WeakReference<TurtleBrain> ref = s_allClientBrains.get( instanceID );
            if( ref != null )
            {
                TurtleBrain brain = ref.get();
                if( brain != null )
                {
                    return brain;
                }
                else
                {
                    s_allClientBrains.remove( instanceID );
                }
            }
        }
        return null;
    }

    public static void setClientBrain( int instanceID, TurtleBrain brain )
    {
        if( instanceID >= 0 )
        {
            if( getClientBrain( instanceID ) != brain )
            {
                s_allClientBrains.put( instanceID, new WeakReference<>( brain ) );
            }
        }
    }

    public static void cleanupBrains()
    {
        if( s_allClientBrains.size() > 0 )
        {
            Iterator<Map.Entry<Integer, WeakReference<TurtleBrain>>> it = s_allClientBrains.entrySet().iterator();
            while( it.hasNext() )
            {
                Map.Entry<Integer, WeakReference<TurtleBrain>> entry = it.next();
                WeakReference<TurtleBrain> ref = entry.getValue();
                if( ref != null )
                {
                    TurtleBrain brain = ref.get();
                    if( brain == null )
                    {
                        it.remove();
                    }
                }
            }
        }
    }

    private static final int ANIM_DURATION = 8;

    private TileTurtle m_owner;
    private ComputerProxy m_proxy;
    private GameProfile m_owningPlayer;

    private LinkedList<TurtleCommandQueueEntry> m_commandQueue;
    private int m_commandsIssued;

    private Map<TurtleSide, ITurtleUpgrade> m_upgrades;
    private Map<TurtleSide, IPeripheral> m_peripherals;
    private Map<TurtleSide, NBTTagCompound> m_upgradeNBTData;

    private int m_selectedSlot;
    private int m_fuelLevel;
    private int m_colourHex;
    private ResourceLocation m_overlay;

    private int m_instanceID;
    private EnumFacing m_direction;
    private TurtleAnimation m_animation;
    private int m_animationProgress;
    private int m_lastAnimationProgress;
    
    TurtlePlayer m_cachedPlayer;

    public TurtleBrain( TileTurtle turtle )
    {
        m_owner = turtle;

        m_commandQueue = new LinkedList<>();
        m_commandsIssued = 0;

        m_upgrades = new HashMap<>();
        m_peripherals = new HashMap<>();
        m_upgradeNBTData = new HashMap<>();

        m_selectedSlot = 0;
        m_fuelLevel = 0;
        m_colourHex = -1;
        m_overlay = null;

        m_instanceID = -1;
        m_direction = EnumFacing.NORTH;
        m_animation = TurtleAnimation.None;
        m_animationProgress = 0;
        m_lastAnimationProgress = 0;
    }

    public TurtleBrain getFutureSelf()
    {
        if( getOwner().getWorld().isRemote )
        {
            TurtleBrain futureSelf = getClientBrain( m_instanceID );
            if( futureSelf != null )
            {
                return futureSelf;
            }
        }
        return this;
    }

    public void setOwner( TileTurtle owner )
    {
        m_owner = owner;
    }

    public TileTurtle getOwner()
    {
        return m_owner;
    }

    public ComputerProxy getProxy()
    {
        if(m_proxy == null) {
            m_proxy = new ComputerProxy()
            {
                @Override
                protected TileComputerBase getTile()
                {
                    return m_owner;
                }
            };
        }
        return m_proxy;
    }

    public ComputerFamily getFamily()
    {
        return m_owner.getFamily();
    }

    public void setupComputer( ServerComputer computer )
    {
        updatePeripherals( computer );
    }

    public void update()
    {
        World world = getWorld();
        if( !world.isRemote )
        {
            // Advance movement
            updateCommands();
        }

        // Advance animation
        updateAnimation();

        // Advance upgrades
        if( !m_upgrades.isEmpty() )
        {
            for( Map.Entry<TurtleSide, ITurtleUpgrade> entry : m_upgrades.entrySet() )
            {
                entry.getValue().update( this, entry.getKey() );
            }
        }
    }

    public void readFromNBT( NBTTagCompound nbttagcompound )
    {
        // Read state
        m_direction = EnumFacing.byIndex( nbttagcompound.getInteger( "dir" ) );
        m_selectedSlot = nbttagcompound.getInteger( "selectedSlot" );
        if( nbttagcompound.hasKey( "fuelLevel" ) )
        {
            m_fuelLevel = nbttagcompound.getInteger( "fuelLevel" );
        }
        else
        {
            m_fuelLevel = 0;
        }

        // Read owner
        if( nbttagcompound.hasKey( "owner", Constants.NBT.TAG_COMPOUND ) )
        {
            NBTTagCompound owner = nbttagcompound.getCompoundTag( "owner" );
            m_owningPlayer = new GameProfile(
                new UUID( owner.getLong( "upper_id" ), owner.getLong( "lower_id" ) ),
                owner.getString( "name" )
            );
        }
        else
        {
            m_owningPlayer = null;
        }

        // Read colour
        m_colourHex = ColourUtils.getHexColour( nbttagcompound );

        // Read overlay
        if( nbttagcompound.hasKey( "overlay_mod" ) )
        {
            String overlay_mod = nbttagcompound.getString( "overlay_mod" );
            if( nbttagcompound.hasKey( "overlay_path" ) )
            {
                String overlay_path = nbttagcompound.getString( "overlay_path" );
                m_overlay = new ResourceLocation( overlay_mod, overlay_path );
            }
            else
            {
                m_overlay = null;
            }
        }
        else
        {
            m_overlay = null;
        }

        // Read upgrades
        // (pre-1.4 turtles will have a "subType" variable, newer things will have "leftUpgrade" and "rightUpgrade")
        ITurtleUpgrade leftUpgrade = null;
        ITurtleUpgrade rightUpgrade = null;
        if( nbttagcompound.hasKey( "subType" ) )
        {
            // Loading a pre-1.4 world
            int subType = nbttagcompound.getInteger( "subType" );
            if( (subType & 0x1) > 0 )
            {
                leftUpgrade = ComputerCraft.Upgrades.diamondPickaxe;
            }
            if( (subType & 0x2) > 0 )
            {
                rightUpgrade = ComputerCraft.Upgrades.wirelessModem;
            }
        }
        else
        {
            // Loading a post-1.4 world
            if( nbttagcompound.hasKey( "leftUpgrade" ) )
            {
                if( nbttagcompound.getTagId( "leftUpgrade" ) == Constants.NBT.TAG_STRING )
                {
                    leftUpgrade = ComputerCraft.getTurtleUpgrade( nbttagcompound.getString( "leftUpgrade" ) );
                }
                else
                {
                    leftUpgrade = ComputerCraft.getTurtleUpgrade( nbttagcompound.getShort( "leftUpgrade" ) );
                }
            }
            if( nbttagcompound.hasKey( "rightUpgrade" ) )
            {
                if( nbttagcompound.getTagId( "rightUpgrade" ) == Constants.NBT.TAG_STRING )
                {
                    rightUpgrade = ComputerCraft.getTurtleUpgrade( nbttagcompound.getString( "rightUpgrade" ) );
                }
                else
                {
                    rightUpgrade = ComputerCraft.getTurtleUpgrade( nbttagcompound.getShort( "rightUpgrade" ) );
                }
            }
        }
        setUpgrade( TurtleSide.Left, leftUpgrade );
        setUpgrade( TurtleSide.Right, rightUpgrade );

        // NBT
        m_upgradeNBTData.clear();
        if( nbttagcompound.hasKey( "leftUpgradeNBT" ) )
        {
            m_upgradeNBTData.put( TurtleSide.Left, nbttagcompound.getCompoundTag( "leftUpgradeNBT" ).copy() );
        }
        if( nbttagcompound.hasKey( "rightUpgradeNBT" ) )
        {
            m_upgradeNBTData.put( TurtleSide.Right, nbttagcompound.getCompoundTag( "rightUpgradeNBT" ).copy() );
        }
    }

    public NBTTagCompound writeToNBT( NBTTagCompound nbttagcompound )
    {
        // Write state
        nbttagcompound.setInteger( "dir", m_direction.getIndex() );
        nbttagcompound.setInteger( "selectedSlot", m_selectedSlot );
        nbttagcompound.setInteger( "fuelLevel", m_fuelLevel );

        // Write owner
        if( m_owningPlayer != null )
        {
            NBTTagCompound owner = new NBTTagCompound();
            nbttagcompound.setTag( "owner", owner );

            owner.setLong( "upper_id", m_owningPlayer.getId().getMostSignificantBits() );
            owner.setLong( "lower_id", m_owningPlayer.getId().getLeastSignificantBits() );
            owner.setString( "name", m_owningPlayer.getName() );
        }

        // Write upgrades
        String leftUpgradeID = getUpgradeID( getUpgrade( TurtleSide.Left ) );
        if( leftUpgradeID != null )
        {
            nbttagcompound.setString( "leftUpgrade", leftUpgradeID );
        }
        String rightUpgradeID = getUpgradeID( getUpgrade( TurtleSide.Right ) );
        if( rightUpgradeID != null )
        {
            nbttagcompound.setString( "rightUpgrade", rightUpgradeID );
        }

        // Write colour
        if( m_colourHex != -1 )
        {
            nbttagcompound.setInteger( "colour", m_colourHex );
        }

        // Write overlay
        if( m_overlay != null )
        {
            nbttagcompound.setString( "overlay_mod", m_overlay.getNamespace() );
            nbttagcompound.setString( "overlay_path", m_overlay.getPath() );
        }

        // Write NBT
        if( m_upgradeNBTData.containsKey( TurtleSide.Left ) )
        {
            nbttagcompound.setTag( "leftUpgradeNBT", getUpgradeNBTData( TurtleSide.Left ).copy() );
        }
        if( m_upgradeNBTData.containsKey( TurtleSide.Right ) )
        {
            nbttagcompound.setTag( "rightUpgradeNBT", getUpgradeNBTData( TurtleSide.Right ).copy() );
        }

        return nbttagcompound;
    }

    private String getUpgradeID( ITurtleUpgrade upgrade )
    {
        if( upgrade != null )
        {
            return upgrade.getUpgradeID().toString();
        }
        return null;
    }

    public void writeDescription( NBTTagCompound nbttagcompound )
    {
        // Upgrades
        String leftUpgradeID = getUpgradeID( getUpgrade( TurtleSide.Left ) );
        if( leftUpgradeID != null )
        {
            nbttagcompound.setString( "leftUpgrade", leftUpgradeID );
        }
        String rightUpgradeID = getUpgradeID( getUpgrade( TurtleSide.Right ) );
        if( rightUpgradeID != null )
        {
            nbttagcompound.setString( "rightUpgrade", rightUpgradeID );
        }

        // NBT
        if( m_upgradeNBTData.containsKey( TurtleSide.Left ) )
        {
            nbttagcompound.setTag( "leftUpgradeNBT", getUpgradeNBTData( TurtleSide.Left ).copy() );
        }
        if( m_upgradeNBTData.containsKey( TurtleSide.Right ) )
        {
            nbttagcompound.setTag( "rightUpgradeNBT", getUpgradeNBTData( TurtleSide.Right ).copy() );
        }

        // Colour
        if( m_colourHex != -1 )
        {
            nbttagcompound.setInteger( "colour", m_colourHex );
        }

        // Overlay
        if( m_overlay != null )
        {
            nbttagcompound.setString( "overlay_mod", m_overlay.getNamespace() );
            nbttagcompound.setString( "overlay_path", m_overlay.getPath() );
        }

        // Animation
        if( m_instanceID < 0 )
        {
            m_instanceID = assignInstanceID();
        }
        nbttagcompound.setInteger( "brainInstanceID", m_instanceID );
        nbttagcompound.setInteger( "animation", m_animation.ordinal() );
        nbttagcompound.setInteger( "direction", m_direction.getIndex() );
        nbttagcompound.setInteger( "fuelLevel", m_fuelLevel );
    }

    public void readDescription( NBTTagCompound nbttagcompound )
    {
        // Upgrades
        if( nbttagcompound.hasKey( "leftUpgrade" ) )
        {
            setUpgrade( TurtleSide.Left, ComputerCraft.getTurtleUpgrade( nbttagcompound.getString( "leftUpgrade" ) ) );
        }
        else
        {
            setUpgrade( TurtleSide.Left, null );
        }
        if( nbttagcompound.hasKey( "rightUpgrade" ) )
        {
            setUpgrade( TurtleSide.Right, ComputerCraft.getTurtleUpgrade( nbttagcompound.getString( "rightUpgrade" ) ) );
        }
        else
        {
            setUpgrade( TurtleSide.Right, null );
        }

        // NBT
        m_upgradeNBTData.clear();
        if( nbttagcompound.hasKey( "leftUpgradeNBT" ) )
        {
            m_upgradeNBTData.put( TurtleSide.Left, nbttagcompound.getCompoundTag( "leftUpgradeNBT" ).copy() );
        }
        if( nbttagcompound.hasKey( "rightUpgradeNBT" ) )
        {
            m_upgradeNBTData.put( TurtleSide.Right, nbttagcompound.getCompoundTag( "rightUpgradeNBT" ).copy() );
        }

        // Colour
        m_colourHex = ColourUtils.getHexColour( nbttagcompound );

        // Overlay
        if( nbttagcompound.hasKey( "overlay_mod" ) && nbttagcompound.hasKey( "overlay_path" ) )
        {
            String overlay_mod = nbttagcompound.getString( "overlay_mod" );
            String overlay_path = nbttagcompound.getString( "overlay_path" );
            m_overlay = new ResourceLocation( overlay_mod, overlay_path );
        }
        else
        {
            m_overlay = null;
        }

        // Animation
        m_instanceID = nbttagcompound.getInteger( "brainInstanceID" );
        setClientBrain( m_instanceID, this );

        TurtleAnimation anim = TurtleAnimation.values()[ nbttagcompound.getInteger( "animation" ) ];
        if( anim != m_animation &&
            anim != TurtleAnimation.Wait &&
            anim != TurtleAnimation.ShortWait &&
            anim != TurtleAnimation.None )
        {
            m_animation = TurtleAnimation.values()[ nbttagcompound.getInteger( "animation" ) ];
            m_animationProgress = 0;
            m_lastAnimationProgress = 0;
        }

        m_direction = EnumFacing.byIndex( nbttagcompound.getInteger( "direction" ) );
        m_fuelLevel = nbttagcompound.getInteger( "fuelLevel" );
    }

    @Nonnull
    @Override
    public World getWorld()
    {
        return m_owner.getWorld();
    }

    @Nonnull
    @Override
    public BlockPos getPosition()
    {
        return m_owner.getPos();
    }

    @Override
    public boolean teleportTo( @Nonnull World world, @Nonnull BlockPos pos )
    {
        if( world.isRemote || getWorld().isRemote )
        {
            throw new UnsupportedOperationException();
        }

        // Cache info about the old turtle (so we don't access this after we delete ourselves)
        World oldWorld = getWorld();
        TileTurtle oldOwner = m_owner;
        BlockPos oldPos = m_owner.getPos();
        Block oldBlock = m_owner.getBlock();

        if( oldWorld == world && oldPos.equals( pos ) )
        {
            // Teleporting to the current position is a no-op
            return true;
        }

        // Ensure the chunk is loaded
        if( !world.isBlockLoaded( pos ) ) return false;

        // Ensure we're inside the world border
        if( !world.getWorldBorder().contains( pos ) ) return false;

        oldOwner.notifyMoveStart();

        try
        {
            // Create a new turtle
            if( world.setBlockState( pos, oldBlock.getDefaultState(), 0 ) )
            {
                Block block = world.getBlockState( pos ).getBlock();
                if( block == oldBlock )
                {
                    TileEntity newTile = world.getTileEntity( pos );
                    if( newTile != null && newTile instanceof TileTurtle )
                    {
                        // Copy the old turtle state into the new turtle
                        TileTurtle newTurtle = (TileTurtle)newTile;
                        newTurtle.setWorld( world );
                        newTurtle.setPos( pos );
                        newTurtle.transferStateFrom( oldOwner );
                        newTurtle.createServerComputer().setWorld( world );
                        newTurtle.createServerComputer().setPosition( pos );

                        // Remove the old turtle
                        oldWorld.setBlockToAir( oldPos );

                        // Make sure everybody knows about it
                        newTurtle.updateBlock();
                        newTurtle.updateInput();
                        newTurtle.updateOutput();
                        return true;
                    }
                }

                // Something went wrong, remove the newly created turtle
                world.setBlockToAir( pos );
            }
        }
        finally
        {
            // whatever happens, unblock old turtle in case it's still in world
            oldOwner.notifyMoveEnd();
        }

        return false;
    }

    @Nonnull
    @Override
    public Vec3d getVisualPosition( float f )
    {
        Vec3d offset = getRenderOffset( f );
        BlockPos pos = m_owner.getPos();
        return new Vec3d(
            pos.getX() + 0.5 + offset.x,
            pos.getY() + 0.5 + offset.y,
            pos.getZ() + 0.5 + offset.z
        );
    }

    @Override
    public float getVisualYaw( float f )
    {
        float yaw = DirectionUtil.toYawAngle( getDirection() );
        switch( m_animation )
        {
            case TurnLeft:
            {
                yaw += 90.0f * (1.0f - getAnimationFraction( f ));
                if( yaw >= 360.0f )
                {
                    yaw -= 360.0f;
                }
                break;
            }
            case TurnRight:
            {
                yaw += -90.0f * (1.0f - getAnimationFraction( f ));
                if( yaw < 0.0f )
                {
                    yaw += 360.0f;
                }
                break;
            }
        }
        return yaw;
    }

    @Nonnull
    @Override
    public EnumFacing getDirection()
    {
        return m_direction;
    }

    @Override
    public void setDirection( @Nonnull EnumFacing dir )
    {
        if( dir.getAxis() == EnumFacing.Axis.Y )
        {
            dir = EnumFacing.NORTH;
        }
        m_direction = dir;
        m_owner.updateOutput();
        m_owner.updateInput();
        m_owner.onTileEntityChange();
    }

    @Override
    public int getSelectedSlot()
    {
        return m_selectedSlot;
    }

    @Override
    public void setSelectedSlot( int slot )
    {
        if( getWorld().isRemote )
        {
            throw new UnsupportedOperationException();
        }
        if( slot >= 0 && slot < m_owner.getSizeInventory() )
        {
            m_selectedSlot = slot;
            m_owner.onTileEntityChange();
        }
    }

    @Nonnull
    @Override
    public IInventory getInventory()
    {
        return m_owner;
    }

    @Nonnull
    @Override
    public IItemHandlerModifiable getItemHandler()
    {
        return m_owner.getItemHandler();
    }

    @Override
    public boolean isFuelNeeded()
    {
        return ComputerCraft.turtlesNeedFuel;
    }

    @Override
    public int getFuelLevel()
    {
        return Math.min( m_fuelLevel, getFuelLimit() );
    }

    @Override
    public void setFuelLevel( int level )
    {
        m_fuelLevel = Math.min( level, getFuelLimit() );
        m_owner.onTileEntityChange();
    }

    @Override
    public int getFuelLimit()
    {
        if( m_owner.getFamily() == ComputerFamily.Advanced )
        {
            return ComputerCraft.advancedTurtleFuelLimit;
        }
        else
        {
            return ComputerCraft.turtleFuelLimit;
        }
    }

    @Override
    public boolean consumeFuel( int fuel )
    {
        if( getWorld().isRemote )
        {
            throw new UnsupportedOperationException();
        }
        if( !isFuelNeeded() )
        {
            return true;
        }

        int consumption = Math.max( fuel, 0 );
        if( getFuelLevel() >= consumption )
        {
            setFuelLevel( getFuelLevel() - consumption );
            return true;
        }
        return false;
    }

    @Override
    public void addFuel( int fuel )
    {
        if( getWorld().isRemote )
        {
            throw new UnsupportedOperationException();
        }
        int addition = Math.max( fuel, 0 );
        setFuelLevel( getFuelLevel() + addition );
    }

    private int issueCommand( ITurtleCommand command )
    {
        m_commandQueue.offer( new TurtleCommandQueueEntry( ++m_commandsIssued, command ) );
        return m_commandsIssued;
    }

    @Nonnull
    @Override
    @Deprecated
    public Object[] executeCommand( @Nonnull ILuaContext context, @Nonnull ITurtleCommand command ) throws LuaException, InterruptedException
    {
        if( getWorld().isRemote )
        {
            throw new UnsupportedOperationException();
        }

        // Issue command
        int commandID = issueCommand( command );

        // Wait for response
        while( true )
        {
            Object[] response = context.pullEvent( "turtle_response" );
            if( response.length >= 3 && response[ 1 ] instanceof Number && response[ 2 ] instanceof Boolean )
            {
                if( ( (Number) response[ 1 ] ).intValue() == commandID )
                {
                    Object[] returnValues = new Object[ response.length - 2 ];
                    System.arraycopy( response, 2, returnValues, 0, returnValues.length );
                    return returnValues;
                }
            }
        }
    }

    @Nonnull
    @Override
    public MethodResult executeCommand( @Nonnull ITurtleCommand command )
    {
        if( getWorld().isRemote )
        {
            throw new UnsupportedOperationException();
        }

        // Issue command
        int commandID = issueCommand( command );

        // Wait for response
        return MethodResult.pullEvent( "turtle_response", new ILuaFunction()
        {
            @Nonnull
            @Override
            public MethodResult call( Object[] response )
            {
                if( response.length >= 3 && response[ 1 ] instanceof Number && ((Number) response[ 1 ]).intValue() == commandID
                    && response[ 2 ] instanceof Boolean )
                {
                    Object[] returnValues = new Object[ response.length - 2 ];
                    System.arraycopy( response, 2, returnValues, 0, returnValues.length );
                    return MethodResult.of( returnValues );
                }

                return MethodResult.pullEvent( "turtle_response", this );
            }
        } );
    }

    @Override
    public void playAnimation( @Nonnull TurtleAnimation animation )
    {
        if( getWorld().isRemote )
        {
            throw new UnsupportedOperationException();
        }
        m_animation = animation;
        if( m_animation == TurtleAnimation.ShortWait )
        {
            m_animationProgress = ANIM_DURATION / 2;
            m_lastAnimationProgress = ANIM_DURATION / 2;
        }
        else
        {
            m_animationProgress = 0;
            m_lastAnimationProgress = 0;
        }
        m_owner.updateBlock();
    }

    public ResourceLocation getOverlay()
    {
        return m_overlay;
    }

    public void setOverlay( ResourceLocation overlay )
    {
        if( !Objects.equal(m_overlay, overlay) )
        {
            m_overlay = overlay;
            m_owner.updateBlock();
        }
    }

    public int getDyeColour()
    {
        if( m_colourHex == -1 ) return -1;
        Colour colour = Colour.fromHex( m_colourHex );
        return colour == null ? -1 : colour.ordinal();
    }

    public void setDyeColour( int dyeColour )
    {
        int newColour = -1;
        if( dyeColour >= 0 && dyeColour < 16 )
        {
            newColour = Colour.values()[ dyeColour ].getHex();
        }
        if( m_colourHex != newColour )
        {
            m_colourHex = newColour;
            m_owner.updateBlock();
        }
    }

    @Override
    public void setColour( int colour )
    {
        if( colour >= 0 && colour <= 0xFFFFFF )
        {
            if( m_colourHex != colour )
            {
                m_colourHex = colour;
                m_owner.updateBlock();
            }
        }
        else if( m_colourHex != -1 )
        {
            m_colourHex = -1;
            m_owner.updateBlock();
        }
    }

    @Override
    public int getColour()
    {
        return m_colourHex;
    }

    public void setOwningPlayer( GameProfile profile )
    {
        m_owningPlayer = profile;
    }

    @Nonnull
    @Override
    public GameProfile getOwningPlayer()
    {
        return m_owningPlayer;
    }

    @Override
    public ITurtleUpgrade getUpgrade( @Nonnull TurtleSide side )
    {
        if( m_upgrades.containsKey( side ) )
        {
            return m_upgrades.get( side );
        }
        return null;
    }

    @Override
    public void setUpgrade( @Nonnull TurtleSide side, ITurtleUpgrade upgrade )
    {
        // Remove old upgrade
        if( m_upgrades.containsKey( side ) )
        {
            if( m_upgrades.get( side ) == upgrade )
            {
                return;
            }
            m_upgrades.remove( side );
        }
        else
        {
            if( upgrade == null )
            {
                return;
            }
        }
        if( m_upgradeNBTData.containsKey( side ) )
        {
            m_upgradeNBTData.remove( side );
        }

        // Set new upgrade
        if( upgrade != null )
        {
            m_upgrades.put( side, upgrade );
        }

        // Notify clients and create peripherals
        if( m_owner.getWorld() != null )
        {
            updatePeripherals( m_owner.createServerComputer() );
            m_owner.updateBlock();
        }
    }

    @Override
    public IPeripheral getPeripheral( @Nonnull TurtleSide side )
    {
        if( m_peripherals.containsKey( side ) )
        {
            return m_peripherals.get( side );
        }
        return null;
    }

    @Nonnull
    @Override
    public NBTTagCompound getUpgradeNBTData( TurtleSide side )
    {
        if( !m_upgradeNBTData.containsKey( side ) )
        {
            m_upgradeNBTData.put( side, new NBTTagCompound() );
        }
        return m_upgradeNBTData.get( side );
    }

    @Override
    public void updateUpgradeNBTData( @Nonnull TurtleSide side )
    {
        m_owner.updateBlock();
    }

    public boolean saveBlockChange( BlockPos coordinates, IBlockState previousState )
    {
        // Overriden by CCEdu
        return false;
    }

    public Vec3d getRenderOffset( float f )
    {
        switch( m_animation )
        {
            case MoveForward:
            case MoveBack:
            case MoveUp:
            case MoveDown:
            {
                // Get direction
                EnumFacing dir;
                switch( m_animation )
                {
                    case MoveForward:
                    default:
                    {
                        dir = getDirection();
                        break;
                    }
                    case MoveBack:
                    {
                        dir = getDirection().getOpposite();
                        break;
                    }
                    case MoveUp:
                    {
                        dir = EnumFacing.UP;
                        break;
                    }
                    case MoveDown:
                    {
                        dir = EnumFacing.DOWN;
                        break;
                    }
                }

                double distance = -1.0 + getAnimationFraction( f );
                return new Vec3d(
                    distance * dir.getXOffset(),
                    distance * dir.getYOffset(),
                    distance * dir.getZOffset()
                );
            }
            default:
            {
                return new Vec3d( 0.0, 0.0, 0.0 );
            }
        }
    }

    public float getToolRenderAngle( TurtleSide side, float f )
    {
        if( (side == TurtleSide.Left && m_animation == TurtleAnimation.SwingLeftTool) ||
            (side == TurtleSide.Right && m_animation == TurtleAnimation.SwingRightTool) )
        {
            return 45.0f * (float)Math.sin( getAnimationFraction( f ) * Math.PI );
        }
        return 0.0f;
    }

    private int toDirection( TurtleSide side )
    {
        switch( side )
        {
            case Left:
            {
                return 5;
            }
            case Right:
            default:
            {
                return 4;
            }
        }
    }

    public void updatePeripherals( ServerComputer serverComputer )
    {
        if( serverComputer == null )
        {
            // Nothing to do
            return;
        }

        // Update peripherals
        for( TurtleSide side : TurtleSide.values() )
        {
            ITurtleUpgrade upgrade = getUpgrade( side );
            IPeripheral peripheral = null;
            if( upgrade != null && upgrade.getType().isPeripheral() )
            {
                peripheral = upgrade.createPeripheral( this, side );
            }

            int dir = toDirection( side );
            if( peripheral != null )
            {
                if( !m_peripherals.containsKey( side ) )
                {
                    serverComputer.setPeripheral( dir, peripheral );
                    m_peripherals.put( side, peripheral );
                }
                else if( !m_peripherals.get( side ).equals( peripheral ) )
                {
                    serverComputer.setPeripheral( dir, peripheral );
                    m_peripherals.remove( side );
                    m_peripherals.put( side, peripheral );
                }
            }
            else if( m_peripherals.containsKey( side ) )
            {
                serverComputer.setPeripheral( dir, null );
                m_peripherals.remove( side );
            }
        }
    }

    private void updateCommands()
    {
        if( m_animation == TurtleAnimation.None )
        {
            // Pull a new command
            TurtleCommandQueueEntry nextCommand = null;
            if( m_commandQueue.peek() != null )
            {
                nextCommand = m_commandQueue.remove();
            }

            if( nextCommand != null )
            {
                // Execute the command
                TurtleCommandResult result = nextCommand.command.execute( this );

                // Dispatch the callback
                int callbackID = nextCommand.callbackID;
                if( callbackID >= 0 )
                {
                    if( result != null && result.isSuccess() )
                    {
                        IComputer computer = m_owner.getComputer();
                        if( computer != null )
                        {
                            Object[] results = result.getResults();
                            if( results != null )
                            {
                                Object[] arguments = new Object[ results.length + 2 ];
                                arguments[0] = callbackID;
                                arguments[1] = true;
                                System.arraycopy( results, 0, arguments, 2, results.length );
                                computer.queueEvent( "turtle_response", arguments );
                            }
                            else
                            {
                                computer.queueEvent( "turtle_response", new Object[] {
                                    callbackID, true
                                } );
                            }
                        }
                    }
                    else
                    {
                        IComputer computer = m_owner.getComputer();
                        if( computer != null )
                        {
                            computer.queueEvent( "turtle_response", new Object[] {
                                callbackID, false, ( result != null ) ? result.getErrorMessage() : null
                            } );
                        }
                    }
                }
            }
        }
    }

    private void updateAnimation()
    {
        if( m_animation != TurtleAnimation.None )
        {
            World world = this.getWorld();

            if( ComputerCraft.turtlesCanPush )
            {
                // Advance entity pushing
                if( m_animation == TurtleAnimation.MoveForward ||
                    m_animation == TurtleAnimation.MoveBack ||
                    m_animation == TurtleAnimation.MoveUp ||
                    m_animation == TurtleAnimation.MoveDown )
                {
                    BlockPos pos = getPosition();
                    EnumFacing moveDir;
                    switch( m_animation )
                    {
                        case MoveForward:
                        default:
                        {
                            moveDir = m_direction;
                            break;
                        }
                        case MoveBack:
                        {
                            moveDir = m_direction.getOpposite();
                            break;
                        }
                        case MoveUp:
                        {
                            moveDir = EnumFacing.UP;
                            break;
                        }
                        case MoveDown:
                        {
                            moveDir = EnumFacing.DOWN;
                            break;
                        }
                    }

                    double minX = pos.getX();
                    double minY = pos.getY();
                    double minZ = pos.getZ();
                    double maxX = minX + 1.0;
                    double maxY = minY + 1.0;
                    double maxZ = minZ + 1.0;

                    float pushFrac = 1.0f - ((float)(m_animationProgress + 1) / (float)ANIM_DURATION);
                    float push = Math.max( pushFrac + 0.0125f, 0.0f );
                    if (moveDir.getXOffset() < 0)
                    {
                        minX += moveDir.getXOffset() * push;
                    }
                    else
                    {
                        maxX -= moveDir.getXOffset() * push;
                    }

                    if (moveDir.getYOffset() < 0)
                    {
                        minY += moveDir.getYOffset() * push;
                    }
                    else
                    {
                        maxY -= moveDir.getYOffset() * push;
                    }

                    if (moveDir.getZOffset() < 0)
                    {
                        minZ += moveDir.getZOffset() * push;
                    }
                    else
                    {
                        maxZ -= moveDir.getZOffset() * push;
                    }

                    AxisAlignedBB aabb = new AxisAlignedBB( minX, minY, minZ, maxX, maxY, maxZ );
                    List<Entity> list = world.getEntitiesWithinAABBExcludingEntity( null, aabb );
                    if( !list.isEmpty() )
                    {
                        double pushStep = 1.0f / ANIM_DURATION;
                        double pushStepX = moveDir.getXOffset() * pushStep;
                        double pushStepY = moveDir.getYOffset() * pushStep;
                        double pushStepZ = moveDir.getZOffset() * pushStep;
                        for (Entity entity : list)
                        {
                            entity.move( MoverType.PISTON, pushStepX, pushStepY, pushStepZ );
                        }
                    }
                }
            }

            // Advance valentines day easter egg
            if( world.isRemote && m_animation == TurtleAnimation.MoveForward && m_animationProgress == 4 )
            {
                // Spawn love pfx if valentines day
                Holiday currentHoliday = HolidayUtil.getCurrentHoliday();
                if( currentHoliday == Holiday.Valentines )
                {
                    Vec3d position = getVisualPosition( 1.0f );
                    if( position != null )
                    {
                        double x = position.x + world.rand.nextGaussian() * 0.1;
                        double y = position.y + 0.5 + world.rand.nextGaussian() * 0.1;
                        double z = position.z + world.rand.nextGaussian() * 0.1;
                        world.spawnParticle(
                                EnumParticleTypes.HEART, x, y, z,
                                world.rand.nextGaussian() * 0.02,
                                world.rand.nextGaussian() * 0.02,
                                world.rand.nextGaussian() * 0.02
                        );
                    }
                }
            }

            // Wait for anim completion
            m_lastAnimationProgress = m_animationProgress;
            if( ++m_animationProgress >= ANIM_DURATION )
            {
                m_animation = TurtleAnimation.None;
                m_animationProgress = 0;
                m_lastAnimationProgress = 0;
            }
        }
    }

    private float getAnimationFraction( float f )
    {
        float next = (float)m_animationProgress / ANIM_DURATION;
        float previous = (float)m_lastAnimationProgress / ANIM_DURATION;
        return previous + (next - previous) * f;
    }
}

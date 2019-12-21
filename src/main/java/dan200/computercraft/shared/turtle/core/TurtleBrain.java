/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import com.google.common.base.Objects;
import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.computer.blocks.ComputerProxy;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.util.*;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TurtleBrain implements ITurtleAccess
{
    private static final int ANIM_DURATION = 8;

    private TileTurtle m_owner;
    private ComputerProxy m_proxy;
    private GameProfile m_owningPlayer;

    private final IInventory m_inventory = (InventoryDelegate) () -> m_owner;
    private final IItemHandlerModifiable m_inventoryWrapper = new InvWrapper( m_inventory );

    private Queue<TurtleCommandQueueEntry> m_commandQueue = new ArrayDeque<>();
    private int m_commandsIssued = 0;

    private Map<TurtleSide, ITurtleUpgrade> m_upgrades = new EnumMap<>( TurtleSide.class );
    private Map<TurtleSide, IPeripheral> peripherals = new EnumMap<>( TurtleSide.class );
    private Map<TurtleSide, NBTTagCompound> m_upgradeNBTData = new EnumMap<>( TurtleSide.class );

    private int m_selectedSlot = 0;
    private int m_fuelLevel = 0;
    private int m_colourHex = -1;
    private ResourceLocation m_overlay = null;

    private EnumFacing m_direction = EnumFacing.NORTH;
    private TurtleAnimation m_animation = TurtleAnimation.None;
    private int m_animationProgress = 0;
    private int m_lastAnimationProgress = 0;

    TurtlePlayer m_cachedPlayer;

    public TurtleBrain( TileTurtle turtle )
    {
        m_owner = turtle;
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
        if( m_proxy == null )
        {
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

    public void readFromNBT( NBTTagCompound nbt )
    {
        // Read state
        m_direction = EnumFacing.byIndex( nbt.getInteger( "dir" ) );
        m_selectedSlot = nbt.getInteger( "selectedSlot" );
        m_fuelLevel = nbt.hasKey( "fuelLevel" ) ? nbt.getInteger( "fuelLevel" ) : 0;

        // Read owner
        if( nbt.hasKey( "owner", Constants.NBT.TAG_COMPOUND ) )
        {
            NBTTagCompound owner = nbt.getCompoundTag( "owner" );
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
        m_colourHex = ColourUtils.getHexColour( nbt );

        // Read overlay
        if( nbt.hasKey( "overlay_mod" ) )
        {
            String overlayMod = nbt.getString( "overlay_mod" );
            if( nbt.hasKey( "overlay_path" ) )
            {
                String overlayPath = nbt.getString( "overlay_path" );
                m_overlay = new ResourceLocation( overlayMod, overlayPath );
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
        if( nbt.hasKey( "subType" ) )
        {
            // Loading a pre-1.4 world
            int subType = nbt.getInteger( "subType" );
            if( (subType & 0x1) > 0 )
            {
                leftUpgrade = ComputerCraft.TurtleUpgrades.diamondPickaxe;
            }
            if( (subType & 0x2) > 0 )
            {
                rightUpgrade = ComputerCraft.TurtleUpgrades.wirelessModem;
            }
        }
        else
        {
            // Loading a post-1.4 world
            if( nbt.hasKey( "leftUpgrade" ) )
            {
                leftUpgrade = nbt.getTagId( "leftUpgrade" ) == Constants.NBT.TAG_STRING
                    ? TurtleUpgrades.get( nbt.getString( "leftUpgrade" ) )
                    : TurtleUpgrades.get( nbt.getShort( "leftUpgrade" ) );
            }
            if( nbt.hasKey( "rightUpgrade" ) )
            {
                rightUpgrade = nbt.getTagId( "rightUpgrade" ) == Constants.NBT.TAG_STRING
                    ? TurtleUpgrades.get( nbt.getString( "rightUpgrade" ) )
                    : TurtleUpgrades.get( nbt.getShort( "rightUpgrade" ) );
            }
        }
        setUpgrade( TurtleSide.Left, leftUpgrade );
        setUpgrade( TurtleSide.Right, rightUpgrade );

        // NBT
        m_upgradeNBTData.clear();
        if( nbt.hasKey( "leftUpgradeNBT" ) )
        {
            m_upgradeNBTData.put( TurtleSide.Left, nbt.getCompoundTag( "leftUpgradeNBT" ).copy() );
        }
        if( nbt.hasKey( "rightUpgradeNBT" ) )
        {
            m_upgradeNBTData.put( TurtleSide.Right, nbt.getCompoundTag( "rightUpgradeNBT" ).copy() );
        }
    }

    public NBTTagCompound writeToNBT( NBTTagCompound nbt )
    {
        // Write state
        nbt.setInteger( "dir", m_direction.getIndex() );
        nbt.setInteger( "selectedSlot", m_selectedSlot );
        nbt.setInteger( "fuelLevel", m_fuelLevel );

        // Write owner
        if( m_owningPlayer != null )
        {
            NBTTagCompound owner = new NBTTagCompound();
            nbt.setTag( "owner", owner );

            owner.setLong( "upper_id", m_owningPlayer.getId().getMostSignificantBits() );
            owner.setLong( "lower_id", m_owningPlayer.getId().getLeastSignificantBits() );
            owner.setString( "name", m_owningPlayer.getName() );
        }

        // Write upgrades
        String leftUpgradeID = getUpgradeID( getUpgrade( TurtleSide.Left ) );
        if( leftUpgradeID != null ) nbt.setString( "leftUpgrade", leftUpgradeID );
        String rightUpgradeID = getUpgradeID( getUpgrade( TurtleSide.Right ) );
        if( rightUpgradeID != null ) nbt.setString( "rightUpgrade", rightUpgradeID );

        // Write colour
        if( m_colourHex != -1 ) nbt.setInteger( "colour", m_colourHex );

        // Write overlay
        if( m_overlay != null )
        {
            nbt.setString( "overlay_mod", m_overlay.getNamespace() );
            nbt.setString( "overlay_path", m_overlay.getPath() );
        }

        // Write NBT
        if( m_upgradeNBTData.containsKey( TurtleSide.Left ) )
        {
            nbt.setTag( "leftUpgradeNBT", getUpgradeNBTData( TurtleSide.Left ).copy() );
        }
        if( m_upgradeNBTData.containsKey( TurtleSide.Right ) )
        {
            nbt.setTag( "rightUpgradeNBT", getUpgradeNBTData( TurtleSide.Right ).copy() );
        }

        return nbt;
    }

    private static String getUpgradeID( ITurtleUpgrade upgrade )
    {
        return upgrade != null ? upgrade.getUpgradeID().toString() : null;
    }

    public void writeDescription( NBTTagCompound nbt )
    {
        // Upgrades
        String leftUpgradeID = getUpgradeID( getUpgrade( TurtleSide.Left ) );
        if( leftUpgradeID != null ) nbt.setString( "leftUpgrade", leftUpgradeID );
        String rightUpgradeID = getUpgradeID( getUpgrade( TurtleSide.Right ) );
        if( rightUpgradeID != null ) nbt.setString( "rightUpgrade", rightUpgradeID );

        // NBT
        if( m_upgradeNBTData.containsKey( TurtleSide.Left ) )
        {
            nbt.setTag( "leftUpgradeNBT", getUpgradeNBTData( TurtleSide.Left ).copy() );
        }
        if( m_upgradeNBTData.containsKey( TurtleSide.Right ) )
        {
            nbt.setTag( "rightUpgradeNBT", getUpgradeNBTData( TurtleSide.Right ).copy() );
        }

        // Colour
        if( m_colourHex != -1 )
        {
            nbt.setInteger( "colour", m_colourHex );
        }

        // Overlay
        if( m_overlay != null )
        {
            nbt.setString( "overlay_mod", m_overlay.getNamespace() );
            nbt.setString( "overlay_path", m_overlay.getPath() );
        }

        // Animation
        nbt.setInteger( "animation", m_animation.ordinal() );
        nbt.setInteger( "direction", m_direction.getIndex() );
        nbt.setInteger( "fuelLevel", m_fuelLevel );
    }

    public void readDescription( NBTTagCompound nbt )
    {
        // Upgrades
        setUpgrade( TurtleSide.Left, nbt.hasKey( "leftUpgrade" ) ? TurtleUpgrades.get( nbt.getString( "leftUpgrade" ) ) : null );
        setUpgrade( TurtleSide.Right, nbt.hasKey( "rightUpgrade" ) ? TurtleUpgrades.get( nbt.getString( "rightUpgrade" ) ) : null );

        // NBT
        m_upgradeNBTData.clear();
        if( nbt.hasKey( "leftUpgradeNBT" ) )
        {
            m_upgradeNBTData.put( TurtleSide.Left, nbt.getCompoundTag( "leftUpgradeNBT" ).copy() );
        }
        if( nbt.hasKey( "rightUpgradeNBT" ) )
        {
            m_upgradeNBTData.put( TurtleSide.Right, nbt.getCompoundTag( "rightUpgradeNBT" ).copy() );
        }

        // Colour
        m_colourHex = ColourUtils.getHexColour( nbt );

        // Overlay
        if( nbt.hasKey( "overlay_mod" ) && nbt.hasKey( "overlay_path" ) )
        {
            String overlayMod = nbt.getString( "overlay_mod" );
            String overlayPath = nbt.getString( "overlay_path" );
            m_overlay = new ResourceLocation( overlayMod, overlayPath );
        }
        else
        {
            m_overlay = null;
        }

        // Animation
        TurtleAnimation anim = TurtleAnimation.values()[nbt.getInteger( "animation" )];
        if( anim != m_animation &&
            anim != TurtleAnimation.Wait &&
            anim != TurtleAnimation.ShortWait &&
            anim != TurtleAnimation.None )
        {
            m_animation = TurtleAnimation.values()[nbt.getInteger( "animation" )];
            m_animationProgress = 0;
            m_lastAnimationProgress = 0;
        }

        m_direction = EnumFacing.byIndex( nbt.getInteger( "direction" ) );
        m_fuelLevel = nbt.getInteger( "fuelLevel" );
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
            throw new UnsupportedOperationException( "Cannot teleport on the client" );
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
                    if( newTile instanceof TileTurtle )
                    {
                        // Copy the old turtle state into the new turtle
                        TileTurtle newTurtle = (TileTurtle) newTile;
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
        float yaw = getDirection().getHorizontalAngle();
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
        if( getWorld().isRemote ) throw new UnsupportedOperationException( "Cannot set the slot on the client" );

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
        return m_inventory;
    }

    @Nonnull
    @Override
    public IItemHandlerModifiable getItemHandler()
    {
        return m_inventoryWrapper;
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
        if( getWorld().isRemote ) throw new UnsupportedOperationException( "Cannot consume fuel on the client" );

        if( !isFuelNeeded() ) return true;

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
        if( getWorld().isRemote ) throw new UnsupportedOperationException( "Cannot add fuel on the client" );

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
    public Object[] executeCommand( @Nonnull ILuaContext context, @Nonnull ITurtleCommand command ) throws LuaException, InterruptedException
    {
        if( getWorld().isRemote ) throw new UnsupportedOperationException( "Cannot run commands on the client" );

        // Issue command
        int commandID = issueCommand( command );

        // Wait for response
        while( true )
        {
            Object[] response = context.pullEvent( "turtle_response" );
            if( response.length >= 3 && response[1] instanceof Number && response[2] instanceof Boolean )
            {
                if( ((Number) response[1]).intValue() == commandID )
                {
                    Object[] returnValues = new Object[response.length - 2];
                    System.arraycopy( response, 2, returnValues, 0, returnValues.length );
                    return returnValues;
                }
            }
        }
    }

    @Override
    public void playAnimation( @Nonnull TurtleAnimation animation )
    {
        if( getWorld().isRemote ) throw new UnsupportedOperationException( "Cannot play animations on the client" );

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
        if( !Objects.equal( m_overlay, overlay ) )
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
            newColour = Colour.values()[dyeColour].getHex();
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
            if( m_upgrades.get( side ) == upgrade ) return;
            m_upgrades.remove( side );
        }
        else
        {
            if( upgrade == null ) return;
        }
        m_upgradeNBTData.remove( side );

        // Set new upgrade
        if( upgrade != null ) m_upgrades.put( side, upgrade );

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
        return peripherals.get( side );
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
                        dir = getDirection();
                        break;
                    case MoveBack:
                        dir = getDirection().getOpposite();
                        break;
                    case MoveUp:
                        dir = EnumFacing.UP;
                        break;
                    case MoveDown:
                        dir = EnumFacing.DOWN;
                        break;
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
                return Vec3d.ZERO;
            }
        }
    }

    public float getToolRenderAngle( TurtleSide side, float f )
    {
        return (side == TurtleSide.Left && m_animation == TurtleAnimation.SwingLeftTool) ||
            (side == TurtleSide.Right && m_animation == TurtleAnimation.SwingRightTool)
            ? 45.0f * (float) Math.sin( getAnimationFraction( f ) * Math.PI )
            : 0.0f;
    }

    private static ComputerSide toDirection( TurtleSide side )
    {
        switch( side )
        {
            case Left:
                return ComputerSide.LEFT;
            case Right:
            default:
                return ComputerSide.RIGHT;
        }
    }

    private void updatePeripherals( ServerComputer serverComputer )
    {
        if( serverComputer == null ) return;

        // Update peripherals
        for( TurtleSide side : TurtleSide.values() )
        {
            ITurtleUpgrade upgrade = getUpgrade( side );
            IPeripheral peripheral = null;
            if( upgrade != null && upgrade.getType().isPeripheral() )
            {
                peripheral = upgrade.createPeripheral( this, side );
            }

            IPeripheral existing = peripherals.get( side );
            if( existing == peripheral || (existing != null && peripheral != null && existing.equals( peripheral )) )
            {
                // If the peripheral is the same, just use that.
                peripheral = existing;
            }
            else
            {
                // Otherwise update our map
                peripherals.put( side, peripheral );
            }

            // Always update the computer: it may not be the same computer as before!
            serverComputer.setPeripheral( toDirection( side ), peripheral );
        }
    }

    private void updateCommands()
    {
        if( m_animation != TurtleAnimation.None || m_commandQueue.isEmpty() ) return;

        // If we've got a computer, ensure that we're allowed to perform work.
        ServerComputer computer = m_owner.getServerComputer();
        if( computer != null && !computer.getComputer().getMainThreadMonitor().canWork() ) return;

        // Pull a new command
        TurtleCommandQueueEntry nextCommand = m_commandQueue.poll();
        if( nextCommand == null ) return;

        // Execute the command
        long start = System.nanoTime();
        TurtleCommandResult result = nextCommand.command.execute( this );
        long end = System.nanoTime();

        // Dispatch the callback
        if( computer == null ) return;
        computer.getComputer().getMainThreadMonitor().trackWork( end - start, TimeUnit.NANOSECONDS );
        int callbackID = nextCommand.callbackID;
        if( callbackID < 0 ) return;

        if( result != null && result.isSuccess() )
        {
            Object[] results = result.getResults();
            if( results != null )
            {
                Object[] arguments = new Object[results.length + 2];
                arguments[0] = callbackID;
                arguments[1] = true;
                System.arraycopy( results, 0, arguments, 2, results.length );
                computer.queueEvent( "turtle_response", arguments );
            }
            else
            {
                computer.queueEvent( "turtle_response", new Object[] {
                    callbackID, true,
                } );
            }
        }
        else
        {
            computer.queueEvent( "turtle_response", new Object[] {
                callbackID, false, result != null ? result.getErrorMessage() : null,
            } );
        }
    }

    private void updateAnimation()
    {
        if( m_animation != TurtleAnimation.None )
        {
            World world = getWorld();

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
                            moveDir = m_direction;
                            break;
                        case MoveBack:
                            moveDir = m_direction.getOpposite();
                            break;
                        case MoveUp:
                            moveDir = EnumFacing.UP;
                            break;
                        case MoveDown:
                            moveDir = EnumFacing.DOWN;
                            break;
                    }

                    double minX = pos.getX();
                    double minY = pos.getY();
                    double minZ = pos.getZ();
                    double maxX = minX + 1.0;
                    double maxY = minY + 1.0;
                    double maxZ = minZ + 1.0;

                    float pushFrac = 1.0f - (float) (m_animationProgress + 1) / ANIM_DURATION;
                    float push = Math.max( pushFrac + 0.0125f, 0.0f );
                    if( moveDir.getXOffset() < 0 )
                    {
                        minX += moveDir.getXOffset() * push;
                    }
                    else
                    {
                        maxX -= moveDir.getXOffset() * push;
                    }

                    if( moveDir.getYOffset() < 0 )
                    {
                        minY += moveDir.getYOffset() * push;
                    }
                    else
                    {
                        maxY -= moveDir.getYOffset() * push;
                    }

                    if( moveDir.getZOffset() < 0 )
                    {
                        minZ += moveDir.getZOffset() * push;
                    }
                    else
                    {
                        maxZ -= moveDir.getZOffset() * push;
                    }

                    AxisAlignedBB aabb = new AxisAlignedBB( minX, minY, minZ, maxX, maxY, maxZ );
                    List<Entity> list = world.getEntitiesWithinAABB( Entity.class, aabb, EntitySelectors.NOT_SPECTATING );
                    if( !list.isEmpty() )
                    {
                        double pushStep = 1.0f / ANIM_DURATION;
                        double pushStepX = moveDir.getXOffset() * pushStep;
                        double pushStepY = moveDir.getYOffset() * pushStep;
                        double pushStepZ = moveDir.getZOffset() * pushStep;
                        for( Entity entity : list )
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
        float next = (float) m_animationProgress / ANIM_DURATION;
        float previous = (float) m_lastAnimationProgress / ANIM_DURATION;
        return previous + (next - previous) * f;
    }
}

/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import com.google.common.base.Objects;
import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaCallback;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.computer.blocks.ComputerProxy;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import dan200.computercraft.shared.util.InventoryDelegate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.DyeColor;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static dan200.computercraft.shared.common.IColouredItem.NBT_COLOUR;
import static dan200.computercraft.shared.util.WaterloggableHelpers.WATERLOGGED;

public class TurtleBrain implements ITurtleAccess
{
    public static final String NBT_RIGHT_UPGRADE = "RightUpgrade";
    public static final String NBT_RIGHT_UPGRADE_DATA = "RightUpgradeNbt";
    public static final String NBT_LEFT_UPGRADE = "LeftUpgrade";
    public static final String NBT_LEFT_UPGRADE_DATA = "LeftUpgradeNbt";
    public static final String NBT_FUEL = "Fuel";
    public static final String NBT_OVERLAY = "Overlay";

    private static final String NBT_SLOT = "Slot";

    private static final int ANIM_DURATION = 8;

    private TileTurtle m_owner;
    private ComputerProxy m_proxy;
    private GameProfile m_owningPlayer;

    private final IInventory m_inventory = (InventoryDelegate) () -> m_owner;
    private final IItemHandlerModifiable m_inventoryWrapper = new InvWrapper( m_inventory );

    private final Queue<TurtleCommandQueueEntry> m_commandQueue = new ArrayDeque<>();
    private int m_commandsIssued = 0;

    private final Map<TurtleSide, ITurtleUpgrade> m_upgrades = new EnumMap<>( TurtleSide.class );
    private final Map<TurtleSide, IPeripheral> peripherals = new EnumMap<>( TurtleSide.class );
    private final Map<TurtleSide, CompoundNBT> m_upgradeNBTData = new EnumMap<>( TurtleSide.class );

    private int m_selectedSlot = 0;
    private int m_fuelLevel = 0;
    private int m_colourHex = -1;
    private ResourceLocation m_overlay = null;

    private TurtleAnimation m_animation = TurtleAnimation.NONE;
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
        if( m_proxy == null ) m_proxy = new ComputerProxy( () -> m_owner );
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

    /**
     * Read common data for saving and client synchronisation.
     *
     * @param nbt The tag to read from
     */
    private void readCommon( CompoundNBT nbt )
    {
        // Read fields
        m_colourHex = nbt.contains( NBT_COLOUR ) ? nbt.getInt( NBT_COLOUR ) : -1;
        m_fuelLevel = nbt.contains( NBT_FUEL ) ? nbt.getInt( NBT_FUEL ) : 0;
        m_overlay = nbt.contains( NBT_OVERLAY ) ? new ResourceLocation( nbt.getString( NBT_OVERLAY ) ) : null;

        // Read upgrades
        setUpgrade( TurtleSide.LEFT, nbt.contains( NBT_LEFT_UPGRADE ) ? TurtleUpgrades.get( nbt.getString( NBT_LEFT_UPGRADE ) ) : null );
        setUpgrade( TurtleSide.RIGHT, nbt.contains( NBT_RIGHT_UPGRADE ) ? TurtleUpgrades.get( nbt.getString( NBT_RIGHT_UPGRADE ) ) : null );

        // NBT
        m_upgradeNBTData.clear();
        if( nbt.contains( NBT_LEFT_UPGRADE_DATA ) )
        {
            m_upgradeNBTData.put( TurtleSide.LEFT, nbt.getCompound( NBT_LEFT_UPGRADE_DATA ).copy() );
        }
        if( nbt.contains( NBT_RIGHT_UPGRADE_DATA ) )
        {
            m_upgradeNBTData.put( TurtleSide.RIGHT, nbt.getCompound( NBT_RIGHT_UPGRADE_DATA ).copy() );
        }
    }

    private void writeCommon( CompoundNBT nbt )
    {
        nbt.putInt( NBT_FUEL, m_fuelLevel );
        if( m_colourHex != -1 ) nbt.putInt( NBT_COLOUR, m_colourHex );
        if( m_overlay != null ) nbt.putString( NBT_OVERLAY, m_overlay.toString() );

        // Write upgrades
        String leftUpgradeId = getUpgradeId( getUpgrade( TurtleSide.LEFT ) );
        if( leftUpgradeId != null ) nbt.putString( NBT_LEFT_UPGRADE, leftUpgradeId );
        String rightUpgradeId = getUpgradeId( getUpgrade( TurtleSide.RIGHT ) );
        if( rightUpgradeId != null ) nbt.putString( NBT_RIGHT_UPGRADE, rightUpgradeId );

        // Write upgrade NBT
        if( m_upgradeNBTData.containsKey( TurtleSide.LEFT ) )
        {
            nbt.put( NBT_LEFT_UPGRADE_DATA, getUpgradeNBTData( TurtleSide.LEFT ).copy() );
        }
        if( m_upgradeNBTData.containsKey( TurtleSide.RIGHT ) )
        {
            nbt.put( NBT_RIGHT_UPGRADE_DATA, getUpgradeNBTData( TurtleSide.RIGHT ).copy() );
        }
    }

    public void readFromNBT( CompoundNBT nbt )
    {
        readCommon( nbt );

        // Read state
        m_selectedSlot = nbt.getInt( NBT_SLOT );

        // Read owner
        if( nbt.contains( "Owner", Constants.NBT.TAG_COMPOUND ) )
        {
            CompoundNBT owner = nbt.getCompound( "Owner" );
            m_owningPlayer = new GameProfile(
                new UUID( owner.getLong( "UpperId" ), owner.getLong( "LowerId" ) ),
                owner.getString( "Name" )
            );
        }
        else
        {
            m_owningPlayer = null;
        }
    }

    public CompoundNBT writeToNBT( CompoundNBT nbt )
    {
        writeCommon( nbt );

        // Write state
        nbt.putInt( NBT_SLOT, m_selectedSlot );

        // Write owner
        if( m_owningPlayer != null )
        {
            CompoundNBT owner = new CompoundNBT();
            nbt.put( "Owner", owner );

            owner.putLong( "UpperId", m_owningPlayer.getId().getMostSignificantBits() );
            owner.putLong( "LowerId", m_owningPlayer.getId().getLeastSignificantBits() );
            owner.putString( "Name", m_owningPlayer.getName() );
        }

        return nbt;
    }

    private static String getUpgradeId( ITurtleUpgrade upgrade )
    {
        return upgrade != null ? upgrade.getUpgradeID().toString() : null;
    }

    public void readDescription( CompoundNBT nbt )
    {
        readCommon( nbt );

        // Animation
        TurtleAnimation anim = TurtleAnimation.values()[nbt.getInt( "Animation" )];
        if( anim != m_animation &&
            anim != TurtleAnimation.WAIT &&
            anim != TurtleAnimation.SHORT_WAIT &&
            anim != TurtleAnimation.NONE )
        {
            m_animation = anim;
            m_animationProgress = 0;
            m_lastAnimationProgress = 0;
        }
    }

    public void writeDescription( CompoundNBT nbt )
    {
        writeCommon( nbt );
        nbt.putInt( "Animation", m_animation.ordinal() );
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
        BlockState oldBlock = m_owner.getBlockState();

        if( oldWorld == world && oldPos.equals( pos ) )
        {
            // Teleporting to the current position is a no-op
            return true;
        }

        // Ensure the chunk is loaded
        if( !world.isAreaLoaded( pos, 0 ) ) return false;

        // Ensure we're inside the world border
        if( !world.getWorldBorder().contains( pos ) ) return false;

        IFluidState existingFluid = world.getBlockState( pos ).getFluidState();
        BlockState newState = oldBlock
            // We only mark this as waterlogged when travelling into a source block. This prevents us from spreading
            // fluid by creating a new source when moving into a block, causing the next block to be almost full and
            // then moving into that.
            .with( WATERLOGGED, existingFluid.isTagged( FluidTags.WATER ) && existingFluid.isSource() );

        oldOwner.notifyMoveStart();

        try
        {
            // Create a new turtle
            if( world.setBlockState( pos, newState, 0 ) )
            {
                Block block = world.getBlockState( pos ).getBlock();
                if( block == oldBlock.getBlock() )
                {
                    TileEntity newTile = world.getTileEntity( pos );
                    if( newTile instanceof TileTurtle )
                    {
                        // Copy the old turtle state into the new turtle
                        TileTurtle newTurtle = (TileTurtle) newTile;
                        newTurtle.setWorldAndPos( world, pos );
                        newTurtle.transferStateFrom( oldOwner );
                        newTurtle.createServerComputer().setWorld( world );
                        newTurtle.createServerComputer().setPosition( pos );

                        // Remove the old turtle
                        oldWorld.removeBlock( oldPos, false );

                        // Make sure everybody knows about it
                        newTurtle.updateBlock();
                        newTurtle.updateInput();
                        newTurtle.updateOutput();
                        return true;
                    }
                }

                // Something went wrong, remove the newly created turtle
                world.removeBlock( pos, false );
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
            case TURN_LEFT:
            {
                yaw += 90.0f * (1.0f - getAnimationFraction( f ));
                if( yaw >= 360.0f )
                {
                    yaw -= 360.0f;
                }
                break;
            }
            case TURN_RIGHT:
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
    public Direction getDirection()
    {
        return m_owner.getDirection();
    }

    @Override
    public void setDirection( @Nonnull Direction dir )
    {
        m_owner.setDirection( dir );
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
        if( m_owner.getFamily() == ComputerFamily.ADVANCED )
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
    public MethodResult executeCommand( @Nonnull ITurtleCommand command )
    {
        if( getWorld().isRemote ) throw new UnsupportedOperationException( "Cannot run commands on the client" );

        // Issue command
        int commandID = issueCommand( command );
        return new CommandCallback( commandID ).pull;
    }

    @Override
    public void playAnimation( @Nonnull TurtleAnimation animation )
    {
        if( getWorld().isRemote ) throw new UnsupportedOperationException( "Cannot play animations on the client" );

        m_animation = animation;
        if( m_animation == TurtleAnimation.SHORT_WAIT )
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

    public DyeColor getDyeColour()
    {
        if( m_colourHex == -1 ) return null;
        Colour colour = Colour.fromHex( m_colourHex );
        return colour == null ? null : DyeColor.byId( 15 - colour.ordinal() );
    }

    public void setDyeColour( DyeColor dyeColour )
    {
        int newColour = -1;
        if( dyeColour != null )
        {
            newColour = Colour.values()[15 - dyeColour.getId()].getHex();
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

    @Nullable
    @Override
    public GameProfile getOwningPlayer()
    {
        return m_owningPlayer;
    }

    @Override
    public ITurtleUpgrade getUpgrade( @Nonnull TurtleSide side )
    {
        return m_upgrades.get( side );
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
    public CompoundNBT getUpgradeNBTData( TurtleSide side )
    {
        CompoundNBT nbt = m_upgradeNBTData.get( side );
        if( nbt == null ) m_upgradeNBTData.put( side, nbt = new CompoundNBT() );
        return nbt;
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
            case MOVE_FORWARD:
            case MOVE_BACK:
            case MOVE_UP:
            case MOVE_DOWN:
            {
                // Get direction
                Direction dir;
                switch( m_animation )
                {
                    case MOVE_FORWARD:
                    default:
                        dir = getDirection();
                        break;
                    case MOVE_BACK:
                        dir = getDirection().getOpposite();
                        break;
                    case MOVE_UP:
                        dir = Direction.UP;
                        break;
                    case MOVE_DOWN:
                        dir = Direction.DOWN;
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
        return (side == TurtleSide.LEFT && m_animation == TurtleAnimation.SWING_LEFT_TOOL) ||
            (side == TurtleSide.RIGHT && m_animation == TurtleAnimation.SWING_RIGHT_TOOL)
            ? 45.0f * (float) Math.sin( getAnimationFraction( f ) * Math.PI )
            : 0.0f;
    }

    private static ComputerSide toDirection( TurtleSide side )
    {
        switch( side )
        {
            case LEFT:
                return ComputerSide.LEFT;
            case RIGHT:
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
        if( m_animation != TurtleAnimation.NONE || m_commandQueue.isEmpty() ) return;

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
        if( m_animation != TurtleAnimation.NONE )
        {
            World world = getWorld();

            if( ComputerCraft.turtlesCanPush )
            {
                // Advance entity pushing
                if( m_animation == TurtleAnimation.MOVE_FORWARD ||
                    m_animation == TurtleAnimation.MOVE_BACK ||
                    m_animation == TurtleAnimation.MOVE_UP ||
                    m_animation == TurtleAnimation.MOVE_DOWN )
                {
                    BlockPos pos = getPosition();
                    Direction moveDir;
                    switch( m_animation )
                    {
                        case MOVE_FORWARD:
                        default:
                            moveDir = getDirection();
                            break;
                        case MOVE_BACK:
                            moveDir = getDirection().getOpposite();
                            break;
                        case MOVE_UP:
                            moveDir = Direction.UP;
                            break;
                        case MOVE_DOWN:
                            moveDir = Direction.DOWN;
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
                    List<Entity> list = world.getEntitiesWithinAABB( Entity.class, aabb, EntityPredicates.NOT_SPECTATING );
                    if( !list.isEmpty() )
                    {
                        double pushStep = 1.0f / ANIM_DURATION;
                        double pushStepX = moveDir.getXOffset() * pushStep;
                        double pushStepY = moveDir.getYOffset() * pushStep;
                        double pushStepZ = moveDir.getZOffset() * pushStep;
                        for( Entity entity : list )
                        {
                            entity.move( MoverType.PISTON, new Vec3d( pushStepX, pushStepY, pushStepZ ) );
                        }
                    }
                }
            }

            // Advance valentines day easter egg
            if( world.isRemote && m_animation == TurtleAnimation.MOVE_FORWARD && m_animationProgress == 4 )
            {
                // Spawn love pfx if valentines day
                Holiday currentHoliday = HolidayUtil.getCurrentHoliday();
                if( currentHoliday == Holiday.VALENTINES )
                {
                    Vec3d position = getVisualPosition( 1.0f );
                    if( position != null )
                    {
                        double x = position.x + world.rand.nextGaussian() * 0.1;
                        double y = position.y + 0.5 + world.rand.nextGaussian() * 0.1;
                        double z = position.z + world.rand.nextGaussian() * 0.1;
                        world.addParticle(
                            ParticleTypes.HEART, x, y, z,
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
                m_animation = TurtleAnimation.NONE;
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

    private static final class CommandCallback implements ILuaCallback
    {
        final MethodResult pull = MethodResult.pullEvent( "turtle_response", this );
        private final int command;

        CommandCallback( int command )
        {
            this.command = command;
        }

        @Nonnull
        @Override
        public MethodResult resume( Object[] response )
        {
            if( response.length < 3 || !(response[1] instanceof Number) || !(response[2] instanceof Boolean) )
            {
                return pull;
            }

            if( ((Number) response[1]).intValue() != command ) return pull;

            return MethodResult.of( Arrays.copyOfRange( response, 2, response.length ) );
        }
    }
}

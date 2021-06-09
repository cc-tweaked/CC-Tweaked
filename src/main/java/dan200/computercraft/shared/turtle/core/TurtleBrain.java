/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
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
import dan200.computercraft.shared.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
    private final Queue<TurtleCommandQueueEntry> commandQueue = new ArrayDeque<>();
    private final Map<TurtleSide, ITurtleUpgrade> upgrades = new EnumMap<>( TurtleSide.class );
    private final Map<TurtleSide, IPeripheral> peripherals = new EnumMap<>( TurtleSide.class );
    private final Map<TurtleSide, CompoundTag> upgradeNBTData = new EnumMap<>( TurtleSide.class );
    TurtlePlayer cachedPlayer;
    private TileTurtle owner;
    private final Inventory inventory = (InventoryDelegate) () -> this.owner;
    private ComputerProxy proxy;
    private GameProfile owningPlayer;
    private int commandsIssued = 0;
    private int selectedSlot = 0;
    private int fuelLevel = 0;
    private int colourHex = -1;
    private Identifier overlay = null;
    private TurtleAnimation animation = TurtleAnimation.NONE;
    private int animationProgress = 0;
    private int lastAnimationProgress = 0;

    public TurtleBrain( TileTurtle turtle )
    {
        this.owner = turtle;
    }

    public TileTurtle getOwner()
    {
        return this.owner;
    }

    public void setOwner( TileTurtle owner )
    {
        this.owner = owner;
    }

    public ComputerProxy getProxy()
    {
        if( this.proxy == null )
        {
            this.proxy = new ComputerProxy( () -> this.owner );
        }
        return this.proxy;
    }

    public ComputerFamily getFamily()
    {
        return this.owner.getFamily();
    }

    public void setupComputer( ServerComputer computer )
    {
        this.updatePeripherals( computer );
    }

    private void updatePeripherals( ServerComputer serverComputer )
    {
        if( serverComputer == null )
        {
            return;
        }

        // Update peripherals
        for( TurtleSide side : TurtleSide.values() )
        {
            ITurtleUpgrade upgrade = this.getUpgrade( side );
            IPeripheral peripheral = null;
            if( upgrade != null && upgrade.getType()
                .isPeripheral() )
            {
                peripheral = upgrade.createPeripheral( this, side );
            }

            IPeripheral existing = this.peripherals.get( side );
            if( existing == peripheral || (existing != null && peripheral != null && existing.equals( peripheral )) )
            {
                // If the peripheral is the same, just use that.
                peripheral = existing;
            }
            else
            {
                // Otherwise update our map
                this.peripherals.put( side, peripheral );
            }

            // Always update the computer: it may not be the same computer as before!
            serverComputer.setPeripheral( toDirection( side ), peripheral );
        }
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

    public void update()
    {
        World world = this.getWorld();
        if( !world.isClient )
        {
            // Advance movement
            this.updateCommands();

            // The block may have been broken while the command was executing (for instance, if a block explodes
            // when being mined). If so, abort.
            if( owner.isRemoved() ) return;
        }

        // Advance animation
        this.updateAnimation();

        // Advance upgrades
        if( !this.upgrades.isEmpty() )
        {
            for( Map.Entry<TurtleSide, ITurtleUpgrade> entry : this.upgrades.entrySet() )
            {
                entry.getValue()
                    .update( this, entry.getKey() );
            }
        }
    }

    @Nonnull
    @Override
    public World getWorld()
    {
        return this.owner.getWorld();
    }

    @Nonnull
    @Override
    public BlockPos getPosition()
    {
        return this.owner.getPos();
    }

    @Override
    public boolean teleportTo( @Nonnull World world, @Nonnull BlockPos pos )
    {
        if( world.isClient || this.getWorld().isClient )
        {
            throw new UnsupportedOperationException( "Cannot teleport on the client" );
        }

        // Cache info about the old turtle (so we don't access this after we delete ourselves)
        World oldWorld = this.getWorld();
        TileTurtle oldOwner = this.owner;
        BlockPos oldPos = this.owner.getPos();
        BlockState oldBlock = this.owner.getCachedState();

        if( oldWorld == world && oldPos.equals( pos ) )
        {
            // Teleporting to the current position is a no-op
            return true;
        }

        // Ensure the chunk is loaded
        if( !world.isChunkLoaded( pos ) )
        {
            return false;
        }

        // Ensure we're inside the world border
        if( !world.getWorldBorder()
            .contains( pos ) )
        {
            return false;
        }

        FluidState existingFluid = world.getBlockState( pos )
            .getFluidState();
        BlockState newState = oldBlock
            // We only mark this as waterlogged when travelling into a source block. This prevents us from spreading
            // fluid by creating a new source when moving into a block, causing the next block to be almost full and
            // then moving into that.
            .with( WATERLOGGED, existingFluid.isIn( FluidTags.WATER ) && existingFluid.isStill() );

        oldOwner.notifyMoveStart();

        try
        {
            // Create a new turtle
            if( world.setBlockState( pos, newState, 0 ) )
            {
                Block block = world.getBlockState( pos )
                    .getBlock();
                if( block == oldBlock.getBlock() )
                {
                    BlockEntity newTile = world.getBlockEntity( pos );
                    if( newTile instanceof TileTurtle )
                    {
                        // Copy the old turtle state into the new turtle
                        TileTurtle newTurtle = (TileTurtle) newTile;
                        newTurtle.setLocation( world, pos );
                        newTurtle.transferStateFrom( oldOwner );
                        newTurtle.createServerComputer()
                            .setWorld( world );
                        newTurtle.createServerComputer()
                            .setPosition( pos );

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
        Vec3d offset = this.getRenderOffset( f );
        BlockPos pos = this.owner.getPos();
        return new Vec3d( pos.getX() + 0.5 + offset.x, pos.getY() + 0.5 + offset.y, pos.getZ() + 0.5 + offset.z );
    }

    @Override
    public float getVisualYaw( float f )
    {
        float yaw = this.getDirection().asRotation();
        switch( this.animation )
        {
            case TURN_LEFT:
            {
                yaw += 90.0f * (1.0f - this.getAnimationFraction( f ));
                if( yaw >= 360.0f )
                {
                    yaw -= 360.0f;
                }
                break;
            }
            case TURN_RIGHT:
            {
                yaw += -90.0f * (1.0f - this.getAnimationFraction( f ));
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
        return this.owner.getDirection();
    }

    @Override
    public void setDirection( @Nonnull Direction dir )
    {
        this.owner.setDirection( dir );
    }

    @Override
    public int getSelectedSlot()
    {
        return this.selectedSlot;
    }

    @Override
    public void setSelectedSlot( int slot )
    {
        if( this.getWorld().isClient )
        {
            throw new UnsupportedOperationException( "Cannot set the slot on the client" );
        }

        if( slot >= 0 && slot < this.owner.size() )
        {
            this.selectedSlot = slot;
            this.owner.onTileEntityChange();
        }
    }

    @Override
    public int getColour()
    {
        return this.colourHex;
    }

    @Override
    public void setColour( int colour )
    {
        if( colour >= 0 && colour <= 0xFFFFFF )
        {
            if( this.colourHex != colour )
            {
                this.colourHex = colour;
                this.owner.updateBlock();
            }
        }
        else if( this.colourHex != -1 )
        {
            this.colourHex = -1;
            this.owner.updateBlock();
        }
    }

    @Nullable
    @Override
    public GameProfile getOwningPlayer()
    {
        return this.owningPlayer;
    }

    @Override
    public boolean isFuelNeeded()
    {
        return ComputerCraft.turtlesNeedFuel;
    }

    @Override
    public int getFuelLevel()
    {
        return Math.min( this.fuelLevel, this.getFuelLimit() );
    }

    @Override
    public void setFuelLevel( int level )
    {
        this.fuelLevel = Math.min( level, this.getFuelLimit() );
        this.owner.onTileEntityChange();
    }

    @Override
    public int getFuelLimit()
    {
        if( this.owner.getFamily() == ComputerFamily.ADVANCED )
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
        if( this.getWorld().isClient )
        {
            throw new UnsupportedOperationException( "Cannot consume fuel on the client" );
        }

        if( !this.isFuelNeeded() )
        {
            return true;
        }

        int consumption = Math.max( fuel, 0 );
        if( this.getFuelLevel() >= consumption )
        {
            this.setFuelLevel( this.getFuelLevel() - consumption );
            return true;
        }
        return false;
    }

    @Override
    public void addFuel( int fuel )
    {
        if( this.getWorld().isClient )
        {
            throw new UnsupportedOperationException( "Cannot add fuel on the client" );
        }

        int addition = Math.max( fuel, 0 );
        this.setFuelLevel( this.getFuelLevel() + addition );
    }

    @Nonnull
    @Override
    public MethodResult executeCommand( @Nonnull ITurtleCommand command )
    {
        if( this.getWorld().isClient )
        {
            throw new UnsupportedOperationException( "Cannot run commands on the client" );
        }

        // Issue command
        int commandID = this.issueCommand( command );
        return new CommandCallback( commandID ).pull;
    }

    private int issueCommand( ITurtleCommand command )
    {
        this.commandQueue.offer( new TurtleCommandQueueEntry( ++this.commandsIssued, command ) );
        return this.commandsIssued;
    }

    @Override
    public void playAnimation( @Nonnull TurtleAnimation animation )
    {
        if( this.getWorld().isClient )
        {
            throw new UnsupportedOperationException( "Cannot play animations on the client" );
        }

        this.animation = animation;
        if( this.animation == TurtleAnimation.SHORT_WAIT )
        {
            this.animationProgress = ANIM_DURATION / 2;
            this.lastAnimationProgress = ANIM_DURATION / 2;
        }
        else
        {
            this.animationProgress = 0;
            this.lastAnimationProgress = 0;
        }
        this.owner.updateBlock();
    }

    @Override
    public ITurtleUpgrade getUpgrade( @Nonnull TurtleSide side )
    {
        return this.upgrades.get( side );
    }

    @Override
    public void setUpgrade( @Nonnull TurtleSide side, ITurtleUpgrade upgrade )
    {
        // Remove old upgrade
        if( this.upgrades.containsKey( side ) )
        {
            if( this.upgrades.get( side ) == upgrade )
            {
                return;
            }
            this.upgrades.remove( side );
        }
        else
        {
            if( upgrade == null )
            {
                return;
            }
        }

        this.upgradeNBTData.remove( side );

        // Set new upgrade
        if( upgrade != null )
        {
            this.upgrades.put( side, upgrade );
        }

        // Notify clients and create peripherals
        if( this.owner.getWorld() != null )
        {
            this.updatePeripherals( this.owner.createServerComputer() );
            this.owner.updateBlock();
        }
    }

    @Override
    public IPeripheral getPeripheral( @Nonnull TurtleSide side )
    {
        return this.peripherals.get( side );
    }

    @Nonnull
    @Override
    public CompoundTag getUpgradeNBTData( TurtleSide side )
    {
        CompoundTag nbt = this.upgradeNBTData.get( side );
        if( nbt == null )
        {
            this.upgradeNBTData.put( side, nbt = new CompoundTag() );
        }
        return nbt;
    }

    @Override
    public void updateUpgradeNBTData( @Nonnull TurtleSide side )
    {
        this.owner.updateBlock();
    }

    @Nonnull
    @Override
    public Inventory getInventory()
    {
        return this.inventory;
    }

    public void setOwningPlayer( GameProfile profile )
    {
        this.owningPlayer = profile;
    }

    private void updateCommands()
    {
        if( this.animation != TurtleAnimation.NONE || this.commandQueue.isEmpty() )
        {
            return;
        }

        // If we've got a computer, ensure that we're allowed to perform work.
        ServerComputer computer = this.owner.getServerComputer();
        if( computer != null && !computer.getComputer()
            .getMainThreadMonitor()
            .canWork() )
        {
            return;
        }

        // Pull a new command
        TurtleCommandQueueEntry nextCommand = this.commandQueue.poll();
        if( nextCommand == null )
        {
            return;
        }

        // Execute the command
        long start = System.nanoTime();
        TurtleCommandResult result = nextCommand.command.execute( this );
        long end = System.nanoTime();

        // Dispatch the callback
        if( computer == null )
        {
            return;
        }
        computer.getComputer()
            .getMainThreadMonitor()
            .trackWork( end - start, TimeUnit.NANOSECONDS );
        int callbackID = nextCommand.callbackID;
        if( callbackID < 0 )
        {
            return;
        }

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
                    callbackID,
                    true,
                } );
            }
        }
        else
        {
            computer.queueEvent( "turtle_response", new Object[] {
                callbackID,
                false,
                result != null ? result.getErrorMessage() : null,
            } );
        }
    }

    private void updateAnimation()
    {
        if( this.animation != TurtleAnimation.NONE )
        {
            World world = this.getWorld();

            if( ComputerCraft.turtlesCanPush )
            {
                // Advance entity pushing
                if( this.animation == TurtleAnimation.MOVE_FORWARD || this.animation == TurtleAnimation.MOVE_BACK || this.animation == TurtleAnimation.MOVE_UP || this.animation == TurtleAnimation.MOVE_DOWN )
                {
                    BlockPos pos = this.getPosition();
                    Direction moveDir;
                    switch( this.animation )
                    {
                        case MOVE_FORWARD:
                        default:
                            moveDir = this.getDirection();
                            break;
                        case MOVE_BACK:
                            moveDir = this.getDirection().getOpposite();
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

                    float pushFrac = 1.0f - (float) (this.animationProgress + 1) / ANIM_DURATION;
                    float push = Math.max( pushFrac + 0.0125f, 0.0f );
                    if( moveDir.getOffsetX() < 0 )
                    {
                        minX += moveDir.getOffsetX() * push;
                    }
                    else
                    {
                        maxX -= moveDir.getOffsetX() * push;
                    }

                    if( moveDir.getOffsetY() < 0 )
                    {
                        minY += moveDir.getOffsetY() * push;
                    }
                    else
                    {
                        maxY -= moveDir.getOffsetY() * push;
                    }

                    if( moveDir.getOffsetZ() < 0 )
                    {
                        minZ += moveDir.getOffsetZ() * push;
                    }
                    else
                    {
                        maxZ -= moveDir.getOffsetZ() * push;
                    }

                    Box aabb = new Box( minX, minY, minZ, maxX, maxY, maxZ );
                    List<Entity> list = world.getEntitiesByClass( Entity.class, aabb, EntityPredicates.EXCEPT_SPECTATOR );
                    if( !list.isEmpty() )
                    {
                        double pushStep = 1.0f / ANIM_DURATION;
                        double pushStepX = moveDir.getOffsetX() * pushStep;
                        double pushStepY = moveDir.getOffsetY() * pushStep;
                        double pushStepZ = moveDir.getOffsetZ() * pushStep;
                        for( Entity entity : list )
                        {
                            entity.move( MovementType.PISTON, new Vec3d( pushStepX, pushStepY, pushStepZ ) );
                        }
                    }
                }
            }

            // Advance valentines day easter egg
            if( world.isClient && this.animation == TurtleAnimation.MOVE_FORWARD && this.animationProgress == 4 )
            {
                // Spawn love pfx if valentines day
                Holiday currentHoliday = HolidayUtil.getCurrentHoliday();
                if( currentHoliday == Holiday.VALENTINES )
                {
                    Vec3d position = this.getVisualPosition( 1.0f );
                    if( position != null )
                    {
                        double x = position.x + world.random.nextGaussian() * 0.1;
                        double y = position.y + 0.5 + world.random.nextGaussian() * 0.1;
                        double z = position.z + world.random.nextGaussian() * 0.1;
                        world.addParticle( ParticleTypes.HEART,
                            x,
                            y,
                            z,
                            world.random.nextGaussian() * 0.02,
                            world.random.nextGaussian() * 0.02,
                            world.random.nextGaussian() * 0.02 );
                    }
                }
            }

            // Wait for anim completion
            this.lastAnimationProgress = this.animationProgress;
            if( ++this.animationProgress >= ANIM_DURATION )
            {
                this.animation = TurtleAnimation.NONE;
                this.animationProgress = 0;
                this.lastAnimationProgress = 0;
            }
        }
    }

    public Vec3d getRenderOffset( float f )
    {
        switch( this.animation )
        {
            case MOVE_FORWARD:
            case MOVE_BACK:
            case MOVE_UP:
            case MOVE_DOWN:
            {
                // Get direction
                Direction dir;
                switch( this.animation )
                {
                    case MOVE_FORWARD:
                    default:
                        dir = this.getDirection();
                        break;
                    case MOVE_BACK:
                        dir = this.getDirection().getOpposite();
                        break;
                    case MOVE_UP:
                        dir = Direction.UP;
                        break;
                    case MOVE_DOWN:
                        dir = Direction.DOWN;
                        break;
                }

                double distance = -1.0 + this.getAnimationFraction( f );
                return new Vec3d( distance * dir.getOffsetX(), distance * dir.getOffsetY(), distance * dir.getOffsetZ() );
            }
            default:
            {
                return Vec3d.ZERO;
            }
        }
    }

    private float getAnimationFraction( float f )
    {
        float next = (float) this.animationProgress / ANIM_DURATION;
        float previous = (float) this.lastAnimationProgress / ANIM_DURATION;
        return previous + (next - previous) * f;
    }

    public void readFromNBT( CompoundTag nbt )
    {
        this.readCommon( nbt );

        // Read state
        this.selectedSlot = nbt.getInt( NBT_SLOT );

        // Read owner
        if( nbt.contains( "Owner", NBTUtil.TAG_COMPOUND ) )
        {
            CompoundTag owner = nbt.getCompound( "Owner" );
            this.owningPlayer = new GameProfile( new UUID( owner.getLong( "UpperId" ), owner.getLong( "LowerId" ) ), owner.getString( "Name" ) );
        }
        else
        {
            this.owningPlayer = null;
        }
    }

    /**
     * Read common data for saving and client synchronisation.
     *
     * @param nbt The tag to read from
     */
    private void readCommon( CompoundTag nbt )
    {
        // Read fields
        this.colourHex = nbt.contains( NBT_COLOUR ) ? nbt.getInt( NBT_COLOUR ) : -1;
        this.fuelLevel = nbt.contains( NBT_FUEL ) ? nbt.getInt( NBT_FUEL ) : 0;
        this.overlay = nbt.contains( NBT_OVERLAY ) ? new Identifier( nbt.getString( NBT_OVERLAY ) ) : null;

        // Read upgrades
        this.setUpgrade( TurtleSide.LEFT, nbt.contains( NBT_LEFT_UPGRADE ) ? TurtleUpgrades.get( nbt.getString( NBT_LEFT_UPGRADE ) ) : null );
        this.setUpgrade( TurtleSide.RIGHT, nbt.contains( NBT_RIGHT_UPGRADE ) ? TurtleUpgrades.get( nbt.getString( NBT_RIGHT_UPGRADE ) ) : null );

        // NBT
        this.upgradeNBTData.clear();
        if( nbt.contains( NBT_LEFT_UPGRADE_DATA ) )
        {
            this.upgradeNBTData.put( TurtleSide.LEFT,
                nbt.getCompound( NBT_LEFT_UPGRADE_DATA )
                    .copy() );
        }
        if( nbt.contains( NBT_RIGHT_UPGRADE_DATA ) )
        {
            this.upgradeNBTData.put( TurtleSide.RIGHT,
                nbt.getCompound( NBT_RIGHT_UPGRADE_DATA )
                    .copy() );
        }
    }

    public CompoundTag writeToNBT( CompoundTag nbt )
    {
        this.writeCommon( nbt );

        // Write state
        nbt.putInt( NBT_SLOT, this.selectedSlot );

        // Write owner
        if( this.owningPlayer != null )
        {
            CompoundTag owner = new CompoundTag();
            nbt.put( "Owner", owner );

            owner.putLong( "UpperId", this.owningPlayer.getId()
                .getMostSignificantBits() );
            owner.putLong( "LowerId", this.owningPlayer.getId()
                .getLeastSignificantBits() );
            owner.putString( "Name", this.owningPlayer.getName() );
        }

        return nbt;
    }

    private void writeCommon( CompoundTag nbt )
    {
        nbt.putInt( NBT_FUEL, this.fuelLevel );
        if( this.colourHex != -1 )
        {
            nbt.putInt( NBT_COLOUR, this.colourHex );
        }
        if( this.overlay != null )
        {
            nbt.putString( NBT_OVERLAY, this.overlay.toString() );
        }

        // Write upgrades
        String leftUpgradeId = getUpgradeId( this.getUpgrade( TurtleSide.LEFT ) );
        if( leftUpgradeId != null )
        {
            nbt.putString( NBT_LEFT_UPGRADE, leftUpgradeId );
        }
        String rightUpgradeId = getUpgradeId( this.getUpgrade( TurtleSide.RIGHT ) );
        if( rightUpgradeId != null )
        {
            nbt.putString( NBT_RIGHT_UPGRADE, rightUpgradeId );
        }

        // Write upgrade NBT
        if( this.upgradeNBTData.containsKey( TurtleSide.LEFT ) )
        {
            nbt.put( NBT_LEFT_UPGRADE_DATA,
                this.getUpgradeNBTData( TurtleSide.LEFT ).copy() );
        }
        if( this.upgradeNBTData.containsKey( TurtleSide.RIGHT ) )
        {
            nbt.put( NBT_RIGHT_UPGRADE_DATA,
                this.getUpgradeNBTData( TurtleSide.RIGHT ).copy() );
        }
    }

    private static String getUpgradeId( ITurtleUpgrade upgrade )
    {
        return upgrade != null ? upgrade.getUpgradeID()
            .toString() : null;
    }

    public void readDescription( CompoundTag nbt )
    {
        this.readCommon( nbt );

        // Animation
        TurtleAnimation anim = TurtleAnimation.values()[nbt.getInt( "Animation" )];
        if( anim != this.animation && anim != TurtleAnimation.WAIT && anim != TurtleAnimation.SHORT_WAIT && anim != TurtleAnimation.NONE )
        {
            this.animation = anim;
            this.animationProgress = 0;
            this.lastAnimationProgress = 0;
        }
    }

    public void writeDescription( CompoundTag nbt )
    {
        this.writeCommon( nbt );
        nbt.putInt( "Animation", this.animation.ordinal() );
    }

    public Identifier getOverlay()
    {
        return this.overlay;
    }

    public void setOverlay( Identifier overlay )
    {
        if( !Objects.equal( this.overlay, overlay ) )
        {
            this.overlay = overlay;
            this.owner.updateBlock();
        }
    }

    public DyeColor getDyeColour()
    {
        if( this.colourHex == -1 )
        {
            return null;
        }
        Colour colour = Colour.fromHex( this.colourHex );
        return colour == null ? null : DyeColor.byId( 15 - colour.ordinal() );
    }

    public void setDyeColour( DyeColor dyeColour )
    {
        int newColour = -1;
        if( dyeColour != null )
        {
            newColour = Colour.values()[15 - dyeColour.getId()].getHex();
        }
        if( this.colourHex != newColour )
        {
            this.colourHex = newColour;
            this.owner.updateBlock();
        }
    }

    public float getToolRenderAngle( TurtleSide side, float f )
    {
        return (side == TurtleSide.LEFT && this.animation == TurtleAnimation.SWING_LEFT_TOOL) || (side == TurtleSide.RIGHT && this.animation == TurtleAnimation.SWING_RIGHT_TOOL) ? 45.0f * (float) Math.sin(
            this.getAnimationFraction( f ) * Math.PI ) : 0.0f;
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
                return this.pull;
            }

            if( ((Number) response[1]).intValue() != this.command )
            {
                return this.pull;
            }

            return MethodResult.of( Arrays.copyOfRange( response, 2, response.length ) );
        }
    }
}

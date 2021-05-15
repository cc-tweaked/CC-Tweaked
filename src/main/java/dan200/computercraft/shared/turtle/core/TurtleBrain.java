/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import static dan200.computercraft.shared.common.IColouredItem.NBT_COLOUR;
import static dan200.computercraft.shared.util.WaterloggableHelpers.WATERLOGGED;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaCallback;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
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
import dan200.computercraft.shared.util.NBTUtil;

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

public class TurtleBrain implements ITurtleAccess {
    public static final String NBT_RIGHT_UPGRADE = "RightUpgrade";
    public static final String NBT_RIGHT_UPGRADE_DATA = "RightUpgradeNbt";
    public static final String NBT_LEFT_UPGRADE = "LeftUpgrade";
    public static final String NBT_LEFT_UPGRADE_DATA = "LeftUpgradeNbt";
    public static final String NBT_FUEL = "Fuel";
    public static final String NBT_OVERLAY = "Overlay";

    private static final String NBT_SLOT = "Slot";

    private static final int ANIM_DURATION = 8;
    private final Queue<TurtleCommandQueueEntry> m_commandQueue = new ArrayDeque<>();
    private final Map<TurtleSide, ITurtleUpgrade> m_upgrades = new EnumMap<>(TurtleSide.class);
    private final Map<TurtleSide, IPeripheral> peripherals = new EnumMap<>(TurtleSide.class);
    private final Map<TurtleSide, CompoundTag> m_upgradeNBTData = new EnumMap<>(TurtleSide.class);
    TurtlePlayer m_cachedPlayer;
    private TileTurtle m_owner;
    private final Inventory m_inventory = (InventoryDelegate) () -> this.m_owner;
    private ComputerProxy m_proxy;
    private GameProfile m_owningPlayer;
    private int m_commandsIssued = 0;
    private int m_selectedSlot = 0;
    private int m_fuelLevel = 0;
    private int m_colourHex = -1;
    private Identifier m_overlay = null;
    private TurtleAnimation m_animation = TurtleAnimation.NONE;
    private int m_animationProgress = 0;
    private int m_lastAnimationProgress = 0;

    public TurtleBrain(TileTurtle turtle) {
        this.m_owner = turtle;
    }

    public TileTurtle getOwner() {
        return this.m_owner;
    }

    public void setOwner(TileTurtle owner) {
        this.m_owner = owner;
    }

    public ComputerProxy getProxy() {
        if (this.m_proxy == null) {
            this.m_proxy = new ComputerProxy(() -> this.m_owner);
        }
        return this.m_proxy;
    }

    public ComputerFamily getFamily() {
        return this.m_owner.getFamily();
    }

    public void setupComputer(ServerComputer computer) {
        this.updatePeripherals(computer);
    }

    private void updatePeripherals(ServerComputer serverComputer) {
        if (serverComputer == null) {
            return;
        }

        // Update peripherals
        for (TurtleSide side : TurtleSide.values()) {
            ITurtleUpgrade upgrade = this.getUpgrade(side);
            IPeripheral peripheral = null;
            if (upgrade != null && upgrade.getType()
                                          .isPeripheral()) {
                peripheral = upgrade.createPeripheral(this, side);
            }

            IPeripheral existing = this.peripherals.get(side);
            if (existing == peripheral || (existing != null && peripheral != null && existing.equals(peripheral))) {
                // If the peripheral is the same, just use that.
                peripheral = existing;
            } else {
                // Otherwise update our map
                this.peripherals.put(side, peripheral);
            }

            // Always update the computer: it may not be the same computer as before!
            serverComputer.setPeripheral(toDirection(side), peripheral);
        }
    }

    private static ComputerSide toDirection(TurtleSide side) {
        switch (side) {
        case LEFT:
            return ComputerSide.LEFT;
        case RIGHT:
        default:
            return ComputerSide.RIGHT;
        }
    }

    public void update() {
        World world = this.getWorld();
        if (!world.isClient) {
            // Advance movement
            this.updateCommands();

            // The block may have been broken while the command was executing (for instance, if a block explodes
            // when being mined). If so, abort.
            if( m_owner.isRemoved() ) return;
        }

        // Advance animation
        this.updateAnimation();

        // Advance upgrades
        if (!this.m_upgrades.isEmpty()) {
            for (Map.Entry<TurtleSide, ITurtleUpgrade> entry : this.m_upgrades.entrySet()) {
                entry.getValue()
                     .update(this, entry.getKey());
            }
        }
    }

    @Nonnull
    @Override
    public World getWorld() {
        return this.m_owner.getWorld();
    }

    @Nonnull
    @Override
    public BlockPos getPosition() {
        return this.m_owner.getPos();
    }

    @Override
    public boolean teleportTo(@Nonnull World world, @Nonnull BlockPos pos) {
        if (world.isClient || this.getWorld().isClient) {
            throw new UnsupportedOperationException("Cannot teleport on the client");
        }

        // Cache info about the old turtle (so we don't access this after we delete ourselves)
        World oldWorld = this.getWorld();
        TileTurtle oldOwner = this.m_owner;
        BlockPos oldPos = this.m_owner.getPos();
        BlockState oldBlock = this.m_owner.getCachedState();

        if (oldWorld == world && oldPos.equals(pos)) {
            // Teleporting to the current position is a no-op
            return true;
        }

        // Ensure the chunk is loaded
        if (!world.isChunkLoaded(pos)) {
            return false;
        }

        // Ensure we're inside the world border
        if (!world.getWorldBorder()
                  .contains(pos)) {
            return false;
        }

        FluidState existingFluid = world.getBlockState(pos)
                                        .getFluidState();
        BlockState newState = oldBlock
                                  // We only mark this as waterlogged when travelling into a source block. This prevents us from spreading
                                  // fluid by creating a new source when moving into a block, causing the next block to be almost full and
                                  // then moving into that.
                                  .with(WATERLOGGED, existingFluid.isIn(FluidTags.WATER) && existingFluid.isStill());

        oldOwner.notifyMoveStart();

        try {
            // Create a new turtle
            if (world.setBlockState(pos, newState, 0)) {
                Block block = world.getBlockState(pos)
                                   .getBlock();
                if (block == oldBlock.getBlock()) {
                    BlockEntity newTile = world.getBlockEntity(pos);
                    if (newTile instanceof TileTurtle) {
                        // Copy the old turtle state into the new turtle
                        TileTurtle newTurtle = (TileTurtle) newTile;
                        newTurtle.setLocation(world, pos);
                        newTurtle.transferStateFrom(oldOwner);
                        newTurtle.createServerComputer()
                                 .setWorld(world);
                        newTurtle.createServerComputer()
                                 .setPosition(pos);

                        // Remove the old turtle
                        oldWorld.removeBlock(oldPos, false);

                        // Make sure everybody knows about it
                        newTurtle.updateBlock();
                        newTurtle.updateInput();
                        newTurtle.updateOutput();
                        return true;
                    }
                }

                // Something went wrong, remove the newly created turtle
                world.removeBlock(pos, false);
            }
        } finally {
            // whatever happens, unblock old turtle in case it's still in world
            oldOwner.notifyMoveEnd();
        }

        return false;
    }

    @Nonnull
    @Override
    public Vec3d getVisualPosition(float f) {
        Vec3d offset = this.getRenderOffset(f);
        BlockPos pos = this.m_owner.getPos();
        return new Vec3d(pos.getX() + 0.5 + offset.x, pos.getY() + 0.5 + offset.y, pos.getZ() + 0.5 + offset.z);
    }

    @Override
    public float getVisualYaw(float f) {
        float yaw = this.getDirection().asRotation();
        switch (this.m_animation) {
        case TURN_LEFT: {
            yaw += 90.0f * (1.0f - this.getAnimationFraction(f));
            if (yaw >= 360.0f) {
                yaw -= 360.0f;
            }
            break;
        }
        case TURN_RIGHT: {
            yaw += -90.0f * (1.0f - this.getAnimationFraction(f));
            if (yaw < 0.0f) {
                yaw += 360.0f;
            }
            break;
        }
        }
        return yaw;
    }

    @Nonnull
    @Override
    public Direction getDirection() {
        return this.m_owner.getDirection();
    }

    @Override
    public void setDirection(@Nonnull Direction dir) {
        this.m_owner.setDirection(dir);
    }

    @Override
    public int getSelectedSlot() {
        return this.m_selectedSlot;
    }

    @Override
    public void setSelectedSlot(int slot) {
        if (this.getWorld().isClient) {
            throw new UnsupportedOperationException("Cannot set the slot on the client");
        }

        if (slot >= 0 && slot < this.m_owner.size()) {
            this.m_selectedSlot = slot;
            this.m_owner.onTileEntityChange();
        }
    }

    @Override
    public int getColour() {
        return this.m_colourHex;
    }

    @Override
    public void setColour(int colour) {
        if (colour >= 0 && colour <= 0xFFFFFF) {
            if (this.m_colourHex != colour) {
                this.m_colourHex = colour;
                this.m_owner.updateBlock();
            }
        } else if (this.m_colourHex != -1) {
            this.m_colourHex = -1;
            this.m_owner.updateBlock();
        }
    }

    @Nullable
    @Override
    public GameProfile getOwningPlayer() {
        return this.m_owningPlayer;
    }

    @Override
    public boolean isFuelNeeded() {
        return ComputerCraft.turtlesNeedFuel;
    }

    @Override
    public int getFuelLevel() {
        return Math.min(this.m_fuelLevel, this.getFuelLimit());
    }

    @Override
    public void setFuelLevel(int level) {
        this.m_fuelLevel = Math.min(level, this.getFuelLimit());
        this.m_owner.onTileEntityChange();
    }

    @Override
    public int getFuelLimit() {
        if (this.m_owner.getFamily() == ComputerFamily.ADVANCED) {
            return ComputerCraft.advancedTurtleFuelLimit;
        } else {
            return ComputerCraft.turtleFuelLimit;
        }
    }

    @Override
    public boolean consumeFuel(int fuel) {
        if (this.getWorld().isClient) {
            throw new UnsupportedOperationException("Cannot consume fuel on the client");
        }

        if (!this.isFuelNeeded()) {
            return true;
        }

        int consumption = Math.max(fuel, 0);
        if (this.getFuelLevel() >= consumption) {
            this.setFuelLevel(this.getFuelLevel() - consumption);
            return true;
        }
        return false;
    }

    @Override
    public void addFuel(int fuel) {
        if (this.getWorld().isClient) {
            throw new UnsupportedOperationException("Cannot add fuel on the client");
        }

        int addition = Math.max(fuel, 0);
        this.setFuelLevel(this.getFuelLevel() + addition);
    }

    @Nonnull
    @Override
    public MethodResult executeCommand(@Nonnull ITurtleCommand command) {
        if (this.getWorld().isClient) {
            throw new UnsupportedOperationException("Cannot run commands on the client");
        }

        // Issue command
        int commandID = this.issueCommand(command);
        return new CommandCallback(commandID).pull;
    }

    private int issueCommand(ITurtleCommand command) {
        this.m_commandQueue.offer(new TurtleCommandQueueEntry(++this.m_commandsIssued, command));
        return this.m_commandsIssued;
    }

    @Override
    public void playAnimation(@Nonnull TurtleAnimation animation) {
        if (this.getWorld().isClient) {
            throw new UnsupportedOperationException("Cannot play animations on the client");
        }

        this.m_animation = animation;
        if (this.m_animation == TurtleAnimation.SHORT_WAIT) {
            this.m_animationProgress = ANIM_DURATION / 2;
            this.m_lastAnimationProgress = ANIM_DURATION / 2;
        } else {
            this.m_animationProgress = 0;
            this.m_lastAnimationProgress = 0;
        }
        this.m_owner.updateBlock();
    }

    @Override
    public ITurtleUpgrade getUpgrade(@Nonnull TurtleSide side) {
        return this.m_upgrades.get(side);
    }

    @Override
    public void setUpgrade(@Nonnull TurtleSide side, ITurtleUpgrade upgrade) {
        // Remove old upgrade
        if (this.m_upgrades.containsKey(side)) {
            if (this.m_upgrades.get(side) == upgrade) {
                return;
            }
            this.m_upgrades.remove(side);
        } else {
            if (upgrade == null) {
                return;
            }
        }

        this.m_upgradeNBTData.remove(side);

        // Set new upgrade
        if (upgrade != null) {
            this.m_upgrades.put(side, upgrade);
        }

        // Notify clients and create peripherals
        if (this.m_owner.getWorld() != null) {
            this.updatePeripherals(this.m_owner.createServerComputer());
            this.m_owner.updateBlock();
        }
    }

    @Override
    public IPeripheral getPeripheral(@Nonnull TurtleSide side) {
        return this.peripherals.get(side);
    }

    @Nonnull
    @Override
    public CompoundTag getUpgradeNBTData(TurtleSide side) {
        CompoundTag nbt = this.m_upgradeNBTData.get(side);
        if (nbt == null) {
            this.m_upgradeNBTData.put(side, nbt = new CompoundTag());
        }
        return nbt;
    }

    @Override
    public void updateUpgradeNBTData(@Nonnull TurtleSide side) {
        this.m_owner.updateBlock();
    }

    @Nonnull
    @Override
    public Inventory getInventory() {
        return this.m_inventory;
    }

    public void setOwningPlayer(GameProfile profile) {
        this.m_owningPlayer = profile;
    }

    private void updateCommands() {
        if (this.m_animation != TurtleAnimation.NONE || this.m_commandQueue.isEmpty()) {
            return;
        }

        // If we've got a computer, ensure that we're allowed to perform work.
        ServerComputer computer = this.m_owner.getServerComputer();
        if (computer != null && !computer.getComputer()
                                         .getMainThreadMonitor()
                                         .canWork()) {
            return;
        }

        // Pull a new command
        TurtleCommandQueueEntry nextCommand = this.m_commandQueue.poll();
        if (nextCommand == null) {
            return;
        }

        // Execute the command
        long start = System.nanoTime();
        TurtleCommandResult result = nextCommand.command.execute(this);
        long end = System.nanoTime();

        // Dispatch the callback
        if (computer == null) {
            return;
        }
        computer.getComputer()
                .getMainThreadMonitor()
                .trackWork(end - start, TimeUnit.NANOSECONDS);
        int callbackID = nextCommand.callbackID;
        if (callbackID < 0) {
            return;
        }

        if (result != null && result.isSuccess()) {
            Object[] results = result.getResults();
            if (results != null) {
                Object[] arguments = new Object[results.length + 2];
                arguments[0] = callbackID;
                arguments[1] = true;
                System.arraycopy(results, 0, arguments, 2, results.length);
                computer.queueEvent("turtle_response", arguments);
            } else {
                computer.queueEvent("turtle_response", new Object[] {
                    callbackID,
                    true,
                    });
            }
        } else {
            computer.queueEvent("turtle_response", new Object[] {
                callbackID,
                false,
                result != null ? result.getErrorMessage() : null,
                });
        }
    }

    private void updateAnimation() {
        if (this.m_animation != TurtleAnimation.NONE) {
            World world = this.getWorld();

            if (ComputerCraft.turtlesCanPush) {
                // Advance entity pushing
                if (this.m_animation == TurtleAnimation.MOVE_FORWARD || this.m_animation == TurtleAnimation.MOVE_BACK || this.m_animation == TurtleAnimation.MOVE_UP || this.m_animation == TurtleAnimation.MOVE_DOWN) {
                    BlockPos pos = this.getPosition();
                    Direction moveDir;
                    switch (this.m_animation) {
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

                    float pushFrac = 1.0f - (float) (this.m_animationProgress + 1) / ANIM_DURATION;
                    float push = Math.max(pushFrac + 0.0125f, 0.0f);
                    if (moveDir.getOffsetX() < 0) {
                        minX += moveDir.getOffsetX() * push;
                    } else {
                        maxX -= moveDir.getOffsetX() * push;
                    }

                    if (moveDir.getOffsetY() < 0) {
                        minY += moveDir.getOffsetY() * push;
                    } else {
                        maxY -= moveDir.getOffsetY() * push;
                    }

                    if (moveDir.getOffsetZ() < 0) {
                        minZ += moveDir.getOffsetZ() * push;
                    } else {
                        maxZ -= moveDir.getOffsetZ() * push;
                    }

                    Box aabb = new Box(minX, minY, minZ, maxX, maxY, maxZ);
                    List<Entity> list = world.getEntitiesByClass(Entity.class, aabb, EntityPredicates.EXCEPT_SPECTATOR);
                    if (!list.isEmpty()) {
                        double pushStep = 1.0f / ANIM_DURATION;
                        double pushStepX = moveDir.getOffsetX() * pushStep;
                        double pushStepY = moveDir.getOffsetY() * pushStep;
                        double pushStepZ = moveDir.getOffsetZ() * pushStep;
                        for (Entity entity : list) {
                            entity.move(MovementType.PISTON, new Vec3d(pushStepX, pushStepY, pushStepZ));
                        }
                    }
                }
            }

            // Advance valentines day easter egg
            if (world.isClient && this.m_animation == TurtleAnimation.MOVE_FORWARD && this.m_animationProgress == 4) {
                // Spawn love pfx if valentines day
                Holiday currentHoliday = HolidayUtil.getCurrentHoliday();
                if (currentHoliday == Holiday.VALENTINES) {
                    Vec3d position = this.getVisualPosition(1.0f);
                    if (position != null) {
                        double x = position.x + world.random.nextGaussian() * 0.1;
                        double y = position.y + 0.5 + world.random.nextGaussian() * 0.1;
                        double z = position.z + world.random.nextGaussian() * 0.1;
                        world.addParticle(ParticleTypes.HEART,
                                          x,
                                          y,
                                          z,
                                          world.random.nextGaussian() * 0.02,
                                          world.random.nextGaussian() * 0.02,
                                          world.random.nextGaussian() * 0.02);
                    }
                }
            }

            // Wait for anim completion
            this.m_lastAnimationProgress = this.m_animationProgress;
            if (++this.m_animationProgress >= ANIM_DURATION) {
                this.m_animation = TurtleAnimation.NONE;
                this.m_animationProgress = 0;
                this.m_lastAnimationProgress = 0;
            }
        }
    }

    public Vec3d getRenderOffset(float f) {
        switch (this.m_animation) {
        case MOVE_FORWARD:
        case MOVE_BACK:
        case MOVE_UP:
        case MOVE_DOWN: {
            // Get direction
            Direction dir;
            switch (this.m_animation) {
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

            double distance = -1.0 + this.getAnimationFraction(f);
            return new Vec3d(distance * dir.getOffsetX(), distance * dir.getOffsetY(), distance * dir.getOffsetZ());
        }
        default: {
            return Vec3d.ZERO;
        }
        }
    }

    private float getAnimationFraction(float f) {
        float next = (float) this.m_animationProgress / ANIM_DURATION;
        float previous = (float) this.m_lastAnimationProgress / ANIM_DURATION;
        return previous + (next - previous) * f;
    }

    public void readFromNBT(CompoundTag nbt) {
        this.readCommon(nbt);

        // Read state
        this.m_selectedSlot = nbt.getInt(NBT_SLOT);

        // Read owner
        if (nbt.contains("Owner", NBTUtil.TAG_COMPOUND)) {
            CompoundTag owner = nbt.getCompound("Owner");
            this.m_owningPlayer = new GameProfile(new UUID(owner.getLong("UpperId"), owner.getLong("LowerId")), owner.getString("Name"));
        } else {
            this.m_owningPlayer = null;
        }
    }

    /**
     * Read common data for saving and client synchronisation.
     *
     * @param nbt The tag to read from
     */
    private void readCommon(CompoundTag nbt) {
        // Read fields
        this.m_colourHex = nbt.contains(NBT_COLOUR) ? nbt.getInt(NBT_COLOUR) : -1;
        this.m_fuelLevel = nbt.contains(NBT_FUEL) ? nbt.getInt(NBT_FUEL) : 0;
        this.m_overlay = nbt.contains(NBT_OVERLAY) ? new Identifier(nbt.getString(NBT_OVERLAY)) : null;

        // Read upgrades
        this.setUpgrade(TurtleSide.LEFT, nbt.contains(NBT_LEFT_UPGRADE) ? TurtleUpgrades.get(nbt.getString(NBT_LEFT_UPGRADE)) : null);
        this.setUpgrade(TurtleSide.RIGHT, nbt.contains(NBT_RIGHT_UPGRADE) ? TurtleUpgrades.get(nbt.getString(NBT_RIGHT_UPGRADE)) : null);

        // NBT
        this.m_upgradeNBTData.clear();
        if (nbt.contains(NBT_LEFT_UPGRADE_DATA)) {
            this.m_upgradeNBTData.put(TurtleSide.LEFT,
                                      nbt.getCompound(NBT_LEFT_UPGRADE_DATA)
                                    .copy());
        }
        if (nbt.contains(NBT_RIGHT_UPGRADE_DATA)) {
            this.m_upgradeNBTData.put(TurtleSide.RIGHT,
                                      nbt.getCompound(NBT_RIGHT_UPGRADE_DATA)
                                    .copy());
        }
    }

    public CompoundTag writeToNBT(CompoundTag nbt) {
        this.writeCommon(nbt);

        // Write state
        nbt.putInt(NBT_SLOT, this.m_selectedSlot);

        // Write owner
        if (this.m_owningPlayer != null) {
            CompoundTag owner = new CompoundTag();
            nbt.put("Owner", owner);

            owner.putLong("UpperId", this.m_owningPlayer.getId()
                                                        .getMostSignificantBits());
            owner.putLong("LowerId", this.m_owningPlayer.getId()
                                                        .getLeastSignificantBits());
            owner.putString("Name", this.m_owningPlayer.getName());
        }

        return nbt;
    }

    private void writeCommon(CompoundTag nbt) {
        nbt.putInt(NBT_FUEL, this.m_fuelLevel);
        if (this.m_colourHex != -1) {
            nbt.putInt(NBT_COLOUR, this.m_colourHex);
        }
        if (this.m_overlay != null) {
            nbt.putString(NBT_OVERLAY, this.m_overlay.toString());
        }

        // Write upgrades
        String leftUpgradeId = getUpgradeId(this.getUpgrade(TurtleSide.LEFT));
        if (leftUpgradeId != null) {
            nbt.putString(NBT_LEFT_UPGRADE, leftUpgradeId);
        }
        String rightUpgradeId = getUpgradeId(this.getUpgrade(TurtleSide.RIGHT));
        if (rightUpgradeId != null) {
            nbt.putString(NBT_RIGHT_UPGRADE, rightUpgradeId);
        }

        // Write upgrade NBT
        if (this.m_upgradeNBTData.containsKey(TurtleSide.LEFT)) {
            nbt.put(NBT_LEFT_UPGRADE_DATA,
                    this.getUpgradeNBTData(TurtleSide.LEFT).copy());
        }
        if (this.m_upgradeNBTData.containsKey(TurtleSide.RIGHT)) {
            nbt.put(NBT_RIGHT_UPGRADE_DATA,
                    this.getUpgradeNBTData(TurtleSide.RIGHT).copy());
        }
    }

    private static String getUpgradeId(ITurtleUpgrade upgrade) {
        return upgrade != null ? upgrade.getUpgradeID()
                                        .toString() : null;
    }

    public void readDescription(CompoundTag nbt) {
        this.readCommon(nbt);

        // Animation
        TurtleAnimation anim = TurtleAnimation.values()[nbt.getInt("Animation")];
        if (anim != this.m_animation && anim != TurtleAnimation.WAIT && anim != TurtleAnimation.SHORT_WAIT && anim != TurtleAnimation.NONE) {
            this.m_animation = anim;
            this.m_animationProgress = 0;
            this.m_lastAnimationProgress = 0;
        }
    }

    public void writeDescription(CompoundTag nbt) {
        this.writeCommon(nbt);
        nbt.putInt("Animation", this.m_animation.ordinal());
    }

    public Identifier getOverlay() {
        return this.m_overlay;
    }

    public void setOverlay(Identifier overlay) {
        if (!Objects.equal(this.m_overlay, overlay)) {
            this.m_overlay = overlay;
            this.m_owner.updateBlock();
        }
    }

    public DyeColor getDyeColour() {
        if (this.m_colourHex == -1) {
            return null;
        }
        Colour colour = Colour.fromHex(this.m_colourHex);
        return colour == null ? null : DyeColor.byId(15 - colour.ordinal());
    }

    public void setDyeColour(DyeColor dyeColour) {
        int newColour = -1;
        if (dyeColour != null) {
            newColour = Colour.values()[15 - dyeColour.getId()].getHex();
        }
        if (this.m_colourHex != newColour) {
            this.m_colourHex = newColour;
            this.m_owner.updateBlock();
        }
    }

    public float getToolRenderAngle(TurtleSide side, float f) {
        return (side == TurtleSide.LEFT && this.m_animation == TurtleAnimation.SWING_LEFT_TOOL) || (side == TurtleSide.RIGHT && this.m_animation == TurtleAnimation.SWING_RIGHT_TOOL) ? 45.0f * (float) Math.sin(
            this.getAnimationFraction(f) * Math.PI) : 0.0f;
    }

    private static final class CommandCallback implements ILuaCallback {
        final MethodResult pull = MethodResult.pullEvent("turtle_response", this);
        private final int command;

        CommandCallback(int command) {
            this.command = command;
        }

        @Nonnull
        @Override
        public MethodResult resume(Object[] response) {
            if (response.length < 3 || !(response[1] instanceof Number) || !(response[2] instanceof Boolean)) {
                return this.pull;
            }

            if (((Number) response[1]).intValue() != this.command) {
                return this.pull;
            }

            return MethodResult.of(Arrays.copyOfRange(response, 2, response.length));
        }
    }
}

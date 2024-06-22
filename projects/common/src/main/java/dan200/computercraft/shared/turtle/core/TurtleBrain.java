// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.api.lua.ILuaCallback;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.util.PeripheralHelpers;
import dan200.computercraft.impl.TurtleUpgrades;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.container.InventoryDelegate;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import dan200.computercraft.shared.util.BlockEntityHelpers;
import dan200.computercraft.shared.util.Holiday;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static dan200.computercraft.shared.common.IColouredItem.NBT_COLOUR;
import static dan200.computercraft.shared.util.WaterloggableHelpers.WATERLOGGED;

public class TurtleBrain implements TurtleAccessInternal {
    public static final String NBT_RIGHT_UPGRADE = "RightUpgrade";
    public static final String NBT_RIGHT_UPGRADE_DATA = "RightUpgradeNbt";
    public static final String NBT_LEFT_UPGRADE = "LeftUpgrade";
    public static final String NBT_LEFT_UPGRADE_DATA = "LeftUpgradeNbt";
    public static final String NBT_FUEL = "Fuel";
    public static final String NBT_OVERLAY = "Overlay";

    private static final String NBT_SLOT = "Slot";

    private static final int ANIM_DURATION = 8;

    private TurtleBlockEntity owner;
    private @Nullable GameProfile owningPlayer;

    private final Container inventory = (InventoryDelegate) () -> owner;

    private final Queue<TurtleCommandQueueEntry> commandQueue = new ArrayDeque<>();
    private int commandsIssued = 0;

    private final Map<TurtleSide, ITurtleUpgrade> upgrades = new EnumMap<>(TurtleSide.class);
    private final Map<TurtleSide, IPeripheral> peripherals = new EnumMap<>(TurtleSide.class);
    private final Map<TurtleSide, CompoundTag> upgradeNBTData = new EnumMap<>(TurtleSide.class);

    private int selectedSlot = 0;
    private int fuelLevel = 0;
    private int colourHex = -1;
    private @Nullable ResourceLocation overlay = null;

    private TurtleAnimation animation = TurtleAnimation.NONE;
    private int animationProgress = 0;
    private int lastAnimationProgress = 0;

    @Nullable
    TurtlePlayer cachedPlayer;

    public TurtleBrain(TurtleBlockEntity turtle) {
        owner = turtle;
    }

    public void setOwner(TurtleBlockEntity owner) {
        this.owner = owner;
    }

    public TurtleBlockEntity getOwner() {
        return owner;
    }

    public ComputerFamily getFamily() {
        return owner.getFamily();
    }

    public void setupComputer(ServerComputer computer) {
        updatePeripherals(computer);
    }

    public void update() {
        var world = getLevel();
        if (!world.isClientSide) {
            // Advance movement
            updateCommands();

            // The block may have been broken while the command was executing (for instance, if a block explodes
            // when being mined). If so, abort.
            if (owner.isRemoved()) return;
        }

        // Advance animation
        updateAnimation();

        // Advance upgrades
        if (!upgrades.isEmpty()) {
            for (var entry : upgrades.entrySet()) {
                entry.getValue().update(this, entry.getKey());
            }
        }
    }

    /**
     * Read common data for saving and client synchronisation.
     *
     * @param nbt The tag to read from
     */
    private void readCommon(CompoundTag nbt) {
        // Read fields
        colourHex = nbt.contains(NBT_COLOUR) ? nbt.getInt(NBT_COLOUR) : -1;
        fuelLevel = nbt.contains(NBT_FUEL) ? nbt.getInt(NBT_FUEL) : 0;
        overlay = nbt.contains(NBT_OVERLAY) ? new ResourceLocation(nbt.getString(NBT_OVERLAY)) : null;

        // Read upgrades
        setUpgradeDirect(TurtleSide.LEFT, readUpgrade(nbt, NBT_LEFT_UPGRADE, NBT_LEFT_UPGRADE_DATA));
        setUpgradeDirect(TurtleSide.RIGHT, readUpgrade(nbt, NBT_RIGHT_UPGRADE, NBT_RIGHT_UPGRADE_DATA));
    }

    private @Nullable UpgradeData<ITurtleUpgrade> readUpgrade(CompoundTag tag, String upgradeKey, String dataKey) {
        if (!tag.contains(upgradeKey)) return null;
        var upgrade = TurtleUpgrades.instance().get(tag.getString(upgradeKey));
        if (upgrade == null) return null;

        return UpgradeData.of(upgrade, tag.getCompound(dataKey));
    }

    private void writeCommon(CompoundTag nbt) {
        nbt.putInt(NBT_FUEL, fuelLevel);
        if (colourHex != -1) nbt.putInt(NBT_COLOUR, colourHex);
        if (overlay != null) nbt.putString(NBT_OVERLAY, overlay.toString());

        // Write upgrades
        var leftUpgradeId = getUpgradeId(getUpgrade(TurtleSide.LEFT));
        if (leftUpgradeId != null) nbt.putString(NBT_LEFT_UPGRADE, leftUpgradeId);
        var rightUpgradeId = getUpgradeId(getUpgrade(TurtleSide.RIGHT));
        if (rightUpgradeId != null) nbt.putString(NBT_RIGHT_UPGRADE, rightUpgradeId);

        // Write upgrade NBT
        if (upgradeNBTData.containsKey(TurtleSide.LEFT)) {
            nbt.put(NBT_LEFT_UPGRADE_DATA, getUpgradeNBTData(TurtleSide.LEFT).copy());
        }
        if (upgradeNBTData.containsKey(TurtleSide.RIGHT)) {
            nbt.put(NBT_RIGHT_UPGRADE_DATA, getUpgradeNBTData(TurtleSide.RIGHT).copy());
        }
    }

    public void readFromNBT(CompoundTag nbt) {
        readCommon(nbt);

        // Read state
        selectedSlot = nbt.getInt(NBT_SLOT);

        // Read owner
        if (nbt.contains("Owner", Tag.TAG_COMPOUND)) {
            var owner = nbt.getCompound("Owner");
            owningPlayer = new GameProfile(
                new UUID(owner.getLong("UpperId"), owner.getLong("LowerId")),
                owner.getString("Name")
            );
        } else {
            owningPlayer = null;
        }
    }

    public CompoundTag writeToNBT(CompoundTag nbt) {
        writeCommon(nbt);

        // Write state
        nbt.putInt(NBT_SLOT, selectedSlot);

        // Write owner
        if (owningPlayer != null) {
            var owner = new CompoundTag();
            nbt.put("Owner", owner);

            owner.putLong("UpperId", owningPlayer.getId().getMostSignificantBits());
            owner.putLong("LowerId", owningPlayer.getId().getLeastSignificantBits());
            owner.putString("Name", owningPlayer.getName());
        }

        return nbt;
    }

    private static @Nullable String getUpgradeId(@Nullable ITurtleUpgrade upgrade) {
        return upgrade != null ? upgrade.getUpgradeID().toString() : null;
    }

    public void readDescription(CompoundTag nbt) {
        readCommon(nbt);

        // Animation
        var anim = TurtleAnimation.values()[nbt.getInt("Animation")];
        if (anim != animation &&
            anim != TurtleAnimation.WAIT &&
            anim != TurtleAnimation.SHORT_WAIT &&
            anim != TurtleAnimation.NONE) {
            animation = anim;
            animationProgress = 0;
            lastAnimationProgress = 0;
        }
    }

    public void writeDescription(CompoundTag nbt) {
        writeCommon(nbt);
        nbt.putInt("Animation", animation.ordinal());
    }

    @Override
    public Level getLevel() {
        return owner.getLevel();
    }

    @Override
    public BlockPos getPosition() {
        return owner.getBlockPos();
    }

    @Override
    public boolean isRemoved() {
        return owner.isRemoved();
    }

    @Override
    public boolean teleportTo(Level world, BlockPos pos) {
        if (world.isClientSide || getLevel().isClientSide) {
            throw new UnsupportedOperationException("Cannot teleport on the client");
        }

        // Cache info about the old turtle (so we don't access this after we delete ourselves)
        var oldWorld = getLevel();
        var oldOwner = owner;
        var oldPos = owner.getBlockPos();
        var oldBlock = owner.getBlockState();

        if (oldWorld == world && oldPos.equals(pos)) {
            // Teleporting to the current position is a no-op
            return true;
        }

        // Ensure the chunk is loaded
        if (!world.isLoaded(pos)) return false;

        // Ensure we're inside the world border
        if (!world.getWorldBorder().isWithinBounds(pos)) return false;

        var existingFluid = world.getBlockState(pos).getFluidState();
        var newState = oldBlock
            // We only mark this as waterlogged when travelling into a source block. This prevents us from spreading
            // fluid by creating a new source when moving into a block, causing the next block to be almost full and
            // then moving into that.
            .setValue(WATERLOGGED, existingFluid.is(FluidTags.WATER) && existingFluid.isSource());

        oldOwner.notifyMoveStart();

        try {
            // We use Block.UPDATE_CLIENTS here to ensure that neighbour updates caused in Block.updateNeighbourShapes
            // are sent to the client. We want to avoid doing a full block update until the turtle state is copied over.
            if (world.setBlock(pos, newState, 2)) {
                var block = world.getBlockState(pos).getBlock();
                if (block == oldBlock.getBlock()) {
                    var newTile = world.getBlockEntity(pos);
                    if (newTile instanceof TurtleBlockEntity newTurtle) {
                        // Copy the old turtle state into the new turtle
                        newTurtle.setLevel(world);
                        newTurtle.transferStateFrom(oldOwner);

                        var computer = newTurtle.createServerComputer();
                        computer.setPosition((ServerLevel) world, pos);

                        // Remove the old turtle
                        oldWorld.removeBlock(oldPos, false);

                        // Make sure everybody knows about it
                        newTurtle.updateRedstone();
                        newTurtle.updateInputsImmediately();
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

    public Vec3 getVisualPosition(float f) {
        var offset = getRenderOffset(f);
        var pos = owner.getBlockPos();
        return new Vec3(
            pos.getX() + 0.5 + offset.x,
            pos.getY() + 0.5 + offset.y,
            pos.getZ() + 0.5 + offset.z
        );
    }

    public float getVisualYaw(float f) {
        var yaw = getDirection().toYRot();
        switch (animation) {
            case TURN_LEFT -> {
                yaw += 90.0f * (1.0f - getAnimationFraction(f));
                if (yaw >= 360.0f) {
                    yaw -= 360.0f;
                }
            }
            case TURN_RIGHT -> {
                yaw += -90.0f * (1.0f - getAnimationFraction(f));
                if (yaw < 0.0f) {
                    yaw += 360.0f;
                }
            }
            default -> {
            }
        }
        return yaw;
    }

    @Override
    public Direction getDirection() {
        return owner.getDirection();
    }

    @Override
    public void setDirection(Direction dir) {
        owner.setDirection(dir);
    }

    @Override
    public int getSelectedSlot() {
        return selectedSlot;
    }

    @Override
    public void setSelectedSlot(int slot) {
        if (getLevel().isClientSide) throw new UnsupportedOperationException("Cannot set the slot on the client");

        if (slot >= 0 && slot < owner.getContainerSize()) {
            selectedSlot = slot;
            owner.onTileEntityChange();
        }
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public boolean isFuelNeeded() {
        return Config.turtlesNeedFuel;
    }

    @Override
    public int getFuelLevel() {
        return Math.min(fuelLevel, getFuelLimit());
    }

    @Override
    public void setFuelLevel(int level) {
        fuelLevel = Math.min(level, getFuelLimit());
        owner.onTileEntityChange();
    }

    @Override
    public int getFuelLimit() {
        return owner.getFuelLimit();
    }

    @Override
    public boolean consumeFuel(int fuel) {
        if (getLevel().isClientSide) throw new UnsupportedOperationException("Cannot consume fuel on the client");

        if (!isFuelNeeded()) return true;

        var consumption = Math.max(fuel, 0);
        if (getFuelLevel() >= consumption) {
            setFuelLevel(getFuelLevel() - consumption);
            return true;
        }
        return false;
    }

    @Override
    public void addFuel(int fuel) {
        if (getLevel().isClientSide) throw new UnsupportedOperationException("Cannot add fuel on the client");

        var addition = Math.max(fuel, 0);
        setFuelLevel(getFuelLevel() + addition);
    }

    @Override
    public MethodResult executeCommand(TurtleCommand command) {
        if (getLevel().isClientSide) throw new UnsupportedOperationException("Cannot run commands on the client");
        if (commandQueue.size() > 16) return MethodResult.of(false, "Too many ongoing turtle commands");

        commandQueue.offer(new TurtleCommandQueueEntry(++commandsIssued, command));
        var commandID = commandsIssued;
        return new CommandCallback(commandID).pull;
    }

    @Override
    public void playAnimation(TurtleAnimation animation) {
        if (getLevel().isClientSide) throw new UnsupportedOperationException("Cannot play animations on the client");

        this.animation = animation;
        if (this.animation == TurtleAnimation.SHORT_WAIT) {
            animationProgress = ANIM_DURATION / 2;
            lastAnimationProgress = ANIM_DURATION / 2;
        } else {
            animationProgress = 0;
            lastAnimationProgress = 0;
        }
        BlockEntityHelpers.updateBlock(owner);
    }

    public @Nullable ResourceLocation getOverlay() {
        return overlay;
    }

    public void setOverlay(@Nullable ResourceLocation overlay) {
        if (!Objects.equals(this.overlay, overlay)) {
            this.overlay = overlay;
            BlockEntityHelpers.updateBlock(owner);
        }
    }

    @Override
    public void setColour(int colour) {
        if (colour >= 0 && colour <= 0xFFFFFF) {
            if (colourHex != colour) {
                colourHex = colour;
                BlockEntityHelpers.updateBlock(owner);
            }
        } else if (colourHex != -1) {
            colourHex = -1;
            BlockEntityHelpers.updateBlock(owner);
        }
    }

    @Override
    public int getColour() {
        return colourHex;
    }

    public void setOwningPlayer(GameProfile profile) {
        owningPlayer = profile;
    }

    @Nullable
    @Override
    public GameProfile getOwningPlayer() {
        return owningPlayer;
    }

    @Override
    public @Nullable ITurtleUpgrade getUpgrade(TurtleSide side) {
        return upgrades.get(side);
    }

    @Override
    public void setUpgradeWithData(TurtleSide side, @Nullable UpgradeData<ITurtleUpgrade> upgrade) {
        if (!setUpgradeDirect(side, upgrade) || owner.getLevel() == null) return;

        // This is a separate function to avoid updating the block when reading the NBT. We don't need to do this as
        // either the block is newly placed (and so won't have changed) or is being updated with /data, which calls
        // updateBlock for us.
        BlockEntityHelpers.updateBlock(owner);

        // Recompute peripherals in case an upgrade being removed has exposed a new peripheral.
        // TODO: Only update peripherals, or even only two sides?
        owner.updateInputsImmediately();
    }

    private boolean setUpgradeDirect(TurtleSide side, @Nullable UpgradeData<ITurtleUpgrade> upgrade) {
        // Remove old upgrade
        var oldUpgrade = upgrades.remove(side);
        if (oldUpgrade == null && upgrade == null) return false;

        // Set new upgrade
        if (upgrade == null) {
            upgradeNBTData.remove(side);
        } else {
            upgrades.put(side, upgrade.upgrade());
            upgradeNBTData.put(side, upgrade.data().copy());
        }

        // Notify clients and create peripherals
        if (owner.getLevel() != null && !owner.getLevel().isClientSide) {
            updatePeripherals(owner.createServerComputer());
        }

        return true;
    }

    @Override
    public @Nullable IPeripheral getPeripheral(TurtleSide side) {
        return peripherals.get(side);
    }

    @Override
    public CompoundTag getUpgradeNBTData(TurtleSide side) {
        var nbt = upgradeNBTData.get(side);
        if (nbt == null) upgradeNBTData.put(side, nbt = new CompoundTag());
        return nbt;
    }

    @Override
    public void updateUpgradeNBTData(TurtleSide side) {
        BlockEntityHelpers.updateBlock(owner);
    }

    public Vec3 getRenderOffset(float f) {
        switch (animation) {
            case MOVE_FORWARD, MOVE_BACK, MOVE_UP, MOVE_DOWN -> {
                // Get direction
                var dir = switch (animation) {
                    case MOVE_FORWARD -> getDirection();
                    case MOVE_BACK -> getDirection().getOpposite();
                    case MOVE_UP -> Direction.UP;
                    case MOVE_DOWN -> Direction.DOWN;
                    default -> throw new IllegalStateException("Impossible direction");
                };

                var distance = -1.0 + getAnimationFraction(f);
                return new Vec3(
                    distance * dir.getStepX(),
                    distance * dir.getStepY(),
                    distance * dir.getStepZ()
                );
            }
            default -> {
                return Vec3.ZERO;
            }
        }
    }

    public float getToolRenderAngle(TurtleSide side, float f) {
        return (side == TurtleSide.LEFT && animation == TurtleAnimation.SWING_LEFT_TOOL) ||
            (side == TurtleSide.RIGHT && animation == TurtleAnimation.SWING_RIGHT_TOOL)
            ? 45.0f * (float) Math.sin(getAnimationFraction(f) * Math.PI)
            : 0.0f;
    }

    private static ComputerSide toDirection(TurtleSide side) {
        return switch (side) {
            case LEFT -> ComputerSide.LEFT;
            case RIGHT -> ComputerSide.RIGHT;
        };
    }

    private void updatePeripherals(ServerComputer serverComputer) {
        if (serverComputer == null) return;

        // Update peripherals
        for (var side : TurtleSide.values()) {
            var upgrade = getUpgrade(side);
            IPeripheral peripheral = null;
            if (upgrade != null && upgrade.getType().isPeripheral()) {
                peripheral = upgrade.createPeripheral(this, side);
            }

            var existing = peripherals.get(side);
            if (PeripheralHelpers.equals(existing, peripheral)) {
                // If the peripheral is the same, just use that.
                peripheral = existing;
            } else {
                // Otherwise update our map
                peripherals.put(side, peripheral);
            }

            // Always update the computer: it may not be the same computer as before!
            serverComputer.setPeripheral(toDirection(side), peripheral);
        }
    }

    private void updateCommands() {
        if (animation != TurtleAnimation.NONE || commandQueue.isEmpty()) return;

        // If we've got a computer, ensure that we're allowed to perform work.
        var computer = owner.getServerComputer();
        if (computer != null && !computer.getMainThreadMonitor().canWork()) return;

        // Pull a new command
        var nextCommand = commandQueue.poll();
        if (nextCommand == null) return;

        // Execute the command
        var start = System.nanoTime();
        var result = nextCommand.command().execute(this);
        var end = System.nanoTime();

        // Dispatch the callback
        if (computer == null) return;
        computer.getMainThreadMonitor().trackWork(end - start, TimeUnit.NANOSECONDS);
        var callbackID = nextCommand.callbackID();
        if (callbackID < 0) return;

        if (result != null && result.isSuccess()) {
            var results = result.getResults();
            if (results != null) {
                var arguments = new Object[results.length + 2];
                arguments[0] = callbackID;
                arguments[1] = true;
                System.arraycopy(results, 0, arguments, 2, results.length);
                computer.queueEvent("turtle_response", arguments);
            } else {
                computer.queueEvent("turtle_response", new Object[]{
                    callbackID, true,
                });
            }
        } else {
            computer.queueEvent("turtle_response", new Object[]{
                callbackID, false, result != null ? result.getErrorMessage() : null,
            });
        }
    }

    private void updateAnimation() {
        if (animation != TurtleAnimation.NONE) {
            var world = getLevel();

            if (Config.turtlesCanPush) {
                // Advance entity pushing
                if (animation == TurtleAnimation.MOVE_FORWARD ||
                    animation == TurtleAnimation.MOVE_BACK ||
                    animation == TurtleAnimation.MOVE_UP ||
                    animation == TurtleAnimation.MOVE_DOWN) {
                    var pos = getPosition();
                    var moveDir = switch (animation) {
                        case MOVE_FORWARD -> getDirection();
                        case MOVE_BACK -> getDirection().getOpposite();
                        case MOVE_UP -> Direction.UP;
                        case MOVE_DOWN -> Direction.DOWN;
                        default -> throw new IllegalStateException("Impossible direction");
                    };

                    double minX = pos.getX();
                    double minY = pos.getY();
                    double minZ = pos.getZ();
                    var maxX = minX + 1.0;
                    var maxY = minY + 1.0;
                    var maxZ = minZ + 1.0;

                    var pushFrac = 1.0f - (float) (animationProgress + 1) / ANIM_DURATION;
                    var push = Math.max(pushFrac + 0.0125f, 0.0f);
                    if (moveDir.getStepX() < 0) {
                        minX += moveDir.getStepX() * push;
                    } else {
                        maxX -= moveDir.getStepX() * push;
                    }

                    if (moveDir.getStepY() < 0) {
                        minY += moveDir.getStepY() * push;
                    } else {
                        maxY -= moveDir.getStepY() * push;
                    }

                    if (moveDir.getStepZ() < 0) {
                        minZ += moveDir.getStepZ() * push;
                    } else {
                        maxZ -= moveDir.getStepZ() * push;
                    }

                    var aabb = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
                    var list = world.getEntitiesOfClass(Entity.class, aabb, TurtleBrain::canPush);
                    if (!list.isEmpty()) {
                        double pushStep = 1.0f / ANIM_DURATION;
                        var pushStepX = moveDir.getStepX() * pushStep;
                        var pushStepY = moveDir.getStepY() * pushStep;
                        var pushStepZ = moveDir.getStepZ() * pushStep;
                        for (var entity : list) {
                            entity.move(MoverType.PISTON, new Vec3(pushStepX, pushStepY, pushStepZ));
                        }
                    }
                }
            }

            // Advance valentines day easter egg
            if (world.isClientSide && animation == TurtleAnimation.MOVE_FORWARD && animationProgress == 4) {
                // Spawn love pfx if valentines day
                var currentHoliday = Holiday.getCurrent();
                if (currentHoliday == Holiday.VALENTINES) {
                    var position = getVisualPosition(1.0f);
                    if (position != null) {
                        var x = position.x + world.random.nextGaussian() * 0.1;
                        var y = position.y + 0.5 + world.random.nextGaussian() * 0.1;
                        var z = position.z + world.random.nextGaussian() * 0.1;
                        world.addParticle(
                            ParticleTypes.HEART, x, y, z,
                            world.random.nextGaussian() * 0.02,
                            world.random.nextGaussian() * 0.02,
                            world.random.nextGaussian() * 0.02
                        );
                    }
                }
            }

            // Wait for anim completion
            lastAnimationProgress = animationProgress;
            if (++animationProgress >= ANIM_DURATION) {
                animation = TurtleAnimation.NONE;
                animationProgress = 0;
                lastAnimationProgress = 0;
            }
        }
    }

    private static boolean canPush(Entity entity) {
        return !entity.isSpectator() && entity.getPistonPushReaction() != PushReaction.IGNORE;
    }

    private float getAnimationFraction(float f) {
        var next = (float) animationProgress / ANIM_DURATION;
        var previous = (float) lastAnimationProgress / ANIM_DURATION;
        return previous + (next - previous) * f;
    }

    @Override
    public ItemStack getItemSnapshot(int slot) {
        return owner.getItemSnapshot(slot);
    }

    private static final class CommandCallback implements ILuaCallback {
        final MethodResult pull = MethodResult.pullEvent("turtle_response", this);
        private final int command;

        CommandCallback(int command) {
            this.command = command;
        }

        @Override
        public MethodResult resume(Object[] response) {
            if (response.length < 3 || !(response[1] instanceof Number id) || !(response[2] instanceof Boolean)) {
                return pull;
            }

            if (id.intValue() != command) return pull;

            return MethodResult.of(Arrays.copyOfRange(response, 2, response.length));
        }
    }
}

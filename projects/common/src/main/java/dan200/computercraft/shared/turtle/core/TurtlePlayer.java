// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.turtle.TurtleUtil;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.UUID;

public final class TurtlePlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TurtlePlayer.class);

    private static final GameProfile DEFAULT_PROFILE = new GameProfile(
        UUID.fromString("0d0c4ca0-4ff1-11e4-916c-0800200c9a66"),
        "[ComputerCraft]"
    );

    private final ServerPlayer player;

    private TurtlePlayer(ServerPlayer player) {
        this.player = player;
    }

    private static TurtlePlayer create(ITurtleAccess turtle) {
        var world = (ServerLevel) turtle.getLevel();
        var profile = turtle.getOwningPlayer();

        var player = new TurtlePlayer(PlatformHelper.get().createFakePlayer(world, getProfile(profile)));
        player.setState(turtle);
        return player;
    }

    private static GameProfile getProfile(@Nullable GameProfile profile) {
        return profile != null && profile.isComplete() ? profile : DEFAULT_PROFILE;
    }

    public static TurtlePlayer get(ITurtleAccess access) {
        if (!(access instanceof TurtleBrain brain)) throw new IllegalStateException("ITurtleAccess is not a brain");

        var player = brain.cachedPlayer;
        if (player == null || player.player.getGameProfile() != getProfile(access.getOwningPlayer())
            || player.player.level() != access.getLevel()) {
            player = brain.cachedPlayer = create(brain);
        } else {
            player.setState(access);
        }

        return player;
    }

    public static TurtlePlayer getWithPosition(ITurtleAccess turtle, BlockPos position, Direction direction) {
        var turtlePlayer = get(turtle);
        turtlePlayer.setPosition(turtle, position, direction);
        return turtlePlayer;
    }

    public ServerPlayer player() {
        return player;
    }

    private void setRotation(float y, float x) {
        player.setYRot(y);
        player.setXRot(x);
    }

    private void setState(ITurtleAccess turtle) {
        if (player.containerMenu != player.inventoryMenu) {
            LOGGER.warn("Turtle has open container ({})", player.containerMenu);
            player.doCloseContainer();
        }

        var position = turtle.getPosition();
        player.setPosRaw(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
        setRotation(turtle.getDirection().toYRot(), 0);

        player.getInventory().clearContent();
    }

    public void setPosition(ITurtleAccess turtle, BlockPos position, Direction direction) {
        var posX = position.getX() + 0.5;
        var posY = position.getY() + 0.5;
        var posZ = position.getZ() + 0.5;

        // Stop intersection with the turtle itself
        if (turtle.getPosition().equals(position)) {
            posX += 0.48 * direction.getStepX();
            posY += 0.48 * direction.getStepY();
            posZ += 0.48 * direction.getStepZ();
        }

        if (direction.getAxis() != Direction.Axis.Y) {
            setRotation(direction.toYRot(), 0);
        } else {
            setRotation(turtle.getDirection().toYRot(), DirectionUtil.toPitchAngle(direction));
        }

        player.setPosRaw(posX, posY, posZ);
        player.xo = posX;
        player.yo = posY;
        player.zo = posZ;
        player.xRotO = player.getXRot();
        player.yHeadRotO = player.yHeadRot = player.yRotO = player.getYRot();
    }

    public void loadInventory(ItemStack stack) {
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, stack);
    }

    public void loadInventory(ITurtleAccess turtle) {
        var inventory = player.getInventory();
        var turtleInventory = turtle.getInventory();
        var currentSlot = turtle.getSelectedSlot();
        var slots = turtleInventory.getContainerSize();

        // Load up the fake inventory
        inventory.selected = 0;
        for (var i = 0; i < slots; i++) {
            inventory.setItem(i, turtleInventory.getItem((currentSlot + i) % slots));
        }
    }

    public void unloadInventory(ITurtleAccess turtle) {
        if (player.isUsingItem()) player.stopUsingItem();

        var inventory = player.getInventory();
        var turtleInventory = turtle.getInventory();
        var currentSlot = turtle.getSelectedSlot();
        var slots = turtleInventory.getContainerSize();

        // Load up the fake inventory
        inventory.selected = 0;
        for (var i = 0; i < slots; i++) {
            turtleInventory.setItem((currentSlot + i) % slots, inventory.getItem(i));
        }

        // Store (or drop) anything else we found
        var totalSize = inventory.getContainerSize();
        for (var i = slots; i < totalSize; i++) {
            TurtleUtil.storeItemOrDrop(turtle, inventory.getItem(i));
        }

        inventory.setChanged();
    }

    public boolean isBlockProtected(ServerLevel level, BlockPos pos) {
        return level.getServer().isUnderSpawnProtection(level, pos, player);
    }
}

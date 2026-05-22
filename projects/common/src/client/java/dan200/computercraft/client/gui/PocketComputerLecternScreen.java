// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.render.CustomLecternRenderer;
import dan200.computercraft.shared.lectern.PocketComputerLecternMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

/**
 * The screen for computers on lecterns.
 * <p>
 * This extends {@link NoTermComputerScreen}, but with support for interacting with the lectern's pocket computer.
 */
public final class PocketComputerLecternScreen extends NoTermComputerScreen<PocketComputerLecternMenu> {
    public PocketComputerLecternScreen(PocketComputerLecternMenu menu, Inventory player, Component title) {
        super(menu, player, title);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var position = getMousePosition();
        if (position != null) {
            computerInput.mouseClick(button + 1, position.x() + 1, position.y() + 1);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        var position = getMousePosition();
        if (position != null) {
            computerInput.mouseDrag(button + 1, position.x() + 1, position.y() + 1);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        var position = getMousePosition();
        if (position != null) {
            computerInput.mouseUp(button + 1, position.x() + 1, position.y() + 1);
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double moueX, double mouseY, double delta) {
        var position = getMousePosition();
        if (position != null && delta != 0) {
            computerInput.mouseScroll(delta < 0 ? 1 : -1, position.x(), position.y());
            return true;
        }

        return super.mouseScrolled(moueX, mouseY, delta);
    }

    /**
     * Get the position of the mouse on the pocket terminal computer.
     *
     * @return The cursor position, or {@code null} if the mouse is out-of-bounds.
     */
    private @Nullable Vector2ic getMousePosition() {
        var minecraft = Objects.requireNonNull(this.minecraft);
        if (minecraft.level == null || minecraft.player == null || minecraft.hitResult == null) return null;

        var lecternPos = getMenu().lectern();
        var terminal = getMenu().getTerminal();

        // First ensure we're looking at the lectern block.
        if (minecraft.hitResult.getType() != HitResult.Type.BLOCK || !((BlockHitResult) minecraft.hitResult).getBlockPos().equals(lecternPos)) {
            return null;
        }

        // Build the same pose stack that we use for rendering pocket computers.
        var poseStack = new PoseStack();
        CustomLecternRenderer.applyLecternTransform(poseStack, minecraft.level.getBlockState(lecternPos));
        CustomLecternRenderer.applyPocketComputerTerminalTransform(poseStack);
        CustomLecternRenderer.applyScaledPocketComputerTerminalTransform(poseStack, terminal);

        // Then take the inverse matrix, and use it to map the player's position and look vector to terminal space.
        var inverseTransform = poseStack.last().pose().invert(new Matrix4f());
        var startPosition = minecraft.player.getEyePosition();
        var transformedStartPosition = inverseTransform.transformPosition(
            (float) (startPosition.x() - lecternPos.getX()),
            (float) (startPosition.y() - lecternPos.getY()),
            (float) (startPosition.z() - lecternPos.getZ()),
            new Vector3f()
        );
        var transformedLookVector = inverseTransform.transformDirection(minecraft.player.getLookAngle().toVector3f());

        // Compute the intersection of our plane with the look vector. This is trivial, as the terminal is at
        // (0, 0, 0), with a normal of (0, 0, 1).
        if (transformedLookVector.z() == 0) return null;
        var intersection = transformedStartPosition.add(
            transformedLookVector.mul(-transformedStartPosition.z() / transformedLookVector.z())
        );

        // Then map back to actual terminal coordinates, and check we're still in bounds.
        var positionX = (int) (intersection.x() / FONT_WIDTH);
        var positionY = (int) (intersection.y() / FONT_HEIGHT);
        return positionX >= 0 && positionX < terminal.getWidth() && positionY >= 0 && positionY < terminal.getHeight()
            ? new Vector2i(positionX, positionY)
            : null;
    }
}

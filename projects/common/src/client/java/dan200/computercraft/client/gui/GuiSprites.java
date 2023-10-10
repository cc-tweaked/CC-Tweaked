// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.data.client.ClientDataProviders;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Sprite sheet for all GUI texutres in the mod.
 */
public final class GuiSprites extends TextureAtlasHolder {
    public static final ResourceLocation SPRITE_SHEET = new ResourceLocation(ComputerCraftAPI.MOD_ID, "gui");
    public static final ResourceLocation TEXTURE = SPRITE_SHEET.withPath(x -> "textures/atlas/" + x + ".png");

    public static final ButtonTextures TURNED_OFF = button("turned_off");
    public static final ButtonTextures TURNED_ON = button("turned_on");
    public static final ButtonTextures TERMINATE = button("terminate");

    public static final ComputerTextures COMPUTER_NORMAL = computer("normal", true, true);
    public static final ComputerTextures COMPUTER_ADVANCED = computer("advanced", true, true);
    public static final ComputerTextures COMPUTER_COMMAND = computer("command", false, true);
    public static final ComputerTextures COMPUTER_COLOUR = computer("colour", true, false);

    private static ButtonTextures button(String name) {
        return new ButtonTextures(
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "gui/buttons/" + name),
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "gui/buttons/" + name + "_hover")
        );
    }

    private static ComputerTextures computer(String name, boolean pocket, boolean sidebar) {
        return new ComputerTextures(
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "gui/border_" + name),
            pocket ? new ResourceLocation(ComputerCraftAPI.MOD_ID, "gui/pocket_bottom_" + name) : null,
            sidebar ? new ResourceLocation(ComputerCraftAPI.MOD_ID, "gui/sidebar_" + name) : null
        );
    }

    private static @Nullable GuiSprites instance;

    private GuiSprites(TextureManager textureManager) {
        super(textureManager, TEXTURE, SPRITE_SHEET);
    }

    /**
     * Initialise the singleton {@link GuiSprites} instance.
     *
     * @param textureManager The current texture manager.
     * @return The singleton {@link GuiSprites} instance, to register as resource reload listener.
     */
    public static GuiSprites initialise(TextureManager textureManager) {
        if (instance != null) throw new IllegalStateException("GuiSprites has already been initialised");
        return instance = new GuiSprites(textureManager);
    }

    /**
     * Lookup a texture on the atlas.
     *
     * @param texture The texture to find.
     * @return The sprite on the atlas.
     */
    public static TextureAtlasSprite get(ResourceLocation texture) {
        if (instance == null) throw new IllegalStateException("GuiSprites has not been initialised");
        return instance.getSprite(texture);
    }

    /**
     * Get the appropriate textures to use for a particular computer family.
     *
     * @param family The computer family.
     * @return The family-specific textures.
     */
    public static ComputerTextures getComputerTextures(ComputerFamily family) {
        return switch (family) {
            case NORMAL -> COMPUTER_NORMAL;
            case ADVANCED -> COMPUTER_ADVANCED;
            case COMMAND -> COMPUTER_COMMAND;
        };
    }

    /**
     * A set of sprites for a button, with both a normal and "active" state.
     *
     * @param normal The normal texture for the button.
     * @param active The texture for the button when it is active (hovered or focused).
     */
    public record ButtonTextures(ResourceLocation normal, ResourceLocation active) {
        public TextureAtlasSprite get(boolean active) {
            return GuiSprites.get(active ? this.active : normal);
        }

        public Stream<ResourceLocation> textures() {
            return Stream.of(normal, active);
        }
    }

    /**
     * Set the set of sprites for a computer family.
     *
     * @param border       The texture for the computer's border.
     * @param pocketBottom The texture for the bottom of a pocket computer.
     * @param sidebar      The texture for the computer sidebar.
     * @see ComputerBorderRenderer
     * @see ClientDataProviders
     */
    public record ComputerTextures(
        ResourceLocation border,
        @Nullable ResourceLocation pocketBottom,
        @Nullable ResourceLocation sidebar
    ) {
        public Stream<ResourceLocation> textures() {
            return Stream.of(border, pocketBottom, sidebar).filter(Objects::nonNull);
        }
    }
}

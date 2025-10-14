// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.render.CustomLecternRenderer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * A model for {@linkplain PocketComputerItem pocket computers} placed on a lectern.
 *
 * @see CustomLecternRenderer
 */
public class LecternPocketModel {
    // Direct texture references for entity rendering (not atlas-based)
    public static final ResourceLocation TEXTURE_NORMAL = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "textures/entity/pocket_computer_normal.png");
    public static final ResourceLocation TEXTURE_ADVANCED = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "textures/entity/pocket_computer_advanced.png");
    public static final ResourceLocation TEXTURE_COLOUR = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "textures/entity/pocket_computer_colour.png");
    public static final ResourceLocation TEXTURE_FRAME = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "textures/entity/pocket_computer_frame.png");
    public static final ResourceLocation TEXTURE_LIGHT = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "textures/entity/pocket_computer_light.png");

    // The size of the terminal within the model.
    public static final float TERM_WIDTH = 12.0f / 32.0f;
    public static final float TERM_HEIGHT = 14.0f / 32.0f;

    // The size of the texture. The texture is 36x36, but is at 2x resolution.
    private static final int TEXTURE_WIDTH = 48 / 2;
    private static final int TEXTURE_HEIGHT = 48 / 2;

    private final ModelPart root;

    public LecternPocketModel() {
        root = buildPages();
    }

    private static ModelPart buildPages() {
        var mesh = new MeshDefinition();
        var parts = mesh.getRoot();
        parts.addOrReplaceChild(
            "root",
            CubeListBuilder.create().texOffs(0, 0).addBox(0f, -5.0f, -4.0f, 1f, 10.0f, 8.0f),
            PartPose.ZERO
        );
        return mesh.getRoot().bake(TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    /**
     * Render the pocket computer model using the new rendering system.
     *
     * @param poseStack     The current pose stack.
     * @param collector     The submit node collector to draw to.
     * @param packedLight   The current light level.
     * @param packedOverlay The overlay texture (used for entity hurt animation).
     * @param family        The computer family.
     * @param frameColour   The pocket computer's color.
     * @param lightColour   The pocket computer's light color.
     */
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay, ComputerFamily family, int frameColour, int lightColour) {
        if (frameColour != -1) {
            collector.submitModelPart(root, poseStack, RenderType.entityCutout(TEXTURE_FRAME), packedLight, packedOverlay, null);
            // TODO: Color tinting for pocket computers - need to implement proper tinting support
            collector.submitModelPart(root, poseStack, RenderType.entityCutout(TEXTURE_COLOUR), packedLight, packedOverlay, null);
        } else {
            var texture = family == ComputerFamily.ADVANCED ? TEXTURE_ADVANCED : TEXTURE_NORMAL;
            collector.submitModelPart(root, poseStack, RenderType.entityCutout(texture), packedLight, packedOverlay, null);
        }

        // Light rendering - TODO: Light color support
        collector.submitModelPart(root, poseStack, RenderType.entityCutout(TEXTURE_LIGHT), LightTexture.FULL_BRIGHT, packedOverlay, null);
    }
}

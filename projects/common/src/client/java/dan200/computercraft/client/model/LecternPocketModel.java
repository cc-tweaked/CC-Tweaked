// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.pocket.PocketComputerData;
import dan200.computercraft.client.render.CustomLecternRenderer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * A model for {@linkplain PocketComputerItem pocket computers} placed on a lectern.
 *
 * @see CustomLecternRenderer
 */
public class LecternPocketModel extends Model<Unit> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "lectern_pocket"), "main");

    public static final Material MATERIAL_NORMAL = Sheets.BLOCK_ENTITIES_MAPPER.apply(ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "pocket_computer_normal"));
    public static final Material MATERIAL_ADVANCED = Sheets.BLOCK_ENTITIES_MAPPER.apply(ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "pocket_computer_advanced"));
    public static final Material MATERIAL_COLOUR = Sheets.BLOCK_ENTITIES_MAPPER.apply(ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "pocket_computer_colour"));
    public static final Material MATERIAL_FRAME = Sheets.BLOCK_ENTITIES_MAPPER.apply(ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "pocket_computer_frame"));
    public static final Material MATERIAL_LIGHT = Sheets.BLOCK_ENTITIES_MAPPER.apply(ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "pocket_computer_light"));

    // The size of the terminal within the model.
    public static final float TERM_WIDTH = 12.0f / 32.0f;
    public static final float TERM_HEIGHT = 14.0f / 32.0f;

    // The size of the texture. The texture is 36x36, but is at 2x resolution.
    private static final int TEXTURE_WIDTH = 48 / 2;
    private static final int TEXTURE_HEIGHT = 48 / 2;

    public LecternPocketModel(ModelPart root) {
        super(root, RenderType::entityCutout);
    }

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var parts = mesh.getRoot();
        parts.addOrReplaceChild(
            "root",
            CubeListBuilder.create().texOffs(0, 0).addBox(0f, -5.0f, -4.0f, 1f, 10.0f, 8.0f),
            PartPose.ZERO
        );
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    /**
     * Render the pocket computer model.
     *
     * @param poseStack   The current pose stack.
     * @param collector   The collector to draw to.
     * @param materials   The current materials
     * @param packedLight The current light level.
     * @param family      The computer family.
     * @param frameColour The pocket computer's {@linkplain DyedItemColor colour}.
     * @param lightColour The pocket computer's {@linkplain PocketComputerData#getLightState() light colour}.
     */
    public void submit(
        PoseStack poseStack, SubmitNodeCollector collector, MaterialSet materials, int packedLight, ComputerFamily family, int frameColour, int lightColour
    ) {
        if (frameColour != -1) {
            collector.submitModel(
                this, Unit.INSTANCE, poseStack, MATERIAL_FRAME.renderType(RenderType::entityCutout),
                packedLight, OverlayTexture.NO_OVERLAY, -1, materials.get(MATERIAL_FRAME), 0, null
            );
            collector.submitModel(
                this, Unit.INSTANCE, poseStack, MATERIAL_COLOUR.renderType(RenderType::entityCutout),
                packedLight, OverlayTexture.NO_OVERLAY, frameColour, materials.get(MATERIAL_COLOUR), 0, null
            );
        } else {
            var material = family == ComputerFamily.ADVANCED ? MATERIAL_ADVANCED : MATERIAL_NORMAL;
            collector.submitModel(
                this, Unit.INSTANCE, poseStack, material.renderType(RenderType::entityCutout),
                packedLight, OverlayTexture.NO_OVERLAY, -1, materials.get(material), 0, null
            );
        }

        collector.submitModel(
            this, Unit.INSTANCE, poseStack, MATERIAL_LIGHT.renderType(RenderType::entityCutout),
            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, lightColour, materials.get(MATERIAL_LIGHT), 0, null
        );
    }
}

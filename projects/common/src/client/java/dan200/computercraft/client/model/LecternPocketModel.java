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
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Unit;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * A model for {@linkplain PocketComputerItem pocket computers} placed on a lectern.
 *
 * @see CustomLecternRenderer
 */
public class LecternPocketModel extends Model<Unit> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "lectern_pocket"), "main");

    public static final SpriteId SPRITE_NORMAL = Sheets.BLOCK_ENTITIES_MAPPER.apply(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "pocket_computer_normal"));
    public static final SpriteId SPRITE_ADVANCED = Sheets.BLOCK_ENTITIES_MAPPER.apply(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "pocket_computer_advanced"));
    public static final SpriteId SPRITE_COLOUR = Sheets.BLOCK_ENTITIES_MAPPER.apply(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "pocket_computer_colour"));
    public static final SpriteId SPRITE_FRAME = Sheets.BLOCK_ENTITIES_MAPPER.apply(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "pocket_computer_frame"));
    public static final SpriteId SPRITE_LIGHT = Sheets.BLOCK_ENTITIES_MAPPER.apply(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "pocket_computer_light"));

    // The size of the terminal within the model.
    public static final float TERM_WIDTH = 12.0f / 32.0f;
    public static final float TERM_HEIGHT = 14.0f / 32.0f;

    // The size of the texture. The texture is 36x36, but is at 2x resolution.
    private static final int TEXTURE_WIDTH = 48 / 2;
    private static final int TEXTURE_HEIGHT = 48 / 2;

    public LecternPocketModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
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
     * @param sprites     The current sprites
     * @param packedLight The current light level.
     * @param family      The computer family.
     * @param frameColour The pocket computer's {@linkplain DyedItemColor colour}.
     * @param lightColour The pocket computer's {@linkplain PocketComputerData#getLightState() light colour}.
     */
    public void submit(
        PoseStack poseStack, SubmitNodeCollector collector, SpriteGetter sprites, int packedLight, ComputerFamily family, int frameColour, int lightColour
    ) {
        if (frameColour != -1) {
            collector.submitModel(
                this, Unit.INSTANCE, poseStack, SPRITE_FRAME.renderType(RenderTypes::entityCutout),
                packedLight, OverlayTexture.NO_OVERLAY, -1, sprites.get(SPRITE_FRAME), 0, null
            );
            collector.submitModel(
                this, Unit.INSTANCE, poseStack, SPRITE_COLOUR.renderType(RenderTypes::entityCutout),
                packedLight, OverlayTexture.NO_OVERLAY, frameColour, sprites.get(SPRITE_COLOUR), 0, null
            );
        } else {
            var material = family == ComputerFamily.ADVANCED ? SPRITE_ADVANCED : SPRITE_NORMAL;
            collector.submitModel(
                this, Unit.INSTANCE, poseStack, material.renderType(RenderTypes::entityCutout),
                packedLight, OverlayTexture.NO_OVERLAY, -1, sprites.get(material), 0, null
            );
        }

        collector.submitModel(
            this, Unit.INSTANCE, poseStack, SPRITE_LIGHT.renderType(RenderTypes::entityCutout),
            LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, lightColour, sprites.get(SPRITE_LIGHT), 0, null
        );
    }
}

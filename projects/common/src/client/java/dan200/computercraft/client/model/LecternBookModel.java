// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.render.CustomLecternRenderer;
import dan200.computercraft.shared.media.items.PrintoutItem;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;

import static dan200.computercraft.client.model.LecternPrintoutModelDefinitions.TEXTURE_HEIGHT;
import static dan200.computercraft.client.model.LecternPrintoutModelDefinitions.TEXTURE_WIDTH;

/**
 * A model for {@linkplain PrintoutItem printed books} placed on a lectern.
 *
 * @see CustomLecternRenderer
 */
public final class LecternBookModel extends Model<Unit> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "lectern_book"), "main");

    public LecternBookModel(ModelPart root) {
        super(root, RenderType::entitySolid);
    }

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var parts = mesh.getRoot();

        parts.addOrReplaceChild(
            "spine",
            CubeListBuilder.create().texOffs(12, 15).addBox(-0.005f, -5.0f, -0.5f, 0, 10, 1.0f),
            PartPose.ZERO
        );

        var angle = (float) Math.toRadians(5);
        parts.addOrReplaceChild(
            "left",
            CubeListBuilder.create()
                .texOffs(0, 10).addBox(0, -5.0f, -6.0f, 0, 10, 6.0f)
                .texOffs(0, 0).addBox(0.005f, -4.0f, -5.0f, 1.0f, 8.0f, 5.0f),
            PartPose.offsetAndRotation(-0.005f, 0, -0.5f, 0, -angle, 0)
        );

        parts.addOrReplaceChild(
            "right",
            CubeListBuilder.create()
                .texOffs(14, 10).addBox(0, -5.0f, 0, 0, 10, 6.0f)
                .texOffs(0, 0).addBox(0.005f, -4.0f, 0, 1.0f, 8.0f, 5.0f),
            PartPose.offsetAndRotation(-0.005f, 0, 0.5f, 0, angle, 0)
        );

        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}

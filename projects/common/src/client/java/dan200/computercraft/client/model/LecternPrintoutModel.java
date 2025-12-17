// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
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
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * A model for {@linkplain PrintoutItem printouts} placed on a lectern. This renders a variable number of pages (1-3),
 * stored in {@link State#pages}.
 *
 * @see CustomLecternRenderer
 */
public final class LecternPrintoutModel extends Model<LecternPrintoutModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "lectern_printout"), "main");

    public static final Material MATERIAL = Sheets.BLOCK_ENTITIES_MAPPER.apply(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "printout"));

    static final int TEXTURE_WIDTH = 32;
    static final int TEXTURE_HEIGHT = 32;

    private static final String PAGE_1 = "page_1";
    private static final String PAGE_2 = "page_2";
    private static final String PAGE_3 = "page_3";
    private static final List<String> PAGES = List.of(PAGE_1, PAGE_2, PAGE_3);

    private final ModelPart[] pages;

    public LecternPrintoutModel(ModelPart root) {
        super(root, RenderTypes::entitySolid);
        pages = PAGES.stream().map(root::getChild).toArray(ModelPart[]::new);
    }

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var parts = mesh.getRoot();
        parts.addOrReplaceChild(
            PAGE_1,
            CubeListBuilder.create().texOffs(0, 0).addBox(-0.005f, -4.0f, -2.5f, 1f, 8.0f, 5.0f),
            PartPose.ZERO
        );

        parts.addOrReplaceChild(
            PAGE_2,
            CubeListBuilder.create().texOffs(12, 0).addBox(-0.005f, -4.0f, -2.5f, 1f, 8.0f, 5.0f),
            PartPose.offsetAndRotation(-0.125f, 0, 1.5f, (float) Math.PI * (1f / 16), 0, 0)
        );
        parts.addOrReplaceChild(
            PAGE_3,
            CubeListBuilder.create().texOffs(12, 0).addBox(-0.005f, -4.0f, -2.5f, 1f, 8.0f, 5.0f),
            PartPose.offsetAndRotation(-0.25f, 0, -1.5f, (float) -Math.PI * (2f / 16), 0, 0)
        );

        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    public void setupAnim(State renderState) {
        var pageCount = renderState.pages;
        if (pageCount > pages.length) pageCount = pages.length;

        var i = 0;
        for (; i < pageCount; i++) pages[i].visible = true;
        for (; i < pages.length; i++) pages[i].visible = false;
    }

    public static class State {
        public int pages;
    }
}

/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;

import dan200.computercraft.api.AbstractTurtleUpgrade;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class TurtleCraftingTable extends AbstractTurtleUpgrade {
    @Environment (EnvType.CLIENT) private ModelIdentifier m_leftModel;

    @Environment (EnvType.CLIENT) private ModelIdentifier m_rightModel;

    public TurtleCraftingTable(Identifier id) {
        super(id, TurtleUpgradeType.Peripheral, Blocks.CRAFTING_TABLE);
    }

    @Override
    public IPeripheral createPeripheral(@Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side) {
        return new CraftingTablePeripheral(turtle);
    }

    @Nonnull
    @Override
    @Environment (EnvType.CLIENT)
    public Pair<BakedModel, Matrix4f> getModel(ITurtleAccess turtle, @Nonnull TurtleSide side) {
        this.loadModelLocations();

        Matrix4f transform = null;
        BakedModelManager modelManager = MinecraftClient.getInstance()
                                                        .getItemRenderer()
                                                        .getModels()
                                                        .getModelManager();
        if (side == TurtleSide.Left) {
            return Pair.of(modelManager.getModel(this.m_leftModel), transform);
        } else {
            return Pair.of(modelManager.getModel(this.m_rightModel), transform);
        }
    }

    @Environment (EnvType.CLIENT)
    private void loadModelLocations() {
        if (this.m_leftModel == null) {
            this.m_leftModel = new ModelIdentifier("computercraft:turtle_crafting_table_left", "inventory");
            this.m_rightModel = new ModelIdentifier("computercraft:turtle_crafting_table_right", "inventory");
        }
    }
}

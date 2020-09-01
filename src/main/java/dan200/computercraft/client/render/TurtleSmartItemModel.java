/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.google.common.base.Objects;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.AffineTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class TurtleSmartItemModel implements BakedModel
{
    private static final AffineTransformation identity, flip;

    static
    {
        MatrixStack stack = new MatrixStack();
        stack.scale( 0, -1, 0 );
        stack.translate( 0, 0, 1 );

        identity = AffineTransformation.identity();
        flip = new AffineTransformation( stack.peek().getModel() );
    }

    private static class TurtleModelCombination
    {
        final boolean m_colour;
        final ITurtleUpgrade m_leftUpgrade;
        final ITurtleUpgrade m_rightUpgrade;
        final Identifier m_overlay;
        final boolean m_christmas;
        final boolean m_flip;

        TurtleModelCombination( boolean colour, ITurtleUpgrade leftUpgrade, ITurtleUpgrade rightUpgrade, Identifier overlay, boolean christmas, boolean flip )
        {
            m_colour = colour;
            m_leftUpgrade = leftUpgrade;
            m_rightUpgrade = rightUpgrade;
            m_overlay = overlay;
            m_christmas = christmas;
            m_flip = flip;
        }

        @Override
        public boolean equals( Object other )
        {
            if( other == this ) return true;
            if( !(other instanceof TurtleModelCombination) ) return false;

            TurtleModelCombination otherCombo = (TurtleModelCombination) other;
            return otherCombo.m_colour == m_colour &&
                otherCombo.m_leftUpgrade == m_leftUpgrade &&
                otherCombo.m_rightUpgrade == m_rightUpgrade &&
                Objects.equal( otherCombo.m_overlay, m_overlay ) &&
                otherCombo.m_christmas == m_christmas &&
                otherCombo.m_flip == m_flip;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 0;
            result = prime * result + (m_colour ? 1 : 0);
            result = prime * result + (m_leftUpgrade != null ? m_leftUpgrade.hashCode() : 0);
            result = prime * result + (m_rightUpgrade != null ? m_rightUpgrade.hashCode() : 0);
            result = prime * result + (m_overlay != null ? m_overlay.hashCode() : 0);
            result = prime * result + (m_christmas ? 1 : 0);
            result = prime * result + (m_flip ? 1 : 0);
            return result;
        }
    }

    private final BakedModel familyModel;
    private final BakedModel colourModel;

    private final HashMap<TurtleModelCombination, BakedModel> m_cachedModels = new HashMap<>();
    private final ModelOverrideList m_overrides;

    public TurtleSmartItemModel( BakedModel familyModel, BakedModel colourModel )
    {
        this.familyModel = familyModel;
        this.colourModel = colourModel;

        m_overrides = new ModelOverrideList()
        {
            @Nonnull
            @Override
            public BakedModel apply( @Nonnull BakedModel originalModel, @Nonnull ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity )
            {
                ItemTurtle turtle = (ItemTurtle) stack.getItem();
                int colour = turtle.getColour( stack );
                ITurtleUpgrade leftUpgrade = turtle.getUpgrade( stack, TurtleSide.LEFT );
                ITurtleUpgrade rightUpgrade = turtle.getUpgrade( stack, TurtleSide.RIGHT );
                Identifier overlay = turtle.getOverlay( stack );
                boolean christmas = HolidayUtil.getCurrentHoliday() == Holiday.CHRISTMAS;
                String label = turtle.getLabel( stack );
                boolean flip = label != null && (label.equals( "Dinnerbone" ) || label.equals( "Grumm" ));
                TurtleModelCombination combo = new TurtleModelCombination( colour != -1, leftUpgrade, rightUpgrade, overlay, christmas, flip );

                BakedModel model = m_cachedModels.get( combo );
                if( model == null ) m_cachedModels.put( combo, model = buildModel( combo ) );
                return model;
            }
        };
    }

    @Nonnull
    @Override
    public ModelOverrideList getOverrides()
    {
        return m_overrides;
    }

    private BakedModel buildModel( TurtleModelCombination combo )
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        BakedModelManager modelManager = mc.getItemRenderer().getModels().getModelManager();
        ModelIdentifier overlayModelLocation = TileEntityTurtleRenderer.getTurtleOverlayModel( combo.m_overlay, combo.m_christmas );

        BakedModel baseModel = combo.m_colour ? colourModel : familyModel;
        BakedModel overlayModel = overlayModelLocation != null ? modelManager.getModel( overlayModelLocation ) : null;
        AffineTransformation transform = combo.m_flip ? flip : identity;
        TransformedModel leftModel = combo.m_leftUpgrade != null ? combo.m_leftUpgrade.getModel( null, TurtleSide.LEFT ) : null;
        TransformedModel rightModel = combo.m_rightUpgrade != null ? combo.m_rightUpgrade.getModel( null, TurtleSide.RIGHT ) : null;
        return new TurtleMultiModel( baseModel, overlayModel, transform, leftModel, rightModel );
    }

    @Nonnull
    @Override
    @Deprecated
    public List<BakedQuad> getQuads( BlockState state, Direction facing, @Nonnull Random rand )
    {
        return familyModel.getQuads( state, facing, rand );
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return familyModel.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth()
    {
        return familyModel.hasDepth();
    }

    @Override
    public boolean isBuiltin()
    {
        return familyModel.isBuiltin();
    }

    @Override
    public boolean isSideLit()
    {
        return familyModel.isSideLit();
    }

    @Nonnull
    @Override
    @Deprecated
    public Sprite getSprite()
    {
        return familyModel.getSprite();
    }

    @Nonnull
    @Override
    @Deprecated
    public ModelTransformation getTransformation()
    {
        return familyModel.getTransformation();
    }

}

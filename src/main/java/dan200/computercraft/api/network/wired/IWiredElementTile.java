package dan200.computercraft.api.network.wired;

import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A {@link net.minecraft.tileentity.TileEntity} which provides a {@link IWiredElement}. This acts
 * as a simpler alternative to a full-blown {@link IWiredProvider}.
 */
public interface IWiredElementTile
{
    /**
     * Get the wired element of this tile for a given side.
     *
     * @param side The side to get the network element from.
     * @return A network element, or {@code null} if there is no element here.
     */
    @Nullable
    IWiredElement getWiredElement( @Nonnull EnumFacing side );
}

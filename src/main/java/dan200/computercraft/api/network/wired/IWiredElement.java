package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

/**
 * An object which may be part of a wired network.
 *
 * Elements should construct a node using {@link ComputerCraftAPI#createWiredNodeForElement(IWiredElement)}. This acts
 * as a proxy for all network objects. Whilst the node may change networks, an element's node should remain constant
 * for its lifespan.
 *
 * Elements are generally tied to a block or tile entity in world. In such as case, one should provide the
 * {@link IWiredElement} capability for the appropriate sides.
 */
public interface IWiredElement extends IWiredSender
{
    /**
     * Fetch the peripherals this network element provides.
     *
     * This is only called when initially attaching to a network and after a call to {@link IWiredNode#invalidate()}}, so
     * one does not <em>need</em> to cache the return value.
     *
     * @return The peripherals this node provides.
     * @see IWiredNode#invalidate()
     */
    @Nonnull
    default Map<String, IPeripheral> getPeripherals()
    {
        return Collections.emptyMap();
    }

    /**
     * Called when objects on the network change. This may occur when network nodes are added or removed, or when
     * peripherals change.
     *
     * @param change The change which occurred.
     * @see IWiredNetworkChange
     */
    default void networkChanged( @Nonnull IWiredNetworkChange change )
    {
    }
}

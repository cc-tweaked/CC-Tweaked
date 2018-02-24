package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.ClientTerminal;
import gnu.trove.set.hash.TIntHashSet;

public class ClientMonitor extends ClientTerminal
{
    private static final TIntHashSet displayLists = new TIntHashSet();

    private final TileMonitor origin;

    public long lastRenderFrame = -1;
    public int[] renderDisplayLists = null;

    public ClientMonitor( boolean colour, TileMonitor origin )
    {
        super( colour );
        this.origin = origin;
    }

    public TileMonitor getOrigin()
    {
        return origin;
    }

    public void destroy()
    {
        if( renderDisplayLists != null )
        {
            for( int displayList : renderDisplayLists )
            {
                ComputerCraft.deleteDisplayLists( displayList, 1 );
            }

            renderDisplayLists = null;
        }
    }
}

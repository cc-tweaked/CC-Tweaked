/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.wired;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNetwork;
import dan200.computercraft.api.network.wired.IWiredNetworkChange;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class NetworkTest
{
    @Before
    public void setup()
    {
        ComputerCraft.log = LogManager.getLogger();
    }

    @Test
    public void testConnect()
    {
        NetworkElement
            aE = new NetworkElement( null, null, "a" ),
            bE = new NetworkElement( null, null, "b" ),
            cE = new NetworkElement( null, null, "c" );

        IWiredNode
            aN = aE.getNode(),
            bN = bE.getNode(),
            cN = cE.getNode();

        assertNotEquals( "A's and B's network must be different", aN.getNetwork(), bN.getNetwork() );
        assertNotEquals( "A's and C's network must be different", aN.getNetwork(), cN.getNetwork() );
        assertNotEquals( "B's and C's network must be different", bN.getNetwork(), cN.getNetwork() );

        assertTrue( "Must be able to add connection", aN.getNetwork().connect( aN, bN ) );
        assertFalse( "Cannot add connection twice", aN.getNetwork().connect( aN, bN ) );

        assertEquals( "A's and B's network must be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's network should be A and B", Sets.newHashSet( aN, bN ), nodes( aN.getNetwork() ) );

        assertEquals( "A's peripheral set should be A, B", Sets.newHashSet( "a", "b" ), aE.allPeripherals().keySet() );
        assertEquals( "B's peripheral set should be A, B", Sets.newHashSet( "a", "b" ), bE.allPeripherals().keySet() );

        aN.getNetwork().connect( aN, cN );

        assertEquals( "A's and B's network must be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's and C's network must be equal", aN.getNetwork(), cN.getNetwork() );
        assertEquals( "A's network should be A, B and C", Sets.newHashSet( aN, bN, cN ), nodes( aN.getNetwork() ) );

        assertEquals( "A's neighbour set should be B, C", Sets.newHashSet( bN, cN ), neighbours( aN ) );
        assertEquals( "B's neighbour set should be A", Sets.newHashSet( aN ), neighbours( bN ) );
        assertEquals( "C's neighbour set should be A", Sets.newHashSet( aN ), neighbours( cN ) );

        assertEquals( "A's peripheral set should be A, B, C", Sets.newHashSet( "a", "b", "c" ), aE.allPeripherals().keySet() );
        assertEquals( "B's peripheral set should be A, B, C", Sets.newHashSet( "a", "b", "c" ), bE.allPeripherals().keySet() );
        assertEquals( "C's peripheral set should be A, B, C", Sets.newHashSet( "a", "b", "c" ), cE.allPeripherals().keySet() );
    }

    @Test
    public void testDisconnectNoChange()
    {
        NetworkElement
            aE = new NetworkElement( null, null, "a" ),
            bE = new NetworkElement( null, null, "b" ),
            cE = new NetworkElement( null, null, "c" );

        IWiredNode
            aN = aE.getNode(),
            bN = bE.getNode(),
            cN = cE.getNode();

        aN.getNetwork().connect( aN, bN );
        aN.getNetwork().connect( aN, cN );
        aN.getNetwork().connect( bN, cN );

        aN.getNetwork().disconnect( aN, bN );

        assertEquals( "A's and B's network must be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's and C's network must be equal", aN.getNetwork(), cN.getNetwork() );
        assertEquals( "A's network should be A, B and C", Sets.newHashSet( aN, bN, cN ), nodes( aN.getNetwork() ) );

        assertEquals( "A's peripheral set should be A, B, C", Sets.newHashSet( "a", "b", "c" ), aE.allPeripherals().keySet() );
        assertEquals( "B's peripheral set should be A, B, C", Sets.newHashSet( "a", "b", "c" ), bE.allPeripherals().keySet() );
        assertEquals( "C's peripheral set should be A, B, C", Sets.newHashSet( "a", "b", "c" ), cE.allPeripherals().keySet() );
    }

    @Test
    public void testDisconnectLeaf()
    {
        NetworkElement
            aE = new NetworkElement( null, null, "a" ),
            bE = new NetworkElement( null, null, "b" ),
            cE = new NetworkElement( null, null, "c" );

        IWiredNode
            aN = aE.getNode(),
            bN = bE.getNode(),
            cN = cE.getNode();

        aN.getNetwork().connect( aN, bN );
        aN.getNetwork().connect( aN, cN );

        aN.getNetwork().disconnect( aN, bN );

        assertNotEquals( "A's and B's network must not be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's and C's network must be equal", aN.getNetwork(), cN.getNetwork() );
        assertEquals( "A's network should be A and C", Sets.newHashSet( aN, cN ), nodes( aN.getNetwork() ) );
        assertEquals( "B's network should be B", Sets.newHashSet( bN ), nodes( bN.getNetwork() ) );

        assertEquals( "A's peripheral set should be A, C", Sets.newHashSet( "a", "c" ), aE.allPeripherals().keySet() );
        assertEquals( "B's peripheral set should be B", Sets.newHashSet( "b" ), bE.allPeripherals().keySet() );
        assertEquals( "C's peripheral set should be A, C", Sets.newHashSet( "a", "c" ), cE.allPeripherals().keySet() );
    }

    @Test
    public void testDisconnectSplit()
    {
        NetworkElement
            aE = new NetworkElement( null, null, "a" ),
            aaE = new NetworkElement( null, null, "a_" ),
            bE = new NetworkElement( null, null, "b" ),
            bbE = new NetworkElement( null, null, "b_" );

        IWiredNode
            aN = aE.getNode(),
            aaN = aaE.getNode(),
            bN = bE.getNode(),
            bbN = bbE.getNode();

        aN.getNetwork().connect( aN, aaN );
        bN.getNetwork().connect( bN, bbN );

        aN.getNetwork().connect( aN, bN );

        aN.getNetwork().disconnect( aN, bN );

        assertNotEquals( "A's and B's network must not be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's and A_'s network must be equal", aN.getNetwork(), aaN.getNetwork() );
        assertEquals( "B's and B_'s network must be equal", bN.getNetwork(), bbN.getNetwork() );

        assertEquals( "A's network should be A and A_", Sets.newHashSet( aN, aaN ), nodes( aN.getNetwork() ) );
        assertEquals( "B's network should be B and B_", Sets.newHashSet( bN, bbN ), nodes( bN.getNetwork() ) );

        assertEquals( "A's peripheral set should be A and A_", Sets.newHashSet( "a", "a_" ), aE.allPeripherals().keySet() );
        assertEquals( "B's peripheral set should be B and B_", Sets.newHashSet( "b", "b_" ), bE.allPeripherals().keySet() );
    }

    @Test
    public void testRemoveSingle()
    {
        NetworkElement aE = new NetworkElement( null, null, "a" );
        IWiredNode aN = aE.getNode();

        IWiredNetwork network = aN.getNetwork();
        assertFalse( "Cannot remove node from an empty network", aN.remove() );
        assertEquals( "Networks are same before and after", network, aN.getNetwork() );
    }

    @Test
    public void testRemoveLeaf()
    {
        NetworkElement
            aE = new NetworkElement( null, null, "a" ),
            bE = new NetworkElement( null, null, "b" ),
            cE = new NetworkElement( null, null, "c" );

        IWiredNode
            aN = aE.getNode(),
            bN = bE.getNode(),
            cN = cE.getNode();

        aN.getNetwork().connect( aN, bN );
        aN.getNetwork().connect( aN, cN );

        assertTrue( "Must be able to remove node", aN.getNetwork().remove( bN ) );
        assertFalse( "Cannot remove a second time", aN.getNetwork().remove( bN ) );

        assertNotEquals( "A's and B's network must not be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's and C's network must be equal", aN.getNetwork(), cN.getNetwork() );

        assertEquals( "A's network should be A and C", Sets.newHashSet( aN, cN ), nodes( aN.getNetwork() ) );
        assertEquals( "B's network should be B", Sets.newHashSet( bN ), nodes( bN.getNetwork() ) );

        assertEquals( "A's peripheral set should be A, C", Sets.newHashSet( "a", "c" ), aE.allPeripherals().keySet() );
        assertEquals( "B's peripheral set should be empty", Sets.newHashSet(), bE.allPeripherals().keySet() );
        assertEquals( "C's peripheral set should be A, C", Sets.newHashSet( "a", "c" ), cE.allPeripherals().keySet() );
    }

    @Test
    public void testRemoveSplit()
    {
        NetworkElement
            aE = new NetworkElement( null, null, "a" ),
            aaE = new NetworkElement( null, null, "a_" ),
            bE = new NetworkElement( null, null, "b" ),
            bbE = new NetworkElement( null, null, "b_" ),
            cE = new NetworkElement( null, null, "c" );

        IWiredNode
            aN = aE.getNode(),
            aaN = aaE.getNode(),
            bN = bE.getNode(),
            bbN = bbE.getNode(),
            cN = cE.getNode();

        aN.getNetwork().connect( aN, aaN );
        bN.getNetwork().connect( bN, bbN );

        cN.getNetwork().connect( aN, cN );
        cN.getNetwork().connect( bN, cN );

        cN.getNetwork().remove( cN );

        assertNotEquals( "A's and B's network must not be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's and A_'s network must be equal", aN.getNetwork(), aaN.getNetwork() );
        assertEquals( "B's and B_'s network must be equal", bN.getNetwork(), bbN.getNetwork() );

        assertEquals( "A's network should be A and A_", Sets.newHashSet( aN, aaN ), nodes( aN.getNetwork() ) );
        assertEquals( "B's network should be B and B_", Sets.newHashSet( bN, bbN ), nodes( bN.getNetwork() ) );
        assertEquals( "C's network should be C", Sets.newHashSet( cN ), nodes( cN.getNetwork() ) );

        assertEquals( "A's peripheral set should be A and A_", Sets.newHashSet( "a", "a_" ), aE.allPeripherals().keySet() );
        assertEquals( "B's peripheral set should be B and B_", Sets.newHashSet( "b", "b_" ), bE.allPeripherals().keySet() );
        assertEquals( "C's peripheral set should be empty", Sets.newHashSet(), cE.allPeripherals().keySet() );
    }

    @Test
    @Ignore( "Takes a long time to run, mostly for stress testing" )
    public void testLarge()
    {
        final int BRUTE_SIZE = 16;
        final int TOGGLE_CONNECTION_TIMES = 5;
        final int TOGGLE_NODE_TIMES = 5;

        Grid<IWiredNode> grid = new Grid<>( BRUTE_SIZE );
        grid.map( ( existing, pos ) -> new NetworkElement( null, null, "n_" + pos ).getNode() );

        // Test connecting
        {
            long start = System.nanoTime();

            grid.forEach( ( existing, pos ) -> {
                for( EnumFacing facing : EnumFacing.VALUES )
                {
                    BlockPos offset = pos.offset( facing );
                    if( (offset.getX() > BRUTE_SIZE / 2) == (pos.getX() > BRUTE_SIZE / 2) )
                    {
                        IWiredNode other = grid.get( offset );
                        if( other != null ) existing.getNetwork().connect( existing, other );
                    }
                }
            } );

            long end = System.nanoTime();

            System.out.printf( "Connecting %s³ nodes took %s seconds\n", BRUTE_SIZE, (end - start) * 1e-9 );
        }

        // Test toggling
        {
            IWiredNode left = grid.get( new BlockPos( BRUTE_SIZE / 2, 0, 0 ) );
            IWiredNode right = grid.get( new BlockPos( BRUTE_SIZE / 2 + 1, 0, 0 ) );
            assertNotEquals( left.getNetwork(), right.getNetwork() );

            long start = System.nanoTime();
            for( int i = 0; i < TOGGLE_CONNECTION_TIMES; i++ )
            {
                left.getNetwork().connect( left, right );
                left.getNetwork().disconnect( left, right );
            }

            long end = System.nanoTime();

            System.out.printf( "Toggling connection %s times took %s seconds\n", TOGGLE_CONNECTION_TIMES, (end - start) * 1e-9 );
        }

        {
            IWiredNode left = grid.get( new BlockPos( BRUTE_SIZE / 2, 0, 0 ) );
            IWiredNode right = grid.get( new BlockPos( BRUTE_SIZE / 2 + 1, 0, 0 ) );
            IWiredNode centre = new NetworkElement( null, null, "c" ).getNode();
            assertNotEquals( left.getNetwork(), right.getNetwork() );

            long start = System.nanoTime();
            for( int i = 0; i < TOGGLE_NODE_TIMES; i++ )
            {
                left.getNetwork().connect( left, centre );
                right.getNetwork().connect( right, centre );

                left.getNetwork().remove( centre );
            }

            long end = System.nanoTime();

            System.out.printf( "Toggling node %s times took %s seconds\n", TOGGLE_NODE_TIMES, (end - start) * 1e-9 );
        }
    }

    private static class NetworkElement implements IWiredElement
    {
        private final World world;
        private final Vec3d position;
        private final String id;
        private final IWiredNode node;
        private final Map<String, IPeripheral> localPeripherals = Maps.newHashMap();
        private final Map<String, IPeripheral> remotePeripherals = Maps.newHashMap();

        private NetworkElement( World world, Vec3d position, String id )
        {
            this.world = world;
            this.position = position;
            this.id = id;
            this.node = ComputerCraftAPI.createWiredNodeForElement( this );
            this.addPeripheral( id );
        }

        @Nonnull
        @Override
        public World getWorld()
        {
            return world;
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            return position;
        }

        @Nonnull
        @Override
        public String getSenderID()
        {
            return id;
        }

        @Override
        public String toString()
        {
            return "NetworkElement{" + id + "}";
        }

        @Nonnull
        @Override
        public IWiredNode getNode()
        {
            return node;
        }

        @Override
        public void networkChanged( @Nonnull IWiredNetworkChange change )
        {
            remotePeripherals.keySet().removeAll( change.peripheralsRemoved().keySet() );
            remotePeripherals.putAll( change.peripheralsAdded() );
        }

        public NetworkElement addPeripheral( String name )
        {
            localPeripherals.put( name, new NetworkPeripheral() );
            getNode().updatePeripherals( localPeripherals );
            return this;
        }

        @Nonnull
        public Map<String, IPeripheral> allPeripherals()
        {
            return remotePeripherals;
        }
    }

    private static class NetworkPeripheral implements IPeripheral
    {
        @Nonnull
        @Override
        public String getType()
        {
            return "test";
        }

        @Nonnull
        @Override
        public String[] getMethodNames()
        {
            return new String[0];
        }

        @Nullable
        @Override
        public Object[] callMethod( @Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
        {
            return new Object[0];
        }

        @Override
        public boolean equals( @Nullable IPeripheral other )
        {
            return this == other;
        }
    }

    private static class Grid<T>
    {
        private final int size;
        private final T[] box;

        @SuppressWarnings( "unchecked" )
        public Grid( int size )
        {
            this.size = size;
            this.box = (T[]) new Object[size * size * size];
        }

        public void set( BlockPos pos, T elem )
        {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();

            if( x >= 0 && x < size && y >= 0 && y < size && z >= 0 && z < size )
            {
                box[x * size * size + y * size + z] = elem;
            }
            else
            {
                throw new IndexOutOfBoundsException( pos.toString() );
            }
        }

        public T get( BlockPos pos )
        {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();

            return x >= 0 && x < size && y >= 0 && y < size && z >= 0 && z < size
                ? box[x * size * size + y * size + z]
                : null;
        }

        public void forEach( BiConsumer<T, BlockPos> transform )
        {
            for( int x = 0; x < size; x++ )
            {
                for( int y = 0; y < size; y++ )
                {
                    for( int z = 0; z < size; z++ )
                    {
                        transform.accept( box[x * size * size + y * size + z], new BlockPos( x, y, z ) );
                    }
                }
            }
        }

        public void map( BiFunction<T, BlockPos, T> transform )
        {
            for( int x = 0; x < size; x++ )
            {
                for( int y = 0; y < size; y++ )
                {
                    for( int z = 0; z < size; z++ )
                    {
                        box[x * size * size + y * size + z] = transform.apply( box[x * size * size + y * size + z], new BlockPos( x, y, z ) );
                    }
                }
            }
        }
    }

    private static Set<WiredNode> nodes( IWiredNetwork network )
    {
        return ((WiredNetwork) network).nodes;
    }

    private static Set<WiredNode> neighbours( IWiredNode node )
    {
        return ((WiredNode) node).neighbours;
    }
}

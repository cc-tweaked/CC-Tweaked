/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.wired;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNetwork;
import dan200.computercraft.api.network.wired.IWiredNetworkChange;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkTest
{
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

        assertNotEquals( aN.getNetwork(), bN.getNetwork(), "A's and B's network must be different" );
        assertNotEquals( aN.getNetwork(), cN.getNetwork(), "A's and C's network must be different" );
        assertNotEquals( bN.getNetwork(), cN.getNetwork(), "B's and C's network must be different" );

        assertTrue( aN.getNetwork().connect( aN, bN ), "Must be able to add connection" );
        assertFalse( aN.getNetwork().connect( aN, bN ), "Cannot add connection twice" );

        assertEquals( aN.getNetwork(), bN.getNetwork(), "A's and B's network must be equal" );
        assertEquals( Sets.newHashSet( aN, bN ), nodes( aN.getNetwork() ), "A's network should be A and B" );

        assertEquals( Sets.newHashSet( "a", "b" ), aE.allPeripherals().keySet(), "A's peripheral set should be A, B" );
        assertEquals( Sets.newHashSet( "a", "b" ), bE.allPeripherals().keySet(), "B's peripheral set should be A, B" );

        aN.getNetwork().connect( aN, cN );

        assertEquals( aN.getNetwork(), bN.getNetwork(), "A's and B's network must be equal" );
        assertEquals( aN.getNetwork(), cN.getNetwork(), "A's and C's network must be equal" );
        assertEquals( Sets.newHashSet( aN, bN, cN ), nodes( aN.getNetwork() ), "A's network should be A, B and C" );

        assertEquals( Sets.newHashSet( bN, cN ), neighbours( aN ), "A's neighbour set should be B, C" );
        assertEquals( Sets.newHashSet( aN ), neighbours( bN ), "B's neighbour set should be A" );
        assertEquals( Sets.newHashSet( aN ), neighbours( cN ), "C's neighbour set should be A" );

        assertEquals( Sets.newHashSet( "a", "b", "c" ), aE.allPeripherals().keySet(), "A's peripheral set should be A, B, C" );
        assertEquals( Sets.newHashSet( "a", "b", "c" ), bE.allPeripherals().keySet(), "B's peripheral set should be A, B, C" );
        assertEquals( Sets.newHashSet( "a", "b", "c" ), cE.allPeripherals().keySet(), "C's peripheral set should be A, B, C" );
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

        assertEquals( aN.getNetwork(), bN.getNetwork(), "A's and B's network must be equal" );
        assertEquals( aN.getNetwork(), cN.getNetwork(), "A's and C's network must be equal" );
        assertEquals( Sets.newHashSet( aN, bN, cN ), nodes( aN.getNetwork() ), "A's network should be A, B and C" );

        assertEquals( Sets.newHashSet( "a", "b", "c" ), aE.allPeripherals().keySet(), "A's peripheral set should be A, B, C" );
        assertEquals( Sets.newHashSet( "a", "b", "c" ), bE.allPeripherals().keySet(), "B's peripheral set should be A, B, C" );
        assertEquals( Sets.newHashSet( "a", "b", "c" ), cE.allPeripherals().keySet(), "C's peripheral set should be A, B, C" );
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

        assertNotEquals( aN.getNetwork(), bN.getNetwork(), "A's and B's network must not be equal" );
        assertEquals( aN.getNetwork(), cN.getNetwork(), "A's and C's network must be equal" );
        assertEquals( Sets.newHashSet( aN, cN ), nodes( aN.getNetwork() ), "A's network should be A and C" );
        assertEquals( Sets.newHashSet( bN ), nodes( bN.getNetwork() ), "B's network should be B" );

        assertEquals( Sets.newHashSet( "a", "c" ), aE.allPeripherals().keySet(), "A's peripheral set should be A, C" );
        assertEquals( Sets.newHashSet( "b" ), bE.allPeripherals().keySet(), "B's peripheral set should be B" );
        assertEquals( Sets.newHashSet( "a", "c" ), cE.allPeripherals().keySet(), "C's peripheral set should be A, C" );
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

        assertNotEquals( aN.getNetwork(), bN.getNetwork(), "A's and B's network must not be equal" );
        assertEquals( aN.getNetwork(), aaN.getNetwork(), "A's and A_'s network must be equal" );
        assertEquals( bN.getNetwork(), bbN.getNetwork(), "B's and B_'s network must be equal" );

        assertEquals( Sets.newHashSet( aN, aaN ), nodes( aN.getNetwork() ), "A's network should be A and A_" );
        assertEquals( Sets.newHashSet( bN, bbN ), nodes( bN.getNetwork() ), "B's network should be B and B_" );

        assertEquals( Sets.newHashSet( "a", "a_" ), aE.allPeripherals().keySet(), "A's peripheral set should be A and A_" );
        assertEquals( Sets.newHashSet( "b", "b_" ), bE.allPeripherals().keySet(), "B's peripheral set should be B and B_" );
    }

    @Test
    public void testRemoveSingle()
    {
        NetworkElement aE = new NetworkElement( null, null, "a" );
        IWiredNode aN = aE.getNode();

        IWiredNetwork network = aN.getNetwork();
        assertFalse( aN.remove(), "Cannot remove node from an empty network" );
        assertEquals( network, aN.getNetwork(), "Networks are same before and after" );
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

        assertTrue( aN.getNetwork().remove( bN ), "Must be able to remove node" );
        assertFalse( aN.getNetwork().remove( bN ), "Cannot remove a second time" );

        assertNotEquals( aN.getNetwork(), bN.getNetwork(), "A's and B's network must not be equal" );
        assertEquals( aN.getNetwork(), cN.getNetwork(), "A's and C's network must be equal" );

        assertEquals( Sets.newHashSet( aN, cN ), nodes( aN.getNetwork() ), "A's network should be A and C" );
        assertEquals( Sets.newHashSet( bN ), nodes( bN.getNetwork() ), "B's network should be B" );

        assertEquals( Sets.newHashSet( "a", "c" ), aE.allPeripherals().keySet(), "A's peripheral set should be A, C" );
        assertEquals( Sets.newHashSet(), bE.allPeripherals().keySet(), "B's peripheral set should be empty" );
        assertEquals( Sets.newHashSet( "a", "c" ), cE.allPeripherals().keySet(), "C's peripheral set should be A, C" );
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

        assertNotEquals( aN.getNetwork(), bN.getNetwork(), "A's and B's network must not be equal" );
        assertEquals( aN.getNetwork(), aaN.getNetwork(), "A's and A_'s network must be equal" );
        assertEquals( bN.getNetwork(), bbN.getNetwork(), "B's and B_'s network must be equal" );

        assertEquals( Sets.newHashSet( aN, aaN ), nodes( aN.getNetwork() ), "A's network should be A and A_" );
        assertEquals( Sets.newHashSet( bN, bbN ), nodes( bN.getNetwork() ), "B's network should be B and B_" );
        assertEquals( Sets.newHashSet( cN ), nodes( cN.getNetwork() ), "C's network should be C" );

        assertEquals( Sets.newHashSet( "a", "a_" ), aE.allPeripherals().keySet(), "A's peripheral set should be A and A_" );
        assertEquals( Sets.newHashSet( "b", "b_" ), bE.allPeripherals().keySet(), "B's peripheral set should be B and B_" );
        assertEquals( Sets.newHashSet(), cE.allPeripherals().keySet(), "C's peripheral set should be empty" );
    }

    private static final int BRUTE_SIZE = 16;
    private static final int TOGGLE_CONNECTION_TIMES = 5;
    private static final int TOGGLE_NODE_TIMES = 5;

    @Test
    @Disabled( "Takes a long time to run, mostly for stress testing" )
    public void testLarge()
    {
        Grid<IWiredNode> grid = new Grid<>( BRUTE_SIZE );
        grid.map( ( existing, pos ) -> new NetworkElement( null, null, "n_" + pos ).getNode() );

        // Test connecting
        {
            long start = System.nanoTime();

            grid.forEach( ( existing, pos ) -> {
                for( Direction facing : DirectionUtil.FACINGS )
                {
                    BlockPos offset = pos.relative( facing );
                    if( offset.getX() > BRUTE_SIZE / 2 == pos.getX() > BRUTE_SIZE / 2 )
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

    private static final class NetworkElement implements IWiredElement
    {
        private final Level world;
        private final Vec3 position;
        private final String id;
        private final IWiredNode node;
        private final Map<String, IPeripheral> localPeripherals = Maps.newHashMap();
        private final Map<String, IPeripheral> remotePeripherals = Maps.newHashMap();

        private NetworkElement( Level world, Vec3 position, String id )
        {
            this.world = world;
            this.position = position;
            this.id = id;
            this.node = ComputerCraftAPI.createWiredNodeForElement( this );
            this.addPeripheral( id );
        }

        @Nonnull
        @Override
        public Level getLevel()
        {
            return world;
        }

        @Nonnull
        @Override
        public Vec3 getPosition()
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
        Grid( int size )
        {
            this.size = size;
            this.box = (T[]) new Object[size * size * size];
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

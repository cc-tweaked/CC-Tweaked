package dan200.computercraft.core.lua.waluaigi;

import com.google.common.io.ByteStreams;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.TimeoutState;
import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.lua.MachineResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class WaluaigiLuaMachine implements ILuaMachine
{
    private final Computer computer;
    private final TimeoutState timeout;
    private LuaState state;

    public WaluaigiLuaMachine( Computer computer, TimeoutState timeout )
    {
        this.computer = computer;
        this.timeout = timeout;
        this.state = new LuaState( 128 * 1024 * 1024, computer, timeout );
    }

    @Override
    public void addAPI( @Nonnull ILuaAPI api )
    {
        if( state != null && state.isOpen() ) state.addAPI( api );
    }

    @Override
    public MachineResult loadBios( @Nonnull InputStream bios )
    {
        if( state == null || !state.isOpen() ) return MachineResult.error( "Could not create Lua state" );

        byte[] bytes;
        try
        {
            bytes = ByteStreams.toByteArray( bios );
        }
        catch( IOException e )
        {
            ComputerCraft.log.error( "Error reading bios", e );
            return MachineResult.error( "Could not load bios" );
        }

        MachineResult load = state.loadString( ByteBuffer.wrap( bytes ), "@bios.lua" );
        if( load.isError() ) return load;

        return MachineResult.OK;
    }

    @Override
    public MachineResult handleEvent( @Nullable String eventName, @Nullable Object[] arguments )
    {
        if( state == null || !state.isOpen() ) return MachineResult.error( "Machine is not open" );
        return state.handleEvent( eventName, arguments );
    }

    @Override
    public void close()
    {
        state = null;
    }
}

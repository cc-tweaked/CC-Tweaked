/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.core;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.ComputerCraftAPIImpl;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.shared.common.ServerTerminal;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.ComputerDataClientMessage;
import dan200.computercraft.shared.network.client.ComputerDeletedClientMessage;
import dan200.computercraft.shared.network.client.ComputerTerminalClientMessage;
import me.shedaniel.cloth.api.utils.v1.GameInstanceUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

public class ServerComputer extends ServerTerminal implements IComputer, IComputerEnvironment
{
    private final int instanceID;
    private final ComputerFamily family;
    private final Computer computer;
    private World world;
    private BlockPos position;
    private CompoundTag userData;
    private boolean changed;

    private boolean changedLastFrame;
    private int ticksSincePing;

    public ServerComputer( World world, int computerID, String label, int instanceID, ComputerFamily family, int terminalWidth, int terminalHeight )
    {
        super( family != ComputerFamily.NORMAL, terminalWidth, terminalHeight );
        this.instanceID = instanceID;

        this.world = world;
        this.position = null;

        this.family = family;
        this.computer = new Computer( this, this.getTerminal(), computerID );
        this.computer.setLabel( label );
        this.userData = null;
        this.changed = false;

        this.changedLastFrame = false;
        this.ticksSincePing = 0;
    }

    public ComputerFamily getFamily()
    {
        return this.family;
    }

    public World getWorld()
    {
        return this.world;
    }

    public void setWorld( World world )
    {
        this.world = world;
    }

    public BlockPos getPosition()
    {
        return this.position;
    }

    public void setPosition( BlockPos pos )
    {
        this.position = new BlockPos( pos );
    }

    public IAPIEnvironment getAPIEnvironment()
    {
        return this.computer.getAPIEnvironment();
    }

    public Computer getComputer()
    {
        return this.computer;
    }

    @Override
    public void update()
    {
        super.update();
        this.computer.tick();

        this.changedLastFrame = this.computer.pollAndResetChanged() || this.changed;
        this.changed = false;

        this.ticksSincePing++;
    }

    public void keepAlive()
    {
        this.ticksSincePing = 0;
    }

    public boolean hasTimedOut()
    {
        return this.ticksSincePing > 100;
    }

    public void unload()
    {
        this.computer.unload();
    }

    public CompoundTag getUserData()
    {
        if( this.userData == null )
        {
            this.userData = new CompoundTag();
        }
        return this.userData;
    }

    public void updateUserData()
    {
        this.changed = true;
    }

    public void broadcastState( boolean force )
    {
        if( this.hasOutputChanged() || force )
        {
            // Send computer state to all clients
            MinecraftServer server = GameInstanceUtils.getServer();
            if( server != null )
            {
                NetworkHandler.sendToAllPlayers( server, this.createComputerPacket() );
            }
        }

        if( this.hasTerminalChanged() || force )
        {
            MinecraftServer server = GameInstanceUtils.getServer();
            if( server != null )
            {
                // Send terminal state to clients who are currently interacting with the computer.

                NetworkMessage packet = null;
                for( PlayerEntity player : server.getPlayerManager()
                    .getPlayerList() )
                {
                    if( this.isInteracting( player ) )
                    {
                        if( packet == null )
                        {
                            packet = this.createTerminalPacket();
                        }
                        NetworkHandler.sendToPlayer( player, packet );
                    }
                }
            }
        }
    }

    public boolean hasOutputChanged()
    {
        return this.changedLastFrame;
    }

    private NetworkMessage createComputerPacket()
    {
        return new ComputerDataClientMessage( this );
    }

    protected boolean isInteracting( PlayerEntity player )
    {
        return this.getContainer( player ) != null;
    }

    protected NetworkMessage createTerminalPacket()
    {
        return new ComputerTerminalClientMessage( this.getInstanceID(), this.write() );
    }

    @Nullable
    public IContainerComputer getContainer( PlayerEntity player )
    {
        if( player == null )
        {
            return null;
        }

        ScreenHandler container = player.currentScreenHandler;
        if( !(container instanceof IContainerComputer) )
        {
            return null;
        }

        IContainerComputer computerContainer = (IContainerComputer) container;
        return computerContainer.getComputer() != this ? null : computerContainer;
    }

    @Override
    public int getInstanceID()
    {
        return this.instanceID;
    }

    @Override
    public void turnOn()
    {
        // Turn on
        this.computer.turnOn();
    }

    // IComputer

    @Override
    public void shutdown()
    {
        // Shutdown
        this.computer.shutdown();
    }

    @Override
    public void reboot()
    {
        // Reboot
        this.computer.reboot();
    }

    @Override
    public void queueEvent( String event, Object[] arguments )
    {
        // Queue event
        this.computer.queueEvent( event, arguments );
    }

    @Override
    public boolean isOn()
    {
        return this.computer.isOn();
    }

    @Override
    public boolean isCursorDisplayed()
    {
        return this.computer.isOn() && this.computer.isBlinking();
    }

    public void sendComputerState( PlayerEntity player )
    {
        // Send state to client
        NetworkHandler.sendToPlayer( player, this.createComputerPacket() );
    }

    public void sendTerminalState( PlayerEntity player )
    {
        // Send terminal state to client
        NetworkHandler.sendToPlayer( player, this.createTerminalPacket() );
    }

    public void broadcastDelete()
    {
        // Send deletion to client
        MinecraftServer server = GameInstanceUtils.getServer();
        if( server != null )
        {
            NetworkHandler.sendToAllPlayers( server, new ComputerDeletedClientMessage( this.getInstanceID() ) );
        }
    }

    public int getID()
    {
        return this.computer.getID();
    }

    public void setID( int id )
    {
        this.computer.setID( id );
    }

    public String getLabel()
    {
        return this.computer.getLabel();
    }

    public void setLabel( String label )
    {
        this.computer.setLabel( label );
    }

    public int getRedstoneOutput( ComputerSide side )
    {
        return this.computer.getEnvironment()
            .getExternalRedstoneOutput( side );
    }

    public void setRedstoneInput( ComputerSide side, int level )
    {
        this.computer.getEnvironment()
            .setRedstoneInput( side, level );
    }

    public int getBundledRedstoneOutput( ComputerSide side )
    {
        return this.computer.getEnvironment()
            .getExternalBundledRedstoneOutput( side );
    }

    public void setBundledRedstoneInput( ComputerSide side, int combination )
    {
        this.computer.getEnvironment()
            .setBundledRedstoneInput( side, combination );
    }

    public void addAPI( ILuaAPI api )
    {
        this.computer.addApi( api );
    }

    // IComputerEnvironment implementation

    public void setPeripheral( ComputerSide side, IPeripheral peripheral )
    {
        this.computer.getEnvironment()
            .setPeripheral( side, peripheral );
    }

    public IPeripheral getPeripheral( ComputerSide side )
    {
        return this.computer.getEnvironment()
            .getPeripheral( side );
    }

    @Override
    public int getDay()
    {
        return (int) ((this.world.getTimeOfDay() + 6000) / 24000) + 1;
    }

    @Override
    public double getTimeOfDay()
    {
        return (this.world.getTimeOfDay() + 6000) % 24000 / 1000.0;
    }

    @Override
    public long getComputerSpaceLimit()
    {
        return ComputerCraft.computerSpaceLimit;
    }

    @Nonnull
    @Override
    public String getHostString()
    {
        return String.format( "ComputerCraft %s (Minecraft %s)", ComputerCraftAPI.getInstalledVersion(), "1.16.4" );
    }

    @Nonnull
    @Override
    public String getUserAgent()
    {
        return ComputerCraft.MOD_ID + "/" + ComputerCraftAPI.getInstalledVersion();
    }

    @Override
    public int assignNewID()
    {
        return ComputerCraftAPI.createUniqueNumberedSaveDir( this.world, "computer" );
    }

    @Override
    public IWritableMount createSaveDirMount( String subPath, long capacity )
    {
        return ComputerCraftAPI.createSaveDirMount( this.world, subPath, capacity );
    }

    @Override
    public IMount createResourceMount( String domain, String subPath )
    {
        return ComputerCraftAPI.createResourceMount( domain, subPath );
    }

    @Override
    public InputStream createResourceFile( String domain, String subPath )
    {
        return ComputerCraftAPIImpl.getResourceFile( domain, subPath );
    }
}

/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.ComputerTerminalClientMessage;
import dan200.computercraft.shared.network.client.TerminalState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.versions.mcp.MCPVersion;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class ServerComputer implements InputHandler, IComputerEnvironment
{
    private final int instanceID;

    private ServerWorld world;
    private BlockPos position;

    private final ComputerFamily family;
    private final Computer computer;

    private final Terminal terminal;
    private final AtomicBoolean terminalChanged = new AtomicBoolean( false );

    private boolean changedLastFrame;
    private int ticksSincePing;

    public ServerComputer( ServerWorld world, int computerID, String label, ComputerFamily family, int terminalWidth, int terminalHeight )
    {
        this.world = world;
        this.family = family;

        instanceID = ServerComputerRegistry.INSTANCE.getUnusedInstanceID();
        terminal = new Terminal( terminalWidth, terminalHeight, family != ComputerFamily.NORMAL, this::markTerminalChanged );

        computer = new Computer( this, terminal, computerID );
        computer.setLabel( label );
    }

    public ComputerFamily getFamily()
    {
        return family;
    }

    public ServerWorld getWorld()
    {
        return world;
    }

    public void setWorld( ServerWorld world )
    {
        this.world = world;
    }

    public BlockPos getPosition()
    {
        return position;
    }

    public void setPosition( BlockPos pos )
    {
        position = new BlockPos( pos );
    }

    public IAPIEnvironment getAPIEnvironment()
    {
        return computer.getAPIEnvironment();
    }

    public Computer getComputer()
    {
        return computer;
    }

    protected void markTerminalChanged()
    {
        terminalChanged.set( true );
    }


    public void tickServer()
    {
        ticksSincePing++;

        computer.tick();

        changedLastFrame = computer.pollAndResetChanged();
        if( terminalChanged.getAndSet( false ) ) onTerminalChanged();
    }

    protected void onTerminalChanged()
    {
        sendToAllInteracting( c -> new ComputerTerminalClientMessage( c, getTerminalState() ) );
    }

    public TerminalState getTerminalState()
    {
        return new TerminalState( terminal );
    }

    public void keepAlive()
    {
        ticksSincePing = 0;
    }

    public boolean hasTimedOut()
    {
        return ticksSincePing > 100;
    }

    public boolean hasOutputChanged()
    {
        return changedLastFrame;
    }

    public int register()
    {
        ServerComputerRegistry.INSTANCE.add( instanceID, this );
        return instanceID;
    }

    void unload()
    {
        computer.unload();
    }

    public void close()
    {
        unload();
        ServerComputerRegistry.INSTANCE.remove( instanceID );
    }

    private void sendToAllInteracting( Function<Container, NetworkMessage> createPacket )
    {
        MinecraftServer server = world.getServer();

        for( ServerPlayerEntity player : server.getPlayerList().getPlayers() )
        {
            if( player.containerMenu instanceof ComputerMenu && ((ComputerMenu) player.containerMenu).getComputer() == this )
            {
                NetworkHandler.sendToPlayer( player, createPacket.apply( player.containerMenu ) );
            }
        }
    }

    protected void onRemoved()
    {
    }

    public int getInstanceID()
    {
        return instanceID;
    }

    public int getID()
    {
        return computer.getID();
    }

    public String getLabel()
    {
        return computer.getLabel();
    }

    public boolean isOn()
    {
        return computer.isOn();
    }

    public ComputerState getState()
    {
        if( !isOn() ) return ComputerState.OFF;
        return computer.isBlinking() ? ComputerState.BLINKING : ComputerState.ON;
    }

    @Override
    public void turnOn()
    {
        // Turn on
        computer.turnOn();
    }

    @Override
    public void shutdown()
    {
        // Shutdown
        computer.shutdown();
    }

    @Override
    public void reboot()
    {
        // Reboot
        computer.reboot();
    }

    @Override
    public void queueEvent( String event, Object[] arguments )
    {
        // Queue event
        computer.queueEvent( event, arguments );
    }

    public int getRedstoneOutput( ComputerSide side )
    {
        return computer.getEnvironment().getExternalRedstoneOutput( side );
    }

    public void setRedstoneInput( ComputerSide side, int level )
    {
        computer.getEnvironment().setRedstoneInput( side, level );
    }

    public int getBundledRedstoneOutput( ComputerSide side )
    {
        return computer.getEnvironment().getExternalBundledRedstoneOutput( side );
    }

    public void setBundledRedstoneInput( ComputerSide side, int combination )
    {
        computer.getEnvironment().setBundledRedstoneInput( side, combination );
    }

    public void addAPI( ILuaAPI api )
    {
        computer.addApi( api );
    }

    public void setPeripheral( ComputerSide side, IPeripheral peripheral )
    {
        computer.getEnvironment().setPeripheral( side, peripheral );
    }

    public IPeripheral getPeripheral( ComputerSide side )
    {
        return computer.getEnvironment().getPeripheral( side );
    }

    public void setLabel( String label )
    {
        computer.setLabel( label );
    }

    // IComputerEnvironment implementation

    @Override
    public double getTimeOfDay()
    {
        return (world.getDayTime() + 6000) % 24000 / 1000.0;
    }

    @Override
    public int getDay()
    {
        return (int) ((world.getDayTime() + 6000) / 24000) + 1;
    }

    @Override
    public IWritableMount createRootMount()
    {
        return ComputerCraftAPI.createSaveDirMount( world, "computer/" + computer.getID(), ComputerCraft.computerSpaceLimit );
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

    @Nonnull
    @Override
    public String getHostString()
    {
        return String.format( "ComputerCraft %s (Minecraft %s)", ComputerCraftAPI.getInstalledVersion(), MCPVersion.getMCVersion() );
    }

    @Nonnull
    @Override
    public String getUserAgent()
    {
        return ComputerCraft.MOD_ID + "/" + ComputerCraftAPI.getInstalledVersion();
    }
}

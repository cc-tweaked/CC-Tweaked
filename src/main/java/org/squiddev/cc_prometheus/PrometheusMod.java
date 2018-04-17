package org.squiddev.cc_prometheus;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

@Mod(
    modid = "cc_prometheus",
    name = "CC Prometheus exporter",
    version = "1.0",
    dependencies = "required-after:computercraft",
    acceptableRemoteVersions = "*"
)
public class PrometheusMod
{
    @Mod.EventHandler
    public void onServerStarting( FMLServerStartingEvent event )
    {
        PrometheusController.startServer();
    }

    @Mod.EventHandler
    public void onServerStopping( FMLServerStoppingEvent event )
    {
        PrometheusController.stopServer();
    }

    @NetworkCheckHandler
    public boolean onNetworkConnect( Map<String, String> mods, Side side )
    {
        // This can work on the server or on the client
        return true;
    }
}

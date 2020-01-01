/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

/**
 * A stub mod for CC: Tweaked. This doesn't have any functionality (everything of note is done in
 * {@link ComputerCraft}), but people may depend on this if they require CC: Tweaked functionality.
 */
@Mod(
    modid = "cctweaked", name = ComputerCraft.NAME, version = ComputerCraft.VERSION,
    acceptableRemoteVersions = "*"
)
public class CCTweaked
{
    @NetworkCheckHandler
    public boolean onNetworkConnect( Map<String, String> mods, Side side )
    {
        return true;
    }
}

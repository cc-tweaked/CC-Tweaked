/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

@Mod( TestMod.MOD_ID )
public class TestMod
{
    public static final Path sourceDir = Paths.get( "../../src/testMod/server-files" ).normalize().toAbsolutePath();

    public static final String MOD_ID = "cctest";

    public static final Logger log = LogManager.getLogger( MOD_ID );

    public TestMod()
    {
        log.info( "CC: Test initialised" );
        ComputerCraftAPI.registerAPIFactory( TestAPI::new );

        StructureUtils.testStructuresDir = sourceDir.resolve( "structures" ).toString();
    }
}

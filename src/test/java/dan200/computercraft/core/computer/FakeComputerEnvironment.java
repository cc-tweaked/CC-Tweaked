/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.core.filesystem.FileMount;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

public class FakeComputerEnvironment implements IComputerEnvironment
{
    private final boolean colour;
    private final int id;

    public FakeComputerEnvironment( int id, boolean colour )
    {
        this.colour = colour;
        this.id = id;
    }

    @Override
    public int getDay()
    {
        return 0;
    }

    @Override
    public double getTimeOfDay()
    {
        return 0;
    }

    @Override
    public boolean isColour()
    {
        return colour;
    }

    @Override
    public int assignNewID()
    {
        return id;
    }

    @Override
    public IWritableMount createSaveDirMount( String subPath, long capacity )
    {
        return new FileMount( new File( "computer/" + subPath ), capacity );
    }

    @Override
    public IMount createResourceMount( String domain, String subPath )
    {
        String fullPath = "assets/" + domain + "/" + subPath;
        URL url = ComputerCraft.class.getProtectionDomain().getCodeSource().getLocation();
        File file = new File( url.getPath(), fullPath );
        if( !file.exists() ) file = new File( "src/main/resources", fullPath );

        if( !file.exists() ) throw new RuntimeException( "Cannot find ROM in " + file );

        return new FileMount( file, 0 );
    }

    @Override
    public InputStream createResourceFile( String domain, String subPath )
    {
        String fullPath = "assets/" + domain + "/" + subPath;
        return ComputerCraft.class.getClassLoader().getResourceAsStream( fullPath );
    }

    @Override
    public long getComputerSpaceLimit()
    {
        return ComputerCraft.computerSpaceLimit;
    }

    @Override
    public String getHostString()
    {
        return "ComputerCraft ${version} (Minecraft " + Loader.MC_VERSION + ")";
    }
}

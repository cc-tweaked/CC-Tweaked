/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.upload;

import dan200.computercraft.ComputerCraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class FileUpload
{
    public static final int CHECKSUM_LENGTH = 32;

    private final String name;
    private final int length;
    private final ByteBuffer bytes;
    private final byte[] checksum;

    public FileUpload( String name, ByteBuffer bytes, byte[] checksum )
    {
        this.name = name;
        this.bytes = bytes;
        length = bytes.remaining();
        this.checksum = checksum;
    }

    @Nonnull
    public String getName()
    {
        return name;
    }

    @Nonnull
    public ByteBuffer getBytes()
    {
        return bytes;
    }

    public int getLength()
    {
        return length;
    }

    @Nonnull
    public byte[] getChecksum()
    {
        return checksum;
    }

    public boolean checksumMatches()
    {
        // This is meant to be a checksum. Doesn't need to be cryptographically secure, hence non-constant time.
        byte[] digest = getDigest( bytes );
        return digest != null && Arrays.equals( checksum, digest );
    }

    @Nullable
    public static byte[] getDigest( ByteBuffer bytes )
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance( "SHA-256" );
            digest.update( bytes.duplicate() );
            return digest.digest();
        }
        catch( NoSuchAlgorithmException e )
        {
            ComputerCraft.log.warn( "Failed to compute digest ({})", e.toString() );
            return null;
        }
    }
}

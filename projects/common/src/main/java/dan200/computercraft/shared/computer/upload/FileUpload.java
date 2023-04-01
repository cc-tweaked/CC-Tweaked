// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.upload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class FileUpload {
    private static final Logger LOG = LoggerFactory.getLogger(FileUpload.class);

    public static final int CHECKSUM_LENGTH = 32;

    private final String name;
    private final int length;
    private final ByteBuffer bytes;
    private final byte[] checksum;

    public FileUpload(String name, ByteBuffer bytes, byte[] checksum) {
        this.name = name;
        this.bytes = bytes;
        length = bytes.remaining();
        this.checksum = checksum;
    }

    public String getName() {
        return name;
    }

    public ByteBuffer getBytes() {
        return bytes;
    }

    public int getLength() {
        return length;
    }

    public byte[] getChecksum() {
        return checksum;
    }

    public boolean checksumMatches() {
        // This is meant to be a checksum. Doesn't need to be cryptographically secure, hence non-constant time.
        var digest = getDigest(bytes);
        return digest != null && Arrays.equals(checksum, digest);
    }

    @Nullable
    public static byte[] getDigest(ByteBuffer bytes) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes.duplicate());
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            LOG.warn("Failed to compute digest ({})", e.toString());
            return null;
        }
    }
}

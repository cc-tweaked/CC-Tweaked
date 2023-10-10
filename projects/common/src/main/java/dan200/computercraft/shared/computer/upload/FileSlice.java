// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.upload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;

public record FileSlice(int fileId, int offset, ByteBuffer bytes) {
    private static final Logger LOG = LoggerFactory.getLogger(FileSlice.class);

    public void apply(List<FileUpload> files) {
        if (fileId < 0 || fileId >= files.size()) {
            LOG.warn("File ID is out-of-bounds (0 <= {} < {})", fileId, files.size());
            return;
        }

        var file = files.get(fileId).getBytes();
        if (offset < 0 || offset + bytes.remaining() > file.capacity()) {
            LOG.warn("File offset is out-of-bounds (0 <= {} <= {})", offset, file.capacity() - offset);
            return;
        }

        file.put(offset, bytes, bytes.position(), bytes.remaining());
    }
}

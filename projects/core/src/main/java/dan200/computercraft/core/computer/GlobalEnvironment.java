// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.computer;

import dan200.computercraft.api.filesystem.Mount;

import javax.annotation.Nullable;
import java.io.InputStream;

/**
 * The global environment in which computers reside.
 */
public interface GlobalEnvironment {
    /**
     * Get a "host" string describing the program hosting CC. It should be of the form {@literal ComputerCraft
     * $CC_VERSION ($HOST)}, where {@literal $HOST} is a user-defined string such as {@literal Minecraft 1.19}.
     *
     * @return The host string.
     */
    String getHostString();

    /**
     * Get the HTTP user-agent to use for requests. This should be similar to {@link #getHostString()} , but in the form
     * of a HTTP User-Agent.
     *
     * @return The HTTP
     */
    String getUserAgent();

    /**
     * Create a mount from mod-provided resources.
     *
     * @param domain  The domain (i.e. mod id) providing resources.
     * @param subPath The path to these resources under the domain.
     * @return The created mount or {@code null} if it could not be created.
     */
    @Nullable
    Mount createResourceMount(String domain, String subPath);

    /**
     * Open a single mod-provided file.
     *
     * @param domain  The domain (i.e. mod id) providing resources.
     * @param subPath The path to these files under the domain.
     * @return The opened file or {@code null} if it could not be opened.
     */
    @Nullable
    InputStream createResourceFile(String domain, String subPath);
}

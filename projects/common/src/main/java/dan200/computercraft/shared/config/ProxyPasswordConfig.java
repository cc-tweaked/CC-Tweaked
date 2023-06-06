// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.config;

import dan200.computercraft.core.CoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

record ProxyPasswordConfig(String username, String password) {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyPasswordConfig.class);

    @Nullable
    private static ProxyPasswordConfig loadFromFile(@Nullable Path path) {
        if (path == null || !path.toFile().exists()) return null;

        try (var br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            var line = br.readLine();
            if (line == null) return null;

            var parts = line.trim().split(":", 2);
            if (parts.length == 0) return null;

            return new ProxyPasswordConfig(parts[0], parts.length == 2 ? parts[1] : "");
        } catch (IOException e) {
            LOG.error("Failed to load proxy password from {}.", path, e);
            return null;
        }
    }

    static void init(@Nullable Path path) {
        var config = loadFromFile(path);
        if (config == null) {
            CoreConfig.httpProxyUsername = "";
            CoreConfig.httpProxyPassword = "";
        } else {
            CoreConfig.httpProxyUsername = config.username;
            CoreConfig.httpProxyPassword = config.password;
        }
    }
}

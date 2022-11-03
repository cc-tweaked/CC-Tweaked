/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dan200.computercraft.ComputerCraft;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class IDAssigner {
    public static final String COMPUTER = "computer";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ID_TOKEN = new TypeToken<Map<String, Integer>>() {
    }.getType();

    private final Path idFile;
    private @Nullable Map<String, Integer> ids;

    public IDAssigner(Path path) {
        idFile = path;
    }

    public synchronized int getNextId(String kind) {
        if (ids == null) ids = loadIds();

        var existing = ids.get(kind);
        var next = existing == null ? 0 : existing + 1;
        ids.put(kind, next);

        // We've changed the ID file, so save it back again.
        try (Writer writer = Files.newBufferedWriter(idFile, StandardCharsets.UTF_8)) {
            GSON.toJson(ids, writer);
        } catch (IOException e) {
            ComputerCraft.log.error("Cannot update ID file '{}'", idFile, e);
        }

        return next;
    }

    private Map<String, Integer> loadIds() {
        if (Files.isRegularFile(idFile)) {
            try (Reader reader = Files.newBufferedReader(idFile, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, ID_TOKEN);
            } catch (Exception e) {
                ComputerCraft.log.error("Cannot load id file '" + idFile + "'", e);
            }
        } else {
            try {
                Files.createDirectories(idFile.getParent());
            } catch (IOException e) {
                ComputerCraft.log.error("Cannot create owning directory, IDs will not be persisted", e);
            }
        }

        return new HashMap<>();
    }
}

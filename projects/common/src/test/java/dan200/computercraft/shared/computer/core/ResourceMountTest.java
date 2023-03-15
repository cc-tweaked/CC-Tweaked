// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.core;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.test.core.CloseScope;
import dan200.computercraft.test.core.filesystem.MountContract;
import net.minecraft.Util;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.util.Unit;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ResourceMountTest implements MountContract {
    private final CloseScope toClose = new CloseScope();

    @Override
    public Mount createSkeleton() throws IOException {
        var path = Files.createTempDirectory("cctweaked-test");
        toClose.add(() -> MoreFiles.deleteRecursively(path, RecursiveDeleteOption.ALLOW_INSECURE));

        Files.createDirectories(path.resolve("data/computercraft/rom/dir"));
        try (var writer = Files.newBufferedWriter(path.resolve("data/computercraft/rom/dir/file.lua"))) {
            writer.write("print('testing')");
        }
        Files.newBufferedWriter(path.resolve("data/computercraft/rom/f.lua")).close();

        var manager = new ReloadableResourceManager(PackType.SERVER_DATA);
        var reload = manager.createReload(Util.backgroundExecutor(), Util.backgroundExecutor(), CompletableFuture.completedFuture(Unit.INSTANCE), List.of(
            new PathPackResources("resources", path, false)
        ));

        try {
            reload.done().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to load resources", e);
        }

        return new ResourceMount("computercraft", "rom", manager);
    }

    @Override
    public boolean hasFileTimes() {
        return false;
    }

    @AfterEach
    public void after() throws Exception {
        toClose.close();
    }
}

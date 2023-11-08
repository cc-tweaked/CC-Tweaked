// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.test.core.filesystem.MountContract;
import dan200.computercraft.test.core.filesystem.WritableMountContract;
import org.opentest4j.TestAbortedException;

import static dan200.computercraft.api.filesystem.MountConstants.EPOCH;

public class MemoryMountTest implements MountContract, WritableMountContract {
    @Override
    public Mount createSkeleton() {
        var mount = new MemoryMount();
        mount.addFile("f.lua", "");
        mount.addFile("dir/file.lua", "print('testing')", EPOCH, MODIFY_TIME);
        return mount;
    }

    @Override
    public MountAccess createMount(long capacity) {
        var mount = new MemoryMount(capacity);
        return new MountAccess() {
            @Override
            public WritableMount mount() {
                return mount;
            }

            @Override
            public void makeReadOnly(String path) {
                throw new TestAbortedException("Not supported for MemoryMount");
            }

            @Override
            public void ensuresExist() {
            }

            @Override
            public long computeRemainingSpace() {
                return mount.getRemainingSpace();
            }
        };
    }
}

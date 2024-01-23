// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Wraps an existing {@link DataProvider}, passing generated JSON through {@link PrettyJsonWriter}.
 *
 * @param provider The provider to wrap.
 * @param <T>      The type of the provider to wrap.
 */
public record PrettyDataProvider<T extends DataProvider>(T provider) implements DataProvider {
    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        return provider.run(new Output(cachedOutput));
    }

    @Override
    public String getName() {
        return provider.getName();
    }

    private record Output(CachedOutput output) implements CachedOutput {
        @SuppressWarnings("deprecation")
        private static final HashFunction HASH_FUNCTION = Hashing.sha1();

        @Override
        public void writeIfNeeded(Path path, byte[] bytes, HashCode hashCode) throws IOException {
            if (path.getFileName().toString().endsWith(".json")) {
                bytes = PrettyJsonWriter.reformat(bytes);
                hashCode = HASH_FUNCTION.hashBytes(bytes);
            }

            output.writeIfNeeded(path, bytes, hashCode);
        }
    }
}

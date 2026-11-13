// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web;

import com.google.auto.service.AutoService;
import org.teavm.extension.spi.substitution.SimpleSubstitutionPolicy;
import org.teavm.extension.spi.substitution.SubstitutionPolicy;
import org.teavm.extension.spi.substitution.SubstitutionSink;

/**
 * {@link SubstitutionPolicy} that optionally remaps {@code T}-prefixed classes in additional packages.
 */
@AutoService(SubstitutionPolicy.class)
public class CustomSubstitutionPolicy extends SimpleSubstitutionPolicy {
    @Override
    public void contribute(SubstitutionSink sink) {
        sink.selectClasses(named("java.util.concurrent.locks.ReentrantLock"))
            .replacePackage("java.util.concurrent.locks", "cc.tweaked.web.stub")
            .dontFallbackWhenNoSubstitution();

        sink.selectClasses(named("java.nio.channels.FileChannel"))
            .replacePackage("java.nio.channels", "cc.tweaked.web.stub")
            .dontFallbackWhenNoSubstitution();

        sink.selectClasses(inPackage("dan200.computercraft", true)).simpleNamePrefix("T");
        sink.selectClasses(inPackage("io.netty", true)).simpleNamePrefix("T");
        sink.selectClasses(inPackage("org.slf4j", true)).simpleNamePrefix("T");
    }
}

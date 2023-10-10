// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

import java.lang.reflect.Method;

/**
 * A {@link DisplayNameGenerator} which replaces underscores with spaces. This is equivalent to
 * {@link DisplayNameGenerator.ReplaceUnderscores}, but excludes the parameter types.
 *
 * @see DisplayNameGeneration
 */
public class ReplaceUnderscoresDisplayNameGenerator extends DisplayNameGenerator.ReplaceUnderscores {
    @Override
    public String generateDisplayNameForMethod(Class<?> testClass, Method testMethod) {
        return testMethod.getName().replace('_', ' ');
    }
}

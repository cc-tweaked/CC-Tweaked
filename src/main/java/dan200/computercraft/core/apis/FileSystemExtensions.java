// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL
package dan200.computercraft.core.apis;

import com.google.common.base.Splitter;
import dan200.computer.core.FileSystem;
import dan200.computer.core.FileSystemException;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.regex.Pattern;

/**
 * Backports additional methods from {@link FileSystem}.
 */
final class FileSystemExtensions {
    public static String getDirectory(String path) {
        path = sanitizePath(path, true);
        if (path.isEmpty()) {
            return "..";
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return path.substring(0, lastSlash);
        } else {
            return "";
        }
    }

    public static void makeParentDir(FileSystem fileSystem, String path) throws FileSystemException {
        var parent = getDirectory(path);
        if (!parent.isEmpty()) fileSystem.makeDir(parent);
    }

    private static final Pattern threeDotsPattern = Pattern.compile("^\\.{3,}$");

    public static String sanitizePath(String path, boolean allowWildcards) {
        // Allow windowsy slashes
        path = path.replace('\\', '/');

        // Clean the path or illegal characters.
        final char[] specialChars = new char[]{
            '"', ':', '<', '>', '?', '|', // Sorted by ascii value (important)
        };

        StringBuilder cleanName = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c >= 32 && Arrays.binarySearch(specialChars, c) < 0 && (allowWildcards || c != '*')) {
                cleanName.append(c);
            }
        }
        path = cleanName.toString();

        // Collapse the string into its component parts, removing ..'s
        Deque<String> outputParts = new ArrayDeque<String>();
        for (String fullPart : Splitter.on('/').split(path)) {
            String part = fullPart.trim();

            if (part.isEmpty() || part.equals(".") || threeDotsPattern.matcher(part).matches()) {
                // . is redundant
                // ... and more are treated as .
                continue;
            }

            if (part.equals("..")) {
                // .. can cancel out the last folder entered
                if (!outputParts.isEmpty()) {
                    String top = outputParts.peekLast();
                    if (!top.equals("..")) {
                        outputParts.removeLast();
                    } else {
                        outputParts.addLast("..");
                    }
                } else {
                    outputParts.addLast("..");
                }
            } else if (part.length() >= 255) {
                // If part length > 255 and it is the last part
                outputParts.addLast(part.substring(0, 255).trim());
            } else {
                // Anything else we add to the stack
                outputParts.addLast(part);
            }
        }

        return String.join("/", outputParts);
    }
}

// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL
package dan200.computercraft.core.apis;

import com.google.common.base.Splitter;
import dan200.computer.core.FileSystem;
import dan200.computer.core.FileSystemException;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Backports additional methods from {@link FileSystem}.
 */
final class FileSystemExtensions {
    private static void findIn(FileSystem fs, String dir, List<String> matches, Pattern wildPattern) throws FileSystemException {
        String[] list = fs.list(dir);
        for (String entry : list) {
            String entryPath = dir.isEmpty() ? entry : dir + "/" + entry;
            if (wildPattern.matcher(entryPath).matches()) {
                matches.add(entryPath);
            }
            if (fs.isDir(entryPath)) {
                findIn(fs, entryPath, matches, wildPattern);
            }
        }
    }

    public static synchronized String[] find(FileSystem fs, String wildPath) throws FileSystemException {
        // Match all the files on the system
        wildPath = sanitizePath(wildPath, true);

        // If we don't have a wildcard at all just check the file exists
        int starIndex = wildPath.indexOf('*');
        if (starIndex == -1) {
            return fs.exists(wildPath) ? new String[]{ wildPath } : new String[0];
        }

        // Find the all non-wildcarded directories. For instance foo/bar/baz* -> foo/bar
        int prevDir = wildPath.substring(0, starIndex).lastIndexOf('/');
        String startDir = prevDir == -1 ? "" : wildPath.substring(0, prevDir);

        // If this isn't a directory then just abort
        if (!fs.isDir(startDir)) return new String[0];

        // Scan as normal, starting from this directory
        Pattern wildPattern = Pattern.compile("^\\Q" + wildPath.replaceAll("\\*", "\\\\E[^\\\\/]*\\\\Q") + "\\E$");
        List<String> matches = new ArrayList<>();
        findIn(fs, startDir, matches, wildPattern);

        // Return matches
        String[] array = matches.toArray(new String[0]);
        Arrays.sort(array);
        return array;
    }

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

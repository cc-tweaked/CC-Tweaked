/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration;

/**
 * Detect whether Optifine is installed.
 */
public final class Optifine {
    private static final boolean LOADED;

    static {
        boolean loaded;
        try {
            Class.forName("optifine.Installer", false, Optifine.class.getClassLoader());
            loaded = true;
        } catch (ReflectiveOperationException | LinkageError ignore) {
            loaded = false;
        }

        LOADED = loaded;
    }

    private Optifine() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }
}

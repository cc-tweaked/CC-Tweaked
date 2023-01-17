/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Shared log markers for ComputerCraft.
 */
public final class Logging {
    public static final Marker COMPUTER_ERROR = MarkerFactory.getMarker("COMPUTER_ERROR");
    public static final Marker HTTP_ERROR = MarkerFactory.getMarker("COMPUTER_ERROR.HTTP");
    public static final Marker JAVA_ERROR = MarkerFactory.getMarker("COMPUTER_ERROR.JAVA");
    public static final Marker VM_ERROR = MarkerFactory.getMarker("COMPUTER_ERROR.VM");

    static {
        HTTP_ERROR.add(COMPUTER_ERROR);
        JAVA_ERROR.add(COMPUTER_ERROR);
        VM_ERROR.add(COMPUTER_ERROR);
    }

    private Logging() {
    }
}

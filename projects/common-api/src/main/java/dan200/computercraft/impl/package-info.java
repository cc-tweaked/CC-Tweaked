/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

/**
 * Internal interfaces for ComputerCraft's API.
 */
@ApiStatus.Internal
@DefaultQualifier(value = NonNull.class, locations = {
    TypeUseLocation.RETURN,
    TypeUseLocation.PARAMETER,
    TypeUseLocation.FIELD,
})
package dan200.computercraft.impl;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.jetbrains.annotations.ApiStatus;

/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.arguments;

import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.command.Exceptions;

import static dan200.computercraft.shared.command.text.ChatHelpers.translate;

public final class TrackingFieldArgumentType extends ChoiceArgumentType<TrackingField>
{
    private static final TrackingFieldArgumentType INSTANCE = new TrackingFieldArgumentType();

    private TrackingFieldArgumentType()
    {
        super( TrackingField.fields().values(), TrackingField::id, x -> translate( x.translationKey() ), Exceptions.TRACKING_FIELD_ARG_NONE );
    }

    public static TrackingFieldArgumentType trackingField()
    {
        return INSTANCE;
    }
}

// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.options;

import java.io.Serial;
import java.util.OptionalInt;

/**
 * Throw when a {@link AddressRule} cannot be parsed.
 *
 * @see AddressRule#parse(String, OptionalInt, PartialOptions)
 * @see AddressPredicate.HostRange#parse(String, String)
 */
public class InvalidRuleException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1303376302865132758L;

    public InvalidRuleException(String message) {
        super(message);
    }
}

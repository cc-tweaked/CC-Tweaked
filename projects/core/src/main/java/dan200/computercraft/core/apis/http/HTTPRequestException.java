// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.apis.http;

import java.io.Serial;

public class HTTPRequestException extends Exception {
    @Serial
    private static final long serialVersionUID = 7591208619422744652L;

    public HTTPRequestException(String s) {
        super(s);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}

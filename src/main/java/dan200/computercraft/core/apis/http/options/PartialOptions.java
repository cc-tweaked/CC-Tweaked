/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http.options;

import javax.annotation.Nonnull;

public final class PartialOptions
{
    static final PartialOptions DEFAULT = new PartialOptions( null, null, null, null, null );

    Action action;
    Long maxUpload;
    Long maxDownload;
    Integer timeout;
    Integer websocketMessage;

    Options options;

    PartialOptions( Action action, Long maxUpload, Long maxDownload, Integer timeout, Integer websocketMessage )
    {
        this.action = action;
        this.maxUpload = maxUpload;
        this.maxDownload = maxDownload;
        this.timeout = timeout;
        this.websocketMessage = websocketMessage;
    }

    @Nonnull
    Options toOptions()
    {
        if( options != null ) return options;

        return options = new Options(
            action == null ? Action.DENY : action,
            maxUpload == null ? AddressRule.MAX_UPLOAD : maxUpload,
            maxDownload == null ? AddressRule.MAX_DOWNLOAD : maxDownload,
            timeout == null ? AddressRule.TIMEOUT : timeout,
            websocketMessage == null ? AddressRule.WEBSOCKET_MESSAGE : websocketMessage
        );
    }

    void merge( @Nonnull PartialOptions other )
    {
        if( action == null && other.action != null ) action = other.action;
        if( maxUpload == null && other.maxUpload != null ) maxUpload = other.maxUpload;
        if( maxDownload == null && other.maxDownload != null ) maxDownload = other.maxDownload;
        if( timeout == null && other.timeout != null ) timeout = other.timeout;
        if( websocketMessage == null && other.websocketMessage != null ) websocketMessage = other.websocketMessage;
    }

    PartialOptions copy()
    {
        return new PartialOptions( action, maxUpload, maxDownload, timeout, websocketMessage );
    }
}

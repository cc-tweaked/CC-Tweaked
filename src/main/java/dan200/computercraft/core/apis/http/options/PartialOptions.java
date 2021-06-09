/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
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
        if( this.options != null )
        {
            return this.options;
        }

        return this.options = new Options( this.action == null ? Action.DENY : this.action,
            this.maxUpload == null ? AddressRule.MAX_UPLOAD : this.maxUpload,
            this.maxDownload == null ? AddressRule.MAX_DOWNLOAD : this.maxDownload,
            this.timeout == null ? AddressRule.TIMEOUT : this.timeout, this.websocketMessage == null ? AddressRule.WEBSOCKET_MESSAGE : this.websocketMessage );
    }

    void merge( @Nonnull PartialOptions other )
    {
        if( this.action == null && other.action != null )
        {
            this.action = other.action;
        }
        if( this.maxUpload == null && other.maxUpload != null )
        {
            this.maxUpload = other.maxUpload;
        }
        if( this.maxDownload == null && other.maxDownload != null )
        {
            this.maxDownload = other.maxDownload;
        }
        if( this.timeout == null && other.timeout != null )
        {
            this.timeout = other.timeout;
        }
        if( this.websocketMessage == null && other.websocketMessage != null )
        {
            this.websocketMessage = other.websocketMessage;
        }
    }

    PartialOptions copy()
    {
        return new PartialOptions( this.action, this.maxUpload, this.maxDownload, this.timeout, this.websocketMessage );
    }
}

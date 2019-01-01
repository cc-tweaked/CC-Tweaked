/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network;

public final class NetworkMessages
{
    private NetworkMessages()
    {
    }

    public static final int COMPUTER_ACTION_SERVER_MESSAGE = 0;
    public static final int QUEUE_EVENT_SERVER_MESSAGE = 1;
    public static final int REQUEST_COMPUTER_SERVER_MESSAGE = 2;

    public static final int CHAT_TABLE_CLIENT_MESSAGE = 10;
    public static final int COMPUTER_DATA_CLIENT_MESSAGE = 11;
    public static final int COMPUTER_DELETED_CLIENT_MESSAGE = 12;
    public static final int COMPUTER_TERMINAL_CLIENT_MESSAGE = 13;
    public static final int PLAY_RECORD_CLIENT_MESSAGE = 14;
}

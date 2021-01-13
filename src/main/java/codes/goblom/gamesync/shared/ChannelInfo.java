/*
 * Copyright (C) Bryan Larson - All Rights Reserved
 * 
 * Unauthorized copying of this file, via any medium is strictly prohibited Proprietary and confidential
 * 
 * Written by Bryan Larson Apr 28, 2020
 */
package codes.goblom.gamesync.shared;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 *
 * @author Bryan Larson
 */
public class ChannelInfo {
    
    private static final String CHANNEL_STUB = "gamesync:";
    
    /**
     * This is sent to Bukkit
     * 
     * Use: Data Stream
     * Usage:
     *    -- Player to TP (UUID)
     *    -- TP to this Player (UUID)
     */
    public static final String TP_CHANNEL_NAME = CHANNEL_STUB + "tp";
    
    /**
     * This is Received from Bukkit
     * 
     * Use: Data Stream
     * Usage: (player to move is the Connection Sender)
     *    -- Current Environment (NORMAL, NETHER, THE_END)
     *    -- Moving to this Environment (NORMAL, NETHER, THE_END)
     *    -- (String) {@link WorldLocation} [world:x:y:z] of Current Environment (Not Moving To Environment)
     */
    public static final String PORTAL_CHANNEL_NAME = CHANNEL_STUB + "portal";
    
    /**
     * This is sent to Bukkit
     * 
     * Note: Does not check to see if world is loaded.
     * 
     * Use: Object Stream
     * Usage:
     *    -- Player to Move (UUID)
     *    -- World Location
     */
    public static final String MOVE_LOCATION_CHANNEL_NAME = CHANNEL_STUB + "movelocation";
    
    /**
     * This is recieved from Bukkit and Forwarded to the players current server
     *    under the same tag
     * 
     * Use: Data Stream
     * Usage:
     *     -- Player UUID
     *     -- Serialized org.bukkit.inventory.PlayerInventory (Base64 Encoding)
     * 
     * @deprecated No Longer how we publish player inventory (Use: {@link ChannelInfo#PLAYER_DATA_CHANNEL_NAME})
     */
    @Deprecated
    public static final String INVENTORY_CHANNEL_NAME = CHANNEL_STUB + "inventory";
    
    /**
     * This is recieved from Bukkit and Forwarded to the players current server
     *    under the same tag
     * 
     * Use: Object Stream
     * Usage:
     *     -- {@see codes.goblom.gamesync.todo.PlayerData}
     */
    public static final String PLAYER_DATA_CHANNEL_NAME = CHANNEL_STUB + "playerdata";
    /**
     * This can be sent or recieved from Bukkit/Proxy
     * 
     * Use: Object Stream
     * Usage:
     *    -- Origin Channel Name
     *    -- Error message
     *    -- byte[] original data sent
     */
    public static final String ERROR_CHANNEL_NAME = CHANNEL_STUB + "error";
    
    /**
     * This is ALL Available Channels (Incoming/Outgoing)
     */
    public static final String[] CHANNEL_NAMES = {
        TP_CHANNEL_NAME, PORTAL_CHANNEL_NAME, MOVE_LOCATION_CHANNEL_NAME,
//        INVENTORY_CHANNEL_NAME,
        PLAYER_DATA_CHANNEL_NAME,
        ERROR_CHANNEL_NAME
    };
    
    public static byte[] buildError(String originalChannel, String errorMessage, byte[] original) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(stream);
        
        out.writeUTF(originalChannel);
        out.writeUTF(errorMessage);
        out.writeObject(original); // TODO: Find better way than ObjectStream
        
        return stream.toByteArray();
    }
}

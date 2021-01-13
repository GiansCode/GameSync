/*
 * Copyright (C) Bryan Larson - All Rights Reserved
 * 
 * Unauthorized copying of this file, via any medium is strictly prohibited Proprietary and confidential
 * 
 * Written by Bryan Larson Apr 28, 2020
 */
package codes.goblom.gamesync.proxy;

import codes.goblom.gamesync.shared.ChannelInfo;
import codes.goblom.gamesync.shared.PlayerData;
import codes.goblom.gamesync.shared.WorldLocation;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.Random;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

/**
 *
 * @author Bryan Larson
 */
@AllArgsConstructor ( access = AccessLevel.PRIVATE )
public class ProxyUtil {
    
    public static final Random RANDOM = new Random();
    
    /**
     * Teleports Player One to Player Two. Accounts for difference servers
     * 
     * @param playerOne Player to TP
     * @param playerTwo TP to this player
     * @throws IOException if there was an error building the ChannelData
     */
    public static void teleport(ProxiedPlayer playerOne, ProxiedPlayer playerTwo) throws IOException {
        if (playerOne == playerTwo) return;
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        
        out.writeUTF(playerOne.getUniqueId().toString());
        out.writeUTF(playerTwo.getUniqueId().toString());
        
        final byte[] sending = stream.toByteArray();
        
        playerTwo.getServer().sendData(ChannelInfo.TP_CHANNEL_NAME, sending);
        
        // Do it after to ensure we attempted to send data to server before connect
        if (playerOne.getServer() != playerTwo.getServer()) {
            playerOne.connect(playerTwo.getServer().getInfo());
        }
    }
    
    /**
     * Transfers the Player to the desired world on its current server
     * 
     * @param player The Player to move
     * @param loc Location to send to
     */
    public static void sendToLocation(ProxiedPlayer player, WorldLocation loc) throws IOException {
        if (!loc.isValid()) throw new RuntimeException("WorldLocation is not valid");
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(stream);
        
        out.writeUTF(player.getUniqueId().toString());
        out.writeObject(loc);
        
        final byte[] sending = stream.toByteArray();
        
        player.getServer().sendData(ChannelInfo.MOVE_LOCATION_CHANNEL_NAME, sending);
    }
    
    /**
     * @deprecated Use {@link ProxyUtil#sendData(net.md_5.bungee.api.config.ServerInfo, codes.goblom.gamesync.shared.PlayerData) }
     */
    @Deprecated
    public static void sendInventory(ServerInfo server, UUID id, String serialized) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        
        out.writeUTF(id.toString());
        out.writeUTF(serialized);
        
        final byte[] sending = stream.toByteArray();
        
        server.sendData(ChannelInfo.INVENTORY_CHANNEL_NAME, sending);
    }
    
    public static void sendData(ServerInfo server, PlayerData data) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(stream);
        
        out.writeObject(data);
        
        final byte[] sending = stream.toByteArray();
        
        server.sendData(ChannelInfo.PLAYER_DATA_CHANNEL_NAME, sending);
    }
    
    public static Configuration getConfiguration(Plugin plugin, String name) throws IOException {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        File configFile = new File(plugin.getDataFolder(), name);
        if (!configFile.exists()) {
            try (InputStream in = plugin.getResourceAsStream(name)) {
                Files.copy(in, configFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
    }
    
}

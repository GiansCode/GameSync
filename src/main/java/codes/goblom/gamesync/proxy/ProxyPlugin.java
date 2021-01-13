/*
 * Copyright (C) Bryan Larson - All Rights Reserved
 * 
 * Unauthorized copying of this file, via any medium is strictly prohibited Proprietary and confidential
 * 
 * Written by Bryan Larson Apr 28, 2020
 */
package codes.goblom.gamesync.proxy;

import codes.goblom.gamesync.proxy.commands.TPCommand;
import codes.goblom.gamesync.proxy.commands.TPRequests;
import codes.goblom.gamesync.shared.ChannelInfo;
import codes.goblom.gamesync.shared.WorldLocation;
import codes.goblom.gamesync.shared.PlayerData;
import com.google.common.collect.Maps;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

/**
 *
 * @author Bryan Larson
 */
public class ProxyPlugin extends Plugin implements Listener {

    private Configuration config;
//    private Map<UUID, String> inventories = Maps.newHashMap();
    protected Map<UUID, PlayerData> playerData = Maps.newHashMap();
    
    private RedisSync redisSync;
    
    @Override
    public void onEnable() {
        try {
            config = ProxyUtil.getConfiguration(this, "settings.yml");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        for (String channel : ChannelInfo.CHANNEL_NAMES) {
            ProxyServer.getInstance().registerChannel(channel);
        }
        
        new TPRequests(this);
        new TPCommand(this);
        
        ProxyServer.getInstance().getPluginManager().registerListener(this, this);
        
        this.redisSync = new RedisSync(this);
    }
    
    @Override
    public void onDisable() {
        this.redisSync.shutdown();
        
        for (String channel : ChannelInfo.CHANNEL_NAMES) {
            ProxyServer.getInstance().unregisterChannel(channel);
        }
    }
    
    public WorldLocation getSpawnLocation() {
        WorldLocation loc = new WorldLocation();
        
        Object spawn = config.get("Spawn");
        
        if (spawn instanceof Configuration) {
            Configuration c = (Configuration) spawn;
            
            String world = c.getString("World");
            Double x = c.getDouble("X");
            Double y = c.getDouble("Y");
            Double z = c.getDouble("Z");
            
            if (x != null && y != null && z != null) {
                loc.setX(x);
                loc.setY(y);
                loc.setZ(z);
            }
            
            if (world != null && !world.isEmpty()) {
                loc.setWorld(world);
            }
            
        } else {
            loc.setWorld(spawn.toString());
        }
        
        return loc;
    }
    
    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        PlayerData data = playerData.remove(player.getUniqueId());
        
        if (data != null) {
            try {
                ProxyUtil.sendData(event.getTarget(), data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            getLogger().info("Player data for " + player.getUniqueId() + " was null... Have they joined before?");
        }
//        String serialized = inventories.get(player.getUniqueId());
//        
//        if (serialized != null && !serialized.isEmpty()) { //Because just in case
//            try {
//                ProxyUtil.sendInventory(event.getTarget(), player.getUniqueId(), serialized);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else {
//            getLogger().warning("Inventory for " + player.getName() + " was null or empty. Have They Joined Before?");
//        }
    }
    
    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        switch (event.getTag()) {
            case ChannelInfo.PLAYER_DATA_CHANNEL_NAME: {
                try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(event.getData()))) {
                    PlayerData data = (PlayerData) in.readObject();
                    
                    playerData.put(data.getUniqueId(), data);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            }
            case ChannelInfo.INVENTORY_CHANNEL_NAME: {
                getLogger().warning("Received message on deprecated channel[" + event.getTag() + "] channel. Doing nothing...");
//                try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
//                    UUID uniqueId = UUID.fromString(in.readUTF());
//                    String serialized = in.readUTF();
//                    
//                    inventories.put(uniqueId, serialized);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                break;
            }
            case ChannelInfo.ERROR_CHANNEL_NAME: {
                try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(event.getData()))) {
                    String originalChannel = in.readUTF();
                    String errorMessage = in.readUTF();
                    byte[] originalMessage = (byte[]) in.readObject();
                    
                    getLogger().warning("Recieved Error On Channel [" + originalChannel + "] Error [" + errorMessage + "] Original Length [" + originalMessage.length + "]");
                } catch(IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            }
            case ChannelInfo.PORTAL_CHANNEL_NAME: {
                try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
                    ProxiedPlayer player = (ProxiedPlayer) event.getSender();
                    
                    String fromEnv = in.readUTF();
                    String toEnv = in.readUTF();
                    WorldLocation currentLoc = WorldLocation.fromString(in.readUTF());
                    
                    Object potentialServers = config.get("Worlds." + toEnv);
                    String randomServer = (potentialServers == null || potentialServers instanceof List) ? null : potentialServers.toString();
                    
                    if (randomServer == null) {
                        if (potentialServers == null) {
                            //Send to Spawn
                            WorldLocation spawn = getSpawnLocation();
                            
                            ProxyUtil.sendToLocation(player, spawn);
                            return;
                        }
                        List<String> servers = (List<String>) potentialServers;
                        randomServer = (servers.size() == 1 ? servers.get(0) : servers.get(ProxyUtil.RANDOM.nextInt(servers.size())));
                        
//                        if (servers.size() == 1) {
//                            randomServer = servers.get(0);
//                        } else {
//                            randomServer = servers.get(ProxyUtil.RANDOM.nextInt(servers.size()));
//                        }
                    }
                    
                    if (randomServer == null) { //If for some reason RandomServer is still null
                        //Send to Spawn
                        WorldLocation spawn = getSpawnLocation();

                        ProxyUtil.sendToLocation(player, spawn);
                        return;
                    }
                    
                    ServerInfo info = ProxyServer.getInstance().getServerInfo(randomServer);
                    if (info == null) { //If we dont know that server name
                        //Send to Spawn
                        WorldLocation spawn = getSpawnLocation();

                        ProxyUtil.sendToLocation(player, spawn);
                        return;
                    }
                    
                    ((ProxiedPlayer) event.getSender()).connect(info);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                break;
            }
        }
    }
}

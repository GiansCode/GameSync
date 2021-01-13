/*
 * Copyright (C) Bryan Larson - All Rights Reserved
 * 
 * Unauthorized copying of this file, via any medium is strictly prohibited Proprietary and confidential
 * 
 * Written by Bryan Larson Apr 28, 2020
 */
package codes.goblom.gamesync.bukkit;

import codes.goblom.gamesync.shared.ChannelInfo;
import codes.goblom.gamesync.shared.WorldLocation;
import codes.goblom.gamesync.shared.PlayerData;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 *
 * @author Bryan Larson
 */

//TODO: Handle PortalEvent for semi-accurate Portal Locations
public class BukkitPlugin extends JavaPlugin implements Listener, PluginMessageListener {
    public static BukkitPlugin instance;
    
    private static final Cache<UUID, UUID> TP_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    
    @Override
    public void onLoad() {
        instance = this;
    }
    
    @Override
    public void onEnable() {
        for (String channel : ChannelInfo.CHANNEL_NAMES) {
            Bukkit.getMessenger().registerIncomingPluginChannel(this, channel, this);
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, channel);
        }
        
        Bukkit.getPluginManager().registerEvents(this, this);
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            Map<UUID, UUID> map = TP_CACHE.asMap();
            
            map.forEach((fromId, toId) -> {
                final Player from = Bukkit.getPlayer(fromId);
                final Player to = Bukkit.getPlayer(toId);
                
                if (from == null || to == null) return;
                
                TP_CACHE.invalidate(fromId);
                
                Bukkit.getScheduler().runTask(this, () -> from.teleport(to));
            });
        }, 0, 1);
        
        PlayerSyncListener listener = new PlayerSyncListener();
        Bukkit.getPluginManager().registerEvents(listener, this);
        Bukkit.getScheduler().runTaskTimer(this, listener, 0, 5); // Post PlayerData every 1/4 of a second
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        event.setCancelled(true); //Always cancel so we don't teleport to different world by accident.
        
        try {
            BukkitUtil.portalTeleport(event.getPlayer(), event.getTo().getWorld().getEnvironment());
        } catch (Exception e) {
            event.getPlayer().sendMessage("Error: " + e.getMessage());
            
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID thisId = event.getPlayer().getUniqueId();
        
        UUID toTP = TP_CACHE.getIfPresent(thisId);
        
        if (toTP != null) {
            TP_CACHE.invalidate(thisId);
            
            Player toPlayer = Bukkit.getPlayer(toTP);
            
            if (toPlayer != null) {
                Runnable r  = () -> event.getPlayer().teleport(toPlayer, PlayerTeleportEvent.TeleportCause.PLUGIN);
                
                Bukkit.getScheduler().runTaskLater(this, r, 1);
            } else {
                event.getPlayer().sendMessage("You were scheduled for a TP. But we couldn't find the player... (?)"); //Did they change server?
            }
        }
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player receiver, byte[] message) {
        switch (channel) {
            case ChannelInfo.PLAYER_DATA_CHANNEL_NAME: {
                try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(message))) {
                    PlayerData data = (PlayerData) in.readObject();
                    
                    PlayerSyncListener.DATA_WAITING.put(data.getUniqueId(), data);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                
                break;
            }
            
            case ChannelInfo.INVENTORY_CHANNEL_NAME: {
                getLogger().warning("Received message on deprecated channel[" + channel + "] channel. Doing nothing...");
                
//                try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
//                    UUID uniqueId = UUID.fromString(in.readUTF());
//                    String serialized = in.readUTF();
//                    
//                    PlayerSyncListener.INV_AWAITING.put(uniqueId, serialized);
//                } catch (IOException e) { 
//                    e.printStackTrace();
//                }
                
                break;
            }
            case ChannelInfo.MOVE_LOCATION_CHANNEL_NAME: {
                try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(message))) {
                    UUID playerId = UUID.fromString(in.readUTF());
                    WorldLocation wloc = (WorldLocation) in.readObject();
                    
                    Player player = Bukkit.getPlayer(playerId);
                    
                    if (player == null) {
                        byte[] error = ChannelInfo.buildError(ChannelInfo.MOVE_LOCATION_CHANNEL_NAME, "Player not found", message);
                        receiver.sendPluginMessage(this, ChannelInfo.ERROR_CHANNEL_NAME, error);
                    }
                    
                    if (wloc.getWorld() == null || wloc.getWorld().isEmpty()) {
                        Location loc = new Location(player.getWorld(), wloc.getX(), wloc.getY(), wloc.getZ());
                        
                        player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    } else {
                        if (wloc.getX() == null || wloc.getY() == null || wloc.getZ() == null) {
                            World world = Bukkit.getWorld(wloc.getWorld());
                            
                            player.teleport(world.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                        } else {
                            World world = Bukkit.getWorld(wloc.getWorld());
                            double x = wloc.getX();
                            double z = wloc.getZ();
                            double y = wloc.getY();
                            
                            Location loc = new Location(world, x, y, z);
                            
                            player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                
                break;
            }
            case ChannelInfo.TP_CHANNEL_NAME: {
                try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
                    String playerOneId = in.readUTF();
                    String playerTwoId = in.readUTF();
                    
                    UUID playerOne = UUID.fromString(playerOneId);
                    UUID playerTwo = UUID.fromString(playerTwoId);
                    
                    TP_CACHE.put(playerOne, playerTwo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                break;
            }
            
            default:
                if (channel.startsWith("minecraft:") || channel.startsWith("mc:")) return;
                
                getLogger().info("Recieved tag with length[" + message.length + "] on channel[" + channel + "]");
        }
    }
}

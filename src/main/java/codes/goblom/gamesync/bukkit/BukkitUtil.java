/*
 * Copyright (C) Bryan Larson - All Rights Reserved
 * 
 * Unauthorized copying of this file, via any medium is strictly prohibited Proprietary and confidential
 * 
 * Written by Bryan Larson Apr 28, 2020
 */
package codes.goblom.gamesync.bukkit;

import codes.goblom.gamesync.shared.ChannelInfo;
import codes.goblom.gamesync.shared.PlayerData;
import codes.goblom.gamesync.shared.WorldLocation;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;

/**
 *
 * @author Bryan Larson
 */
public class BukkitUtil {
    
    public static void portalTeleport(Player player, Environment toEnv) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        
        out.writeUTF(player.getWorld().getEnvironment().name());
        out.writeUTF(toEnv.name());
        out.writeUTF(compatLocation(player.getLocation()).toString());
        
        final byte[] data = stream.toByteArray();
        
        player.sendPluginMessage(BukkitPlugin.instance, ChannelInfo.PORTAL_CHANNEL_NAME, data);
    }
    
    /**
     * @see codes.goblom.gamesync.shared.ChannelInfo#INVENTORY_CHANNEL_NAME
     * 
     * @deprecated Use {@link BukkitUtil#postData(org.bukkit.entity.Player)}
     */
    @Deprecated
    public static void postInventory(Player player) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        
        String serialized = ItemStackParser.serialize(player.getInventory());
        
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(serialized);
        
        final byte[] data = stream.toByteArray();
        
        player.sendPluginMessage(BukkitPlugin.instance, ChannelInfo.INVENTORY_CHANNEL_NAME, data);
    }
    
    public static void postData(Player player) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(stream);
        
        out.writeObject(createData(player));
        
        final byte[] data = stream.toByteArray();
        
        player.sendPluginMessage(BukkitPlugin.instance, ChannelInfo.PLAYER_DATA_CHANNEL_NAME, data);
    }
    
    public static PlayerData createData(Player player) {
        PlayerData data = new PlayerData(player.getUniqueId());
        
        data.setPlayerName(player.getName());
        data.setLastSeen(player.getLastSeen());
        
        data.setGameMode(player.getGameMode().name());
        
        data.setHealthCurrent(player.getHealth());
        data.setHealthScale(player.getHealthScale());
        
        data.setFoodLevel(player.getFoodLevel());
        data.setSaturation(player.getSaturation());
        data.setExhaustion(player.getExhaustion());
        
        data.setAirCurrent(player.getRemainingAir());
        data.setAirMax(player.getMaximumAir());
        
        data.setEncodedInventory(ItemStackParser.serialize(player.getInventory()));
        
        return data;
    }
    
    public static boolean updatePlayer(PlayerData data) {
        Player player = Bukkit.getPlayer(data.getUniqueId());
        if (player == null) {
            player = Bukkit.getPlayer(data.getPlayerName());
        }
        
        if (player == null) { //Player Still null??? Does not exist on current Bukkit Server
            return false;
        }
        
        player.setGameMode(GameMode.valueOf(data.getGameMode()));
        
        player.setHealthScale(data.getHealthScale());
        player.setHealth(data.getHealthCurrent());
        
        player.setFoodLevel(data.getFoodLevel());
        player.setSaturation(data.getSaturation());
        player.setExhaustion(data.getExhaustion());
        
        player.setMaximumAir(data.getAirMax());
        player.setRemainingAir(data.getAirCurrent());
        
        try {
            ItemStackParser.setInventory(player, data.getEncodedInventory());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    public static WorldLocation compatLocation(Location loc) {
        WorldLocation wloc = new WorldLocation();
        
        wloc.setWorld(loc.getWorld().getName());
        wloc.setX(loc.getX());
        wloc.setY(loc.getY());
        wloc.setZ(loc.getZ());
        
        return wloc;
    }
}

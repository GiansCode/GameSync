/*
 * Copyright (C) Bryan Larson - All Rights Reserved
 * 
 * Unauthorized copying of this file, via any medium is strictly prohibited Proprietary and confidential
 * 
 * Written by Bryan Larson Apr 30, 2020
 */
package codes.goblom.gamesync.bukkit;

import codes.goblom.gamesync.shared.PlayerData;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author Bryan Larson
 */

//TODO: Handle Death Event.. Send to player bed location
public class PlayerSyncListener implements Listener, Runnable {
//    protected static Map<UUID, String> INV_AWAITING = Maps.newHashMap();
    protected static Map<UUID, PlayerData> DATA_WAITING = Maps.newHashMap();
    
    private final Cache<UUID, Long> justJoined = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.SECONDS)
            .build();
    
    @Override
    public void run() { //Ensures the Proxy always has the latest inventory.
        Bukkit.getOnlinePlayers().forEach((player) -> {
            if (justJoined.getIfPresent(player.getUniqueId()) != null) return;
            
            try {
//                BukkitUtil.postInventory(player);
                BukkitUtil.postData(player);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        try {
//            BukkitUtil.postInventory(event.getPlayer());
            BukkitUtil.postData(event.getPlayer());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws CommandSyntaxException { //Can we throw an Exception here?
        UUID id = event.getPlayer().getUniqueId();
        justJoined.put(id, System.currentTimeMillis());
        
//        if (INV_AWAITING.containsKey(id)) {
//            event.getPlayer().getInventory().clear(); //Prevent duping - unsynced items
//            
//            String serialInventory = INV_AWAITING.get(id);
//            
//            ItemStackParser.setInventory(event.getPlayer(), serialInventory);
//            
//            INV_AWAITING.remove(id);
//        }

        if (DATA_WAITING.containsKey(id)) {
            PlayerData data = DATA_WAITING.remove(id);
            
            if (!BukkitUtil.updatePlayer(data)) {
                event.getPlayer().sendMessage("Your data was somehow corrupted... Please message an Admin.");
            }
        }
    }
}

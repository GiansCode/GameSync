/*
 * Copyright (C) Bryan Larson - All Rights Reserved
 * 
 * Unauthorized copying of this file, via any medium is strictly prohibited Proprietary and confidential
 * 
 * Written by Bryan Larson May 1, 2020
 */
package codes.goblom.gamesync.proxy;

import codes.goblom.gamesync.shared.ChannelInfo;
import codes.goblom.gamesync.shared.PlayerData;
import com.google.common.collect.Lists;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.bukkit.event.EventHandler;

/**
 *
 * @author Bryan Larson
 */
public class RedisSync implements Runnable, Listener {
    
    private final ProxyPlugin plugin;
    private final RedisBungeeAPI redis;
    private final ScheduledTask thisTask;
    
    protected RedisSync(ProxyPlugin plugin) {
        this.plugin = plugin;
        this.redis = RedisBungee.getApi();
        
        this.redis.registerPubSubChannels(ChannelInfo.PLAYER_DATA_CHANNEL_NAME);
        ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);
        this.thisTask = ProxyServer.getInstance().getScheduler().schedule(plugin, this, 1, 20 / 5, TimeUnit.MILLISECONDS); //Every 1/4 second
    }
    
    public void shutdown() {
        if (thisTask != null) {
            thisTask.cancel();
        }
        
        this.redis.unregisterPubSubChannels(ChannelInfo.PLAYER_DATA_CHANNEL_NAME);
    }
    
    @Override
    public void run() {
        List<PlayerData> clone = Lists.newArrayList(plugin.playerData.values());
        
        clone.forEach((data) -> {
            redis.sendChannelMessage(ChannelInfo.PLAYER_DATA_CHANNEL_NAME, data.toString());
        });
    }
    
    @EventHandler
    public void onMessageEvent(PubSubMessageEvent event) {
        if (ChannelInfo.PLAYER_DATA_CHANNEL_NAME.equals(event.getChannel())) {
            String serializedData = event.getMessage();
            
            PlayerData data = PlayerData.fromString(serializedData);
            
            plugin.playerData.put(data.getUniqueId(), data);
        }
    }
}

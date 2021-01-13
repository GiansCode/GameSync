/*
 * Copyright (C) Bryan Larson - All Rights Reserved
 * 
 * Unauthorized copying of this file, via any medium is strictly prohibited Proprietary and confidential
 * 
 * Written by Bryan Larson Apr 28, 2020
 */
package codes.goblom.gamesync.proxy.commands;

import codes.goblom.gamesync.proxy.ProxyPlugin;
import codes.goblom.gamesync.proxy.ProxyUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
 *
 * @author Bryan Larson
 */
public class TPRequests {
    
    /// <Requested, <Asker, Time Asked>>
    protected static Map<String, Cache<String, Long>> WAITING = Maps.newConcurrentMap();
            
    public TPRequests(ProxyPlugin plugin) {
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new TPAsk());
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new TPAccept());
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new TPDeny());
    }
    
    // Usage:
    //   /tpask [player]
    private class TPAsk extends Command {

        public TPAsk() {
            super("tpaskt", "gamesync.tpask", "tpa");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage("TP Ask can only be ran as a player");
                return;
            }
            
            if (args.length == 1) {
                String playerName = args[0];
                ProxiedPlayer askPlayer = ProxyServer.getInstance().getPlayer(playerName);
                
                if (askPlayer == null) {
                    sender.sendMessage(playerName + " is not online");
                } else {
                    Cache<String, Long> askers = WAITING.get(askPlayer.getName());
                    if (askers == null) {
                        askers = CacheBuilder.newBuilder()
                                .expireAfterWrite(1, TimeUnit.MINUTES)
                                .build();
                        
                        WAITING.put(askPlayer.getName(), askers);
                    }
                    
                    askers.put(sender.getName(), System.currentTimeMillis());
                    
                    askPlayer.sendMessage(sender.getName() + " has requested to TP to you. type /tpaccept or /tpdeny");
                }
            } else {
                sender.sendMessage("Usage: /tpa [player]");
            }
        }
    }
    
    //Usage:
    //   /tpaccept -> Accepts tp request if only one player otherwise prints all requested
    //   /tpaccept [player] -> accepts only [player] tpask
    private class TPAccept extends Command {

        public TPAccept() {
            super("tpaccept", "gamesync.tpaccept", "tpyes");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage("TP Accept can only be ran as a player");
                return;
            }
            
            Cache<String, Long> askers = WAITING.get(sender.getName());
            
            if (askers == null || askers.size() == 0) {
                sender.sendMessage("You have no requests waiting.");
                
                return;
            }
            
            if (args.length == 0) {
                if (askers.size() == 1) {
                    String askerName = askers.asMap().keySet().stream().findFirst().orElseGet(() -> ""); //Return first element of Cache ??? Easiest Way ?????
                    
                    if (askerName.isEmpty()) {
                        sender.sendMessage("Somehow there was an error and there is no one to TP Accept. (?)");
                    } else {
                        ProxiedPlayer asker = ProxyServer.getInstance().getPlayer(askerName);
                        askers.invalidate(askerName);
                        
                        if (asker == null) {
                            sender.sendMessage(askerName + " is no longer online");
                            
                            return;
                        }
                        
                        try {
                            ProxyUtil.teleport(asker, (ProxiedPlayer) sender);
                            
                            asker.sendMessage(sender.getName() + " has accepted your tp ask");
                        } catch (IOException e) {
                            sender.sendMessage("Error: " + e.getMessage());

                            e.printStackTrace();
                        }
                    }
                } else {
                    StringBuilder sb = new StringBuilder("Askers(").append(askers.size()).append("): ");
                    
                    askers.asMap().forEach((name, time) -> {
                        sb.append(name).append("[").append((System.currentTimeMillis() - time) / 20).append("s], ");
                    });
                    
                    String incantation = sb.toString();
                           incantation = incantation.substring(0, incantation.length() - 2);
                           
                    sender.sendMessage(incantation);
                    sender.sendMessage("Use '/tpaccept [player]' to accept a specific player or '/tpaccept all' to accept everyone");
                }
            } else if (args.length == 1) {
                String playerName = args[0];
                
                if (playerName.isEmpty()) {
                    sender.sendMessage("Playername is empty.");
                } else {
                    switch (playerName.toLowerCase()) {
                        case "all":
                        case "everyone": {
                            for (String name : askers.asMap().keySet()) {
                                ProxiedPlayer player = ProxyServer.getInstance().getPlayer(name);
                                if (player == null) continue;
                                
                                try {
                                    ProxyUtil.teleport(player, (ProxiedPlayer) sender);
                                } catch (IOException e) {
                                    e.printStackTrace();;
                                }
                            }
                            
                            askers.invalidateAll();
                            break;
                        }
                        default:
                            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
                            askers.invalidate(playerName);
                            if (player == null) {
                                sender.sendMessage(playerName + " is no longer online.");
                                return;
                            }
                            
                            Long time = askers.getIfPresent(playerName);
                            
                            if (time == null) {
                                sender.sendMessage(playerName + " is not requesting to teleport to you.");
                            } else {
                                try {
                                    ProxyUtil.teleport(player, (ProxiedPlayer) sender);

                                    player.sendMessage(sender.getName() + " has accepted your tp ask");
                                } catch (IOException e) {
                                    sender.sendMessage("Error: " + e.getMessage());

                                    e.printStackTrace();
                                }
                            }
                    }
                }
            }
        }
    }
    
    //Usage:
    //   /tpdeny -> denies tp request if only one player otherwise prints all requested
    //   /tpdeny [player] -> denies only [player] tpask
    private class TPDeny extends Command {

        public TPDeny() {
            super("tpdeny", "gamesync.tpdeny", "tpno");
        }
        
        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage("TP Deny can only be ran as a player");
                return;
            }
            
            Cache<String, Long> askers = WAITING.get(sender.getName());
            
            if (askers == null || askers.size() == 0) {
                sender.sendMessage("You have no requests waiting.");
                
                return;
            }
            
            if (args.length == 0) {
                if (askers.size() == 1) {
                    String askerName = askers.asMap().keySet().stream().findFirst().orElseGet(() -> ""); //Return first element of Cache ??? Easiest Way ?????
                    
                    if (askerName.isEmpty()) {
                        sender.sendMessage("Somehow there was an error and there is no one to TP Accept. (?)");
                    } else {
                        askers.invalidate(askerName);
                        sender.sendMessage("You have denied " + askerName);
                    }
                } else {
                    StringBuilder sb = new StringBuilder("Askers(").append(askers.size()).append("): ");
                    
                    askers.asMap().forEach((name, time) -> {
                        sb.append(name).append("[").append((System.currentTimeMillis() - time) / 20).append("s], ");
                    });
                    
                    String incantation = sb.toString();
                           incantation = incantation.substring(0, incantation.length() - 2);
                           
                    sender.sendMessage(incantation);
                    sender.sendMessage("Use '/tpdeny [player]' to deny a specific player or '/tpdeny all' to deny everyone");
                }
            } else if (args.length == 1) {
                String playerName = args[0];
                
                if (playerName.isEmpty()) {
                    sender.sendMessage("Playername is empty.");
                } else {
                    switch (playerName.toLowerCase()) {
                        case "all":
                        case "everyone": {
                            askers.invalidateAll();
                            sender.sendMessage("You denied everyone.");
                            break;
                        }
                        default:
                            askers.invalidate(playerName);
                            sender.sendMessage("You have denied " + playerName);
                    }
                }
            }
        }
        
    }
}

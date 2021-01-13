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
import java.io.IOException;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
 *
 * @author Bryan Larson
 */
public class TPCommand extends Command {

    public TPCommand(ProxyPlugin plugin) {
        super("tp", "gamesync.tp");
        
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, this);
    }

    @Override
    public void execute(CommandSender cs, String[] args) {
        if (args.length == 1) {
            if (!(cs instanceof ProxiedPlayer)) {
                cs.sendMessage("TP can only be used by a player. Try /tp [player] [player]");
                return;
            }
            
            String toPlayerName = args[0];
            
            if (toPlayerName.isEmpty()) {
                cs.sendMessage("Please specify a player");
                return;
            }
            
            ProxiedPlayer thisPlayer = (ProxiedPlayer) cs;
            ProxiedPlayer toPlayer = ProxyServer.getInstance().getPlayer(toPlayerName);
            
            if (toPlayer == null) {
                cs.sendMessage(toPlayerName + " is offline");
            } else {
                try {
                    ProxyUtil.teleport(thisPlayer, toPlayer);
                } catch (IOException e) {
                    cs.sendMessage("Error: " + e.getMessage());
                    
                    e.printStackTrace();
                }
            }
        } else if (args.length == 2) {
            String playerNameOne = args[0];
            String playerNameTwo = args[1];
            
            ProxiedPlayer playerOne = ProxyServer.getInstance().getPlayer(playerNameOne);
            ProxiedPlayer playerTwo = ProxyServer.getInstance().getPlayer(playerNameTwo);
            
            if (playerOne == null || playerTwo == null) {
                cs.sendMessage((playerOne == null ? playerNameOne : playerNameTwo) + " is not online."); 
                return; 
            }
            
            try {
                ProxyUtil.teleport(playerOne, playerTwo);
            } catch (IOException e) {
                cs.sendMessage("Error: " + e.getMessage());

                e.printStackTrace();
            }
        }
    }
}

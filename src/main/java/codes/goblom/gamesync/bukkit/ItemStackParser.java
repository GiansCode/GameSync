/*
 * Copyright (C) Bryan Larson - All Rights Reserved
 * 
 * Unauthorized copying of this file, via any medium is strictly prohibited Proprietary and confidential
 * 
 * Written by Bryan Larson Apr 29, 2020
 */
package codes.goblom.gamesync.bukkit;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Base64;
import net.minecraft.server.v1_15_R1.ItemStack;
import net.minecraft.server.v1_15_R1.MojangsonParser;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;

/**
 *
 * @author Bryan Larson
 */
public class ItemStackParser {
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final String SEPARATOR = "|";
    private static final String SPLITTER = ":";
    
    private static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (Exception e) { }
        
        return false;
    }
    
    public static void setInventory(Player player, String cerial) throws CommandSyntaxException {
        String decoded = new String(DECODER.decode(cerial));
        String[] separator = decoded.split(SEPARATOR);
        
        PlayerInventory inv = player.getInventory();
                        player.closeInventory();
                        inv.clear(); //Dupe Protection -- In case we don't override everything
                        
        for (String separated : separator) {
            String[] split = separated.split(SPLITTER);
            String token = split[0];
            String data = split[1];
            
            org.bukkit.inventory.ItemStack item = deserialize(data);
            
            if (isInteger(token)) {
                int slot = Integer.parseInt(token);
                
                inv.setItem(slot, item);
            } else if (token.equals("MAIN_HAND")) {
                inv.setItemInMainHand(item);
            } else {
                EquipmentSlot slot = EquipmentSlot.valueOf(token);
                inv.setItem(slot, item);
            }
        }
        
        player.updateInventory();
    }
    
    public static String serialize(PlayerInventory inv) {
        StringBuilder sb = new StringBuilder();
        
        if (inv.getItemInMainHand() != null) {
            sb.append("MAIN_HAND");
            sb.append(SPLITTER);
            sb.append(serialize(inv.getItemInMainHand()));
            sb.append(SEPARATOR);
        }
        
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            org.bukkit.inventory.ItemStack stack = inv.getItem(slot);
            
            if (stack == null) continue;
            
            sb.append(slot);
            sb.append(SPLITTER);
            sb.append(serialize(stack));
            sb.append(SEPARATOR);
        }
        
        org.bukkit.inventory.ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            org.bukkit.inventory.ItemStack stack = contents[i];
            
            if (stack == null) continue;
            
            sb.append(i);
            sb.append(SPLITTER);
            sb.append(serialize(stack));
            sb.append(SEPARATOR);
        }
        
        String parsed = sb.toString();
               parsed = parsed.substring(0, parsed.length() - 1);
        System.out.println(parsed);
        return ENCODER.encodeToString(parsed.getBytes());
    }
    
    public static String serialize(org.bukkit.inventory.ItemStack stack) {
        ItemStack nmsCopy = CraftItemStack.asNMSCopy(stack);
        NBTTagCompound nbt = nmsCopy.getOrCreateTag();
        
        return ENCODER.encodeToString(nbt.toString().getBytes());
    }
    
    public static org.bukkit.inventory.ItemStack deserialize(String str) throws CommandSyntaxException {
        NBTTagCompound tag = MojangsonParser.parse(new String(DECODER.decode(str)));
        ItemStack nmsStack = ItemStack.a(tag);
        
        return CraftItemStack.asBukkitCopy(nmsStack);
    }
}

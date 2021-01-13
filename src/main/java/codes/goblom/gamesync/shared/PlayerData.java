/*
 * Copyright (C) Bryan Larson - All Rights Reserved
 * 
 * Unauthorized copying of this file, via any medium is strictly prohibited Proprietary and confidential
 * 
 * Written by Bryan Larson Apr 28, 2020
 */
package codes.goblom.gamesync.shared;

import com.google.gson.Gson;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Bryan Larson
 */
@RequiredArgsConstructor
@Data

//TODO: PotionEffects
//TODO: EnderChests
//TODO: Experience (player level, experience, experience scale, total experience)
public class PlayerData implements Serializable {
    private static final Gson GSON = new Gson();
    
    private final UUID uniqueId;
    
    private String playerName;
    private long lastSeen;
    
    private String gameMode;
    
    private double healthCurrent;
    private double healthScale;
    
//    private double mealthMax;
    
    private int foodLevel;
    private float saturation;
    private float exhaustion;
    
    private int airCurrent;
    private int airMax;
    
    private String encodedInventory;
    
    @Override
    public String toString() {
        return GSON.toJson(this);
    }
    
    public static PlayerData fromString(String gson) {
        return GSON.fromJson(gson, PlayerData.class);
    }
}

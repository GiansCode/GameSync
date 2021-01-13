/*
 * Copyright (C) Bryan Larson - All Rights Reserved
 * 
 * Unauthorized copying of this file, via any medium is strictly prohibited Proprietary and confidential
 * 
 * Written by Bryan Larson Apr 29, 2020
 */
package codes.goblom.gamesync.shared;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author Bryan Larson
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorldLocation implements Serializable {
    
    /**
     * If world is null uses Players current world
     * 
     * If world is not null moves to world
     */
    private String world = null;
    
    /**
     * Cannot be null if world is null
     * 
     * Can be null if world is not null
     */
    private Double x = null;
    
    /**
     * Cannot be null if world is null
     * 
     * Can be null if world is not null
     */
    private Double y = null;
    
    /**
     * Cannot be null if world is null
     * 
     * Can be null if world is not null
     */
    private Double z = null;
    
    WorldLocation world(String world) {
        this.world = world;
        return this;
    }
    
    WorldLocation x(double x) {
        this.x = x;
        return this;
    }
    
    WorldLocation y(double y) {
        this.y = y;
        return this;
    }
    
    WorldLocation z(double z) {
        this.z = z;
        return this;
    }
    
    public boolean isValid() {
        if (world == null) {
            return x != null && y != null && z != null;
        }
        
        return true;
    }
    
    public static WorldLocation fromString(String str) {
        String[] split = str.split(":");
        
        if (split.length == 4) {
            String world = split[0];
            double x = Double.parseDouble(split[1]);
            double y = Double.parseDouble(split[2]);
            double z = Double.parseDouble(split[3]);
            
            return new WorldLocation(world, x, y, z);
        } else {
            double x = Double.parseDouble(split[0]);
            double y = Double.parseDouble(split[1]);
            double z = Double.parseDouble(split[2]);
            
            return new WorldLocation().x(x).y(y).z(z); //Looks funny
        }
    }
    
    @Override
    public String toString() {
        if (!isValid()) throw new RuntimeException("WorldLocation is not valid");
        
        StringBuilder builder = new StringBuilder();
        
        if (world != null && !world.isEmpty()) {
            builder.append(world).append(":");
        }
        
        builder.append(x).append(":");
        builder.append(y).append(":");
        builder.append(z);
        
        return builder.toString();
    }
}

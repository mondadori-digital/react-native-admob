package com.sbugert.rnadmob;

import java.util.HashMap;

public class RNAdConfig {
    
    private HashMap GAM2Criteo;
    public HashMap getGAM2Criteo() {return GAM2Criteo;}
    public void setGAM2Criteo(HashMap GAM2Criteo) {this.GAM2Criteo = GAM2Criteo;}

    private static final RNAdConfig config = new RNAdConfig();
    public static RNAdConfig getInstance() {return config;}
}
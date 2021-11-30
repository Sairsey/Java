package com.java_polytech.pipeline;

import com.java_polytech.config_support.IGrammar;

public class RLEConfig implements IGrammar {
    public enum ConfigFields {
        MAX_AMOUNT_TO_COMPRESS("MAX_AMOUNT_TO_COMPRESS"),
        MIN_AMOUNT_TO_COMPRESS("MIN_AMOUNT_TO_COMPRESS"),
        MAX_UNCOMPRESSED_BLOCK_SIZE("MAX_UNCOMPRESSED_BLOCK_SIZE"),
        OUT_BUFFER_SIZE("OUT_BUFFER_SIZE"),
        MODE("MODE");


        private String configString;
        ConfigFields(String configString){
            this.configString = configString;
        }
        public String asString(){ return configString;}
    }
    final static private String MAP_SEPARATING_STRING = " = ";
    final static private String ARRAY_SEPARATING_STRING = ", ";

    public String getMappingSeparatingString(){
        return MAP_SEPARATING_STRING;
    }
    public String getArraySeparatingString(){
        return ARRAY_SEPARATING_STRING;
    }

    public boolean isGrammarKey(String s) {
        for (ConfigFields prefix : ConfigFields.values())
            if (s.equalsIgnoreCase(prefix.asString()))
                return true;
        return false;
    }

    public int numberOfElements() {
        return ConfigFields.values().length;
    }
}
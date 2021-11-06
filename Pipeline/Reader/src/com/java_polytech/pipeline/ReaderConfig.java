package com.java_polytech.pipeline;

import com.java_polytech.config_support.IGrammar;

public class ReaderConfig implements IGrammar {
    public enum ConfigFields {
        BUFFER_SIZE("BUFFER_SIZE");

        private String configString;
        ConfigFields(String configString){
            this.configString = configString;
        }
        public String asString(){ return configString;}
    }

    final static private String SEPARATING_STRING = " = ";

    public String getSeparatingString() {
        return SEPARATING_STRING;
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
package com.java_polytech.config_support;

public interface IGrammar {
    String getMappingSeparatingString();
    String getArraySeparatingString();
    boolean isGrammarKey(String key);
    int numberOfElements();
}

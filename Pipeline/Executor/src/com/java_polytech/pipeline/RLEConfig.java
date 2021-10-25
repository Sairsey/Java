
        package com.java_polytech.pipeline;

import com.java_polytech.pipeline_interfaces.RC;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public class RLEConfig {
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

    final static private String SEPARATING_STRING = " = ";
    private HashMap<String, String> LOADED_DATA;
    public boolean correctConfig;

    public RC process(String filename) {
        // default variables
        HashMap<String, String> defaultHashMap = new HashMap<String, String>();
        this.correctConfig = true;

        // load config
        File file = new File(filename);
        String line = null;
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        }
        catch (FileNotFoundException ex)
        {
            this.correctConfig = false;
            return RC.RC_EXECUTOR_CONFIG_FILE_ERROR;
        }

        // Read file
        while (this.correctConfig && scanner.hasNext()) {
            line = scanner.nextLine();

            String[] pair = line.split(SEPARATING_STRING);
            if (pair.length != 2) {
                this.correctConfig = false;
                return RC.RC_EXECUTOR_CONFIG_GRAMMAR_ERROR;
            }

            boolean is_line_correct = false;

            for (ConfigFields prefix : ConfigFields.values()) {
                if (pair[0].equalsIgnoreCase(prefix.asString())) {
                    if (defaultHashMap.containsKey(prefix.asString())) {

                        this.correctConfig = false;
                        return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "In config file, prefix " + prefix.asString() + " met two times.");
                    }
                    defaultHashMap.put(prefix.asString(), pair[1]);
                    is_line_correct = true;
                    break;
                }
            }
            if (!is_line_correct) {
                this.correctConfig = false;
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "In config file, unknown prefix " + pair[0]);
            }

            if (!this.correctConfig)
                break;
        }

        if (defaultHashMap.size() != ConfigFields.values().length) {
            this.correctConfig = false;
            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "In config file, Not enough fields");
        }

        LOADED_DATA = defaultHashMap;
        scanner.close();
        return RC.RC_SUCCESS;
    }

    public String GetField(ConfigFields field) {
        return LOADED_DATA.get(field.asString());
    }
}
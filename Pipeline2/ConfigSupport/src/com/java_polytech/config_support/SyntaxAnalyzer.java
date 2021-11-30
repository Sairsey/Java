package com.java_polytech.config_support;

import com.java_polytech.pipeline_interfaces.RC;
import javafx.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class SyntaxAnalyzer {
    RC.RCWho who = RC.RCWho.UNKNOWN;
    IGrammar myGrammar;

    public SyntaxAnalyzer(RC.RCWho owner, IGrammar grammar)
    {
        who = owner;
        myGrammar = grammar;
    }

    private HashMap<String, ArrayList<String>> LOADED_DATA;
    public boolean correctConfig;

    public RC process(String filename) {
        // default variables
        HashMap<String, ArrayList<String>> defaultHashMap = new HashMap<String, ArrayList<String>>();
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
            return new RC(who, RC.RCType.CODE_CONFIG_FILE_ERROR, "Couldn't open config file.");
        }

        // Read file
        while (this.correctConfig && scanner.hasNext()) {
            line = scanner.nextLine();

            String[] pair = line.split(myGrammar.getMappingSeparatingString());
            if (pair.length != 2) {
                this.correctConfig = false;
                return new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "Some grammar error in config file.");
            }

            if (myGrammar.isGrammarKey(pair[0]))
            {
                String[] argsArray = pair[1].split(myGrammar.getArraySeparatingString());
                if (argsArray.length == 0) {
                    this.correctConfig = false;
                    return new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "In config file, prefix " + pair[0] + " has no values");
                }
                ArrayList<String> array = new ArrayList<String>(Arrays.asList(argsArray));
                defaultHashMap.put(pair[0], array);
            }
            else
            {
                this.correctConfig = false;
                return new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "In config file, prefix " + pair[0] + " met two times.");
            }

            if (!this.correctConfig)
                break;
        }

        if (defaultHashMap.size() != myGrammar.numberOfElements()) {
            this.correctConfig = false;
            return new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "In config file, Not enough fields");
        }

        LOADED_DATA = defaultHashMap;
        scanner.close();
        return RC.RC_SUCCESS;
    }

    public Pair<RC, ArrayList<String>> GetField(String field) {
        if (myGrammar.isGrammarKey(field))
            return new Pair(RC.RC_SUCCESS, LOADED_DATA.get(field));
        else
            return new Pair(
                    new RC(who, RC.RCType.CODE_CUSTOM_ERROR, "Unknown field asked from grammar"),
                    LOADED_DATA.get(field));
    }
}

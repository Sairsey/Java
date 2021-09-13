import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public class Config {

    public enum ConfigFields
    {
        IN_FILENAME("IN_FILENAME"),
        OUT_FILENAME("OUT_FILENAME"),
        BUFFER_SIZE("BUFFER_SIZE"),
        MODE("MODE");

        private String configString;
        ConfigFields(String configString){
            this.configString = configString;
        }
        public String asString(){ return configString;}
    }

    enum Mode {
        UNKNOWN("UNKNOWN"),
        COMPRESS("COMPRESS"),
        DECOMPRESS("DECOMPRESS");

        private String configString;
        Mode(String configString){
            this.configString = configString;
        }
        public String asString(){ return configString;}

        static Mode ToEnum(String str) {
            if (str.equalsIgnoreCase(COMPRESS.asString()))
                return COMPRESS;
            else if (str.equalsIgnoreCase(DECOMPRESS.asString()))
                return DECOMPRESS;
            else
                return UNKNOWN;
        }
    }

    final private String SEPARATING_STRING = " = ";
    final private HashMap<String, String> LOADED_DATA;
    public boolean correct_config;

    public Config(String filename) {
        // default variables
        HashMap<String, String> defaultHashMap = new HashMap<String, String>();
        this.correct_config = true;
        String inName = "undefined";
        String outName = "undefined";
        int bufferSize = 0;
        Mode mode = null;

        // load config
        File file = new File(filename);
        String line = null;
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        }
        catch (FileNotFoundException ex)
        {
            System.out.println("Cannot open file");
            this.correct_config = false;
        }

        // Read file
        while (this.correct_config && scanner.hasNext()) {
            line = scanner.nextLine();

            String[] pair = line.split(SEPARATING_STRING);
            if (pair.length != 2) {
                System.out.println("Incorrect line (double =)");
                this.correct_config = false;
                break;
            }

            boolean is_line_correct = false;

            for (ConfigFields prefix : ConfigFields.values()) {
                if (pair[0].equalsIgnoreCase(prefix.asString())) {
                    if (defaultHashMap.containsKey(prefix.asString())) {
                        System.out.println("ERROR: prefix " + prefix.asString() + " meeted two times.");
                        this.correct_config = false;
                        break;
                    }
                    defaultHashMap.put(prefix.asString(), pair[1]);
                    is_line_correct = true;
                    break;
                }
            }
            if (!is_line_correct) {
                System.out.println("ERROR: unknown prefix " + pair[0]);
                this.correct_config = false;
                break;
            }

            if (!this.correct_config)
                break;
        }

        if (defaultHashMap.size() != ConfigFields.values().length) {
            System.out.println("ERROR: Not enough fields");
            this.correct_config = false;
        }

        LOADED_DATA = defaultHashMap;
        scanner.close();
    }

    public String GetField(ConfigFields field)
    {
        return LOADED_DATA.get(field.asString());
    }
}

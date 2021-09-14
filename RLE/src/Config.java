import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public class Config {

    public enum ConfigFields {
        IN_FILENAME("IN_FILENAME"),
        OUT_FILENAME("OUT_FILENAME"),
        IN_BUFFER_SIZE("IN_BUFFER_SIZE"),
        OUT_BUFFER_SIZE("OUT_BUFFER_SIZE"),
        MAX_AMOUNT_TO_COMPRESS("MAX_AMOUNT_TO_COMPRESS"),
        MIN_AMOUNT_TO_COMPRESS("MIN_AMOUNT_TO_COMPRESS"),
        MAX_UNCOMPRESSED_BLOCK_SIZE("MAX_UNCOMPRESSED_BLOCK_SIZE"),
        MODE("MODE");

        private String configString;
        ConfigFields(String configString){
            this.configString = configString;
        }
        public String asString(){ return configString;}
    }

    final static private String SEPARATING_STRING = " = ";
    final private HashMap<String, String> LOADED_DATA;
    public boolean correctConfig;

    public Config(String filename) {
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
            System.out.println("Cannot open file");
            this.correctConfig = false;
        }

        // Read file
        while (this.correctConfig && scanner.hasNext()) {
            line = scanner.nextLine();

            String[] pair = line.split(SEPARATING_STRING);
            if (pair.length != 2) {
                System.out.println("Incorrect line (double =)");
                this.correctConfig = false;
                break;
            }

            boolean is_line_correct = false;

            for (ConfigFields prefix : ConfigFields.values()) {
                if (pair[0].equalsIgnoreCase(prefix.asString())) {
                    if (defaultHashMap.containsKey(prefix.asString())) {
                        System.out.println("ERROR: prefix " + prefix.asString() + " meeted two times.");
                        this.correctConfig = false;
                        break;
                    }
                    defaultHashMap.put(prefix.asString(), pair[1]);
                    is_line_correct = true;
                    break;
                }
            }
            if (!is_line_correct) {
                System.out.println("ERROR: unknown prefix " + pair[0]);
                this.correctConfig = false;
                break;
            }

            if (!this.correctConfig)
                break;
        }

        if (defaultHashMap.size() != ConfigFields.values().length) {
            System.out.println("ERROR: Not enough fields");
            this.correctConfig = false;
        }

        LOADED_DATA = defaultHashMap;
        scanner.close();
    }

    public String GetField(ConfigFields field) {
        return LOADED_DATA.get(field.asString());
    }
}

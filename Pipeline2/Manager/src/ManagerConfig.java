import com.java_polytech.config_support.IGrammar;

public class ManagerConfig implements IGrammar{
    public enum ConfigFields {
        IN_FILENAME("IN_FILENAME"),
        OUT_FILENAME("OUT_FILENAME"),
        READER_NAME("READER_NAME"),
        WRITER_NAME("WRITER_NAME"),
        EXECUTORS_NAMES("EXECUTORS_NAMES"),
        READER_CONFIG("READER_CONFIG"),
        WRITER_CONFIG("WRITER_CONFIG"),
        EXECUTORS_CONFIGS("EXECUTORS_CONFIGS");

        private String configString;
        ConfigFields(String configString){
            this.configString = configString;
        }
        public String asString(){ return configString;}
    }
    final static private String MAP_SEPARATING_STRING = "=";
    final static private String ARRAY_SEPARATING_STRING = ",";
    final static private String COMMENT_STRING = "#";

    public String getMappingSeparatingString(){
        return MAP_SEPARATING_STRING;
    }
    public String getArraySeparatingString(){
        return ARRAY_SEPARATING_STRING;
    }
    public String getCommentString() {return COMMENT_STRING; }

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
import com.java_polytech.config_support.IGrammar;

public class ManagerConfig implements IGrammar{
    public enum ConfigFields {
        IN_FILENAME("IN_FILENAME"),
        OUT_FILENAME("OUT_FILENAME"),
        READER_NAME("READER_NAME"),
        WRITER_NAME("WRITER_NAME"),
        EXECUTOR_NAME("EXECUTOR_NAME"),
        READER_CONFIG("READER_CONFIG"),
        WRITER_CONFIG("WRITER_CONFIG"),
        EXECUTOR_CONFIG("EXECUTOR_CONFIG");

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
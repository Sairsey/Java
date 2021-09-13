import java.io.*;

public class RLE {
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

    private Config myСonfig;
    private File inputFile;
    private FileInputStream inputFileStream;
    private File outputFile;
    private FileOutputStream outputFileStream;
    private int bufferSize;
    private Mode mode;
    private boolean isInited;

    public RLE(Config config) {
        myСonfig = config;
        isInited = false;
        if (!myСonfig.correct_config) {
            System.out.println("ERROR Incorrect Config. Won't run this");
            return;
        }
        initFields();
    }

    private void initFields(){
        try {
            inputFile = new File(myСonfig.GetField(Config.ConfigFields.IN_FILENAME));
            inputFileStream = new FileInputStream(inputFile);
        }
        catch (FileNotFoundException ex) {
            System.out.println("ERROR File " + myСonfig.GetField(Config.ConfigFields.IN_FILENAME) + " not found");
            return;
        }

        try {
            outputFile = new File(myСonfig.GetField(Config.ConfigFields.OUT_FILENAME));
            outputFileStream = new FileOutputStream(outputFile);
        }
        catch (IOException ex) {
            // close everything after us
            try {
                inputFileStream.close();
            }
            catch (IOException ex1)
            {
                System.out.println("ERROR: Cannot close input file");
            }

            System.out.println("ERROR File " + myСonfig.GetField(Config.ConfigFields.OUT_FILENAME) + "cannot be opened for writing");
            return;
        }

        try {
            bufferSize = Integer.parseInt(myСonfig.GetField(Config.ConfigFields.BUFFER_SIZE));
        }
        catch (NumberFormatException ex) {

            System.out.println("ERROR bufferSize is not a number");
            CloseStreams();
            return;
        }

        mode = Mode.ToEnum(myСonfig.GetField(Config.ConfigFields.MODE));

        isInited = true;
    }


    private void CloseStreams() {
        // close everything after us
        try {
            inputFileStream.close();
        }
        catch (IOException ex)
        {
            System.out.println("ERROR: Cannot close input file");
        }

        try {
            outputFileStream.close();
        }
        catch (IOException ex)
        {
            System.out.println("ERROR: Cannot close output file");
        }
    }

    public void Run() {
        if (isInited)
        {
            if (mode == Mode.COMPRESS)
                Compress();
            else
                Decompress();
        }
    }

    private void Compress() {
        byte buffer[] = new byte[bufferSize];
        int length = 0;


        try {
            length = inputFileStream.read(buffer, 0, bufferSize);
        }
        catch (IOException ex)
        {
            System.out.println("ERROR: Cannot read from input file");
            CloseStreams();
            return;
        }

        try {
            outputFileStream.write(bufferSize); // write chunk size
        }
        catch (IOException ex)
        {
            System.out.println("ERROR: Cannot write to output file");
            CloseStreams();
            return;
        }

        // for every chunk
        while (length > 0)
        {
            // temporary write this chunk
            for (int i = 0; i < length; i++) {
                try {
                    outputFileStream.write(buffer[i]);
                }
                catch (IOException ex)
                {
                    System.out.println("ERROR: Cannot write to output file");
                    CloseStreams();
                    return;
                }
            }

            // read new chunk
            try {
                length = inputFileStream.read(buffer, 0, bufferSize);
            }
            catch (IOException ex)
            {
                System.out.println("ERROR: Cannot read from input file");
                CloseStreams();
                return;
            }
        }

        CloseStreams();
    }

    private void Decompress() {

    }
}

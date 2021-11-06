import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

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

    static class RLEPair {
        public byte element;
        public int amount;

        public RLEPair(byte newElement)
        {
            this.element = newElement;
            this.amount = 1;
        }
    }


    final private int byteMaxValue = 255;
    final private int naturalMinValue = 1;
    private Config myСonfig;
    private BufferedReader inputFile;
    private BufferedWriter outputFile;
    private int inputBufferSize;
    private int outputBufferSize;
    private int minCompressAmount;
    private int maxCompressAmount;
    private int maxUncompressedAmount;
    private Mode mode;
    private boolean isInited;

    public RLE(Config config) {
        myСonfig = config;
        isInited = false;
        if (!myСonfig.correctConfig) {
            System.out.println("ERROR Incorrect Config. Won't run this");
            return;
        }
        initFields();
    }

    private void initFields(){
        try {
            inputFile = new BufferedReader(myСonfig.GetField(Config.ConfigFields.IN_FILENAME));
        }
        catch (FileNotFoundException ex) {
            System.out.println("ERROR File " + myСonfig.GetField(Config.ConfigFields.IN_FILENAME) + " not found");
            return;
        }

        try {
            outputFile = new BufferedWriter(myСonfig.GetField(Config.ConfigFields.OUT_FILENAME));
        }
        catch (IOException ex) {
            // close everything after us
            try {
                inputFile.Close();
            }
            catch (IOException ex1) {
                System.out.println("ERROR: Cannot close input file");
            }

            System.out.println("ERROR File " + myСonfig.GetField(Config.ConfigFields.OUT_FILENAME) + "cannot be opened for writing");
            return;
        }

        try {
            inputBufferSize = Integer.parseInt(myСonfig.GetField(Config.ConfigFields.IN_BUFFER_SIZE));
        }
        catch (NumberFormatException ex) {
            System.out.println("ERROR inputBufferSize is not a number");
            CloseStreams();
            return;
        }

        try {
            outputBufferSize = Integer.parseInt(myСonfig.GetField(Config.ConfigFields.OUT_BUFFER_SIZE));
        }
        catch (NumberFormatException ex) {
            System.out.println("ERROR outputBufferSize is not a number");
            CloseStreams();
            return;
        }

        try {
            minCompressAmount = Integer.parseInt(myСonfig.GetField(Config.ConfigFields.MIN_AMOUNT_TO_COMPRESS));
        }
        catch (NumberFormatException ex) {
            System.out.println("ERROR minCompressAmount is not a number");
            CloseStreams();
            return;
        }

        try {
            maxCompressAmount = Integer.parseInt(myСonfig.GetField(Config.ConfigFields.MAX_AMOUNT_TO_COMPRESS));
        }
        catch (NumberFormatException ex) {
            System.out.println("ERROR maxCompressAmount is not a number");
            CloseStreams();
            return;
        }

        try {
            maxUncompressedAmount = Integer.parseInt(myСonfig.GetField(Config.ConfigFields.MAX_UNCOMPRESSED_BLOCK_SIZE));
        }
        catch (NumberFormatException ex) {
            System.out.println("ERROR maxUncompressedAmount is not a number");
            CloseStreams();
            return;
        }

        mode = Mode.ToEnum(myСonfig.GetField(Config.ConfigFields.MODE));

        // check if minCompressAmount is valid
        if (minCompressAmount < naturalMinValue){
            System.out.println("ERROR minCompressAmount incorrect it must be 1 or greater");
            CloseStreams();
            return;
        }

        // check if maxCompressAmount is valid
        if (maxCompressAmount < naturalMinValue || maxCompressAmount > byteMaxValue) {
            System.out.println("ERROR maxCompressAmount incorrect. it must be from 2 to 255");
            CloseStreams();
            return;
        }

        // check if maxCompressAmount is valid
        if (maxUncompressedAmount < naturalMinValue || maxUncompressedAmount > byteMaxValue) {
            System.out.println("ERROR maxUncompressedAmount incorrect. it must be from 2 to 255");
            CloseStreams();
            return;
        }

        if (minCompressAmount >= maxCompressAmount) {
            System.out.println("ERROR maxCompressAmount must be bigger than minCompressAmount");
            CloseStreams();
            return;
        }

        isInited = true;
    }


    private void CloseStreams() {
        // close everything after us
        try {
            inputFile.Close();
        }
        catch (IOException ex)
        {
            System.out.println("ERROR: Cannot close input file");
        }

        try {
            outputFile.Close();
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
            else if (mode == Mode.DECOMPRESS)
                Decompress();
            else
                System.out.println("ERROR: unknown mode type");
        }
    }

    private void Compress() {
        inputFile.SetBuffer(inputBufferSize);
        outputFile.SetBuffer(outputBufferSize);
        ArrayList<RLEPair> currentSymbols = new ArrayList<>();
        int currentUncompressedLen = 0;

        try {
            inputFile.UpdateIfNecessary();
        }
        catch (IOException ex)
        {
            System.out.println("ERROR: Cannot read from input file");
            CloseStreams();
            return;
        }


        while (inputFile.NotEnded())
        {
            byte data = inputFile.ReadByte();
            // to not overflow our uncompressed buffer
            if (currentUncompressedLen == maxUncompressedAmount) {
                if (!WriteArrayAsRLE(currentSymbols, currentUncompressedLen)) // save
                    return;
                currentSymbols.clear();
                currentUncompressedLen = 0;
            }

            // if we met same symbol
            if (currentSymbols.size() > 0 && data == currentSymbols.get(currentSymbols.size() - 1).element) {
                currentSymbols.get(currentSymbols.size() - 1).amount++;
                currentUncompressedLen++;
                if (currentSymbols.get(currentSymbols.size() - 1).amount == maxCompressAmount) {
                    if (!WriteArrayAsRLE(currentSymbols, currentUncompressedLen)) // save
                        return;
                    currentSymbols.clear();
                    currentUncompressedLen = 0;
                }
            }
            else {
                if (currentSymbols.size() == 0 || // if we read first symbol in series
                        currentSymbols.get(currentSymbols.size() - 1).amount < this.minCompressAmount) {// or here not enough symbols to compress
                    currentSymbols.add(new RLEPair(data)); // just add new symbol to our buffer
                    currentUncompressedLen++;
                }
                else { // otherwise, we write to file and add new
                    if (!WriteArrayAsRLE(currentSymbols, currentUncompressedLen)) // save
                        return;
                    currentSymbols.clear();
                    currentUncompressedLen = 0;
                    currentSymbols.add(new RLEPair(data));
                    currentUncompressedLen++;
                }
            }

            // read new chunk
            try {
                inputFile.UpdateIfNecessary();
            }
            catch (IOException ex)
            {
                System.out.println("ERROR: Cannot read from input file");
                CloseStreams();
                return;
            }
        }

        // write all left data
        WriteArrayAsRLE(currentSymbols, currentUncompressedLen);

        CloseStreams();
    }

    // helper function to print ArrayList<RLEPair> to file.
    private boolean WriteArrayAsRLE( ArrayList<RLEPair> currentSymbols, int currentLength ) {
        boolean isWriteCompressed = false;
        boolean isWriteUncompressed = false;

        // determine which we must write
        if (currentSymbols.get(currentSymbols.size() - 1).amount >= minCompressAmount)
            isWriteCompressed = true;

        if ((isWriteCompressed && currentSymbols.size() > 1) || (!isWriteCompressed && currentSymbols.size() > 0))
            isWriteUncompressed = true;

        // correct prefix data in case we have both compressed and not-compressed data
        if (isWriteCompressed)
            currentLength = currentLength - currentSymbols.get(currentSymbols.size() - 1).amount;

        // write special prefix in case of uncompressed
        if (isWriteUncompressed) {
            try {
                outputFile.WriteByte((byte)0);
                outputFile.WriteByte((byte)currentLength);
            } catch (IOException ex) {
                System.out.println("ERROR: Cannot write to output file");
                CloseStreams();
                return false;
            }
        }

        int cycleBorder = currentSymbols.size() - 1;

        if (!isWriteCompressed)
            cycleBorder = currentSymbols.size();

        for (int i = 0; i < cycleBorder; i++) {
            try {
                for (int j = 0; j < currentSymbols.get(i).amount; j++)
                    outputFile.WriteByte(currentSymbols.get(i).element);
            } catch (IOException ex) {
                System.out.println("ERROR: Cannot write to output file");
                CloseStreams();
                return false;
            }
        }

        if (isWriteCompressed) {
            // write last (which must be RLE encodable)
            try {
                outputFile.WriteByte((byte)currentSymbols.get(currentSymbols.size() - 1).amount);
                outputFile.WriteByte(currentSymbols.get(currentSymbols.size() - 1).element);
            } catch (IOException ex) {
                System.out.println("ERROR: Cannot write to output file");
                CloseStreams();
                return false;
            }
        }
        System.out.println("SUCCESSFULLY COMPRESSED");
        return true;
    }

    private void Decompress() {
        inputFile.SetBuffer(inputBufferSize);
        outputFile.SetBuffer(outputBufferSize);
        boolean is_compressed_data = true;
        int not_compressed_amount = 0;
        int compressed_amount = 0;

        try {
            inputFile.UpdateIfNecessary();
        }
        catch (IOException ex)
        {
            System.out.println("ERROR: Cannot read from input file");
            CloseStreams();
            return;
        }

        // for every chunk
        while (inputFile.NotEnded())
        {
            byte data = inputFile.ReadByte();
            if (is_compressed_data) {
                if (compressed_amount == 0) {
                    compressed_amount = Byte.toUnsignedInt(data);
                    if (compressed_amount == 0) {   // if still 0 then we must change
                        is_compressed_data = false;
                        not_compressed_amount = 0;
                    }
                }
                else {
                    try {
                        for (int j = 0; j < compressed_amount; j++)
                            outputFile.WriteByte(data);
                    }
                    catch (IOException ex) {
                        System.out.println("ERROR: Cannot write to file");
                        CloseStreams();
                        return;
                    }
                    compressed_amount = 0;
                }
            }
            else {
                if (not_compressed_amount == 0)
                    not_compressed_amount = Byte.toUnsignedInt(data);
                else {
                    try {
                        outputFile.WriteByte(data);
                    }
                    catch (IOException ex) {
                        System.out.println("ERROR: Cannot write to file");
                        CloseStreams();
                        return;
                    }
                    not_compressed_amount--;
                    if (not_compressed_amount == 0) {
                        is_compressed_data = true;
                        compressed_amount = 0;
                    }
                }
            }

            // read new chunk
            try {
                inputFile.UpdateIfNecessary();
            }
            catch (IOException ex)
            {
                System.out.println("ERROR: Cannot read from input file");
                CloseStreams();
                return;
            }
        }

        CloseStreams();
        System.out.println("SUCCESSFULLY DECOMPRESSED");
    }
}

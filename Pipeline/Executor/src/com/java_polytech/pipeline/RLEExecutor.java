package com.java_polytech.pipeline;

import com.java_polytech.pipeline_interfaces.IConsumer;
import com.java_polytech.pipeline_interfaces.IExecutor;
import com.java_polytech.pipeline_interfaces.RC;

import java.util.ArrayList;

import static com.java_polytech.pipeline_interfaces.RC.RC_SUCCESS;

public class RLEExecutor implements IExecutor {
    boolean IsConsumerInited = false;
    boolean IsConfigInited = false;
    IConsumer Next;

    enum Mode {
        UNKNOWN("UNKNOWN"),
        COMPRESS("COMPRESS"),
        DECOMPRESS("DECOMPRESS");

        private final String configString;
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

    private int minCompressAmount;
    private int maxCompressAmount;
    private int maxUncompressedAmount;
    private Mode mode;

    // compress specific variables
    ArrayList<RLEPair> currentSymbols;
    int currentUncompressedLen = 0;
    // decompress specific variables
    boolean is_compressed_data = true;
    int not_compressed_amount = 0;
    int compressed_amount = 0;

    byte[] outBuffer;
    int outBufferIndex = 0;
    int outBufferSize = 0;

    public RC consume(byte[] bytes) {
        if (bytes == null) {

            if (mode == Mode.COMPRESS && currentSymbols.size() != 0)
                WriteArrayAsRLE(currentSymbols, currentUncompressedLen);

            return OutBufferClose();
        }

        if (!IsConfigInited)
            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "No config is set");

        if (!IsConsumerInited)
            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "No consumer inited");

        if (mode == Mode.COMPRESS)
            return Compress(bytes);
        else
            return Decompress(bytes);
    }

    public RC setConsumer(IConsumer iConsumer) {
        Next = iConsumer;
        IsConsumerInited = true;
        return RC.RC_SUCCESS;
    }

    public RC setConfig(String s) {
        RLEConfig config = new RLEConfig();
        RC code = config.process(s);
        if (code.isSuccess())
        {
            try {
                minCompressAmount = Integer.parseInt(config.GetField(RLEConfig.ConfigFields.MIN_AMOUNT_TO_COMPRESS));
            }
            catch (NumberFormatException ex) {
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "minCompressAmount is not a number");
            }

            try {
                outBufferSize = Integer.parseInt(config.GetField(RLEConfig.ConfigFields.OUT_BUFFER_SIZE));
            }
            catch (NumberFormatException ex) {
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "outBufferSize is not a number");
            }

            outBuffer = new byte[outBufferSize];
            outBufferIndex = 0;

            try {
                maxCompressAmount = Integer.parseInt(config.GetField(RLEConfig.ConfigFields.MAX_AMOUNT_TO_COMPRESS));
            }
            catch (NumberFormatException ex) {
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "maxCompressAmount is not a number");
            }

            try {
                maxUncompressedAmount = Integer.parseInt(config.GetField(RLEConfig.ConfigFields.MAX_UNCOMPRESSED_BLOCK_SIZE));
            }
            catch (NumberFormatException ex) {
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "maxUncompressedAmount is not a number");
            }

            mode = Mode.ToEnum(config.GetField(RLEConfig.ConfigFields.MODE));

            if (mode == Mode.UNKNOWN)
            {
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "unknown MODE");
            }

            // check if minCompressAmount is valid
            if (minCompressAmount < 1){
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "minCompressAmount incorrect it must be 1 or greater");
            }

            // check if maxCompressAmount is valid
            if (maxCompressAmount < 1 || maxCompressAmount > 255) {
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "maxCompressAmount incorrect. it must be from 2 to 255");
            }

            // check if maxCompressAmount is valid
            if (maxUncompressedAmount < 1 || maxUncompressedAmount > 255) {
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "maxUncompressedAmount incorrect. it must be from 2 to 255");
            }

            if (minCompressAmount >= maxCompressAmount) {
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "maxCompressAmount must be bigger than minCompressAmount");
            }

            currentSymbols = new ArrayList<RLEPair>();

            IsConfigInited = true;
            return RC_SUCCESS;
        }
        return code;
    }

    private RC Compress(byte[] bytes) {
        int index = 0;
        RC tmp_rc;
        while (index < bytes.length)
        {
            byte data = bytes[index];
            // to not overflow our uncompressed buffer
            if (currentUncompressedLen == maxUncompressedAmount) {
                tmp_rc = WriteArrayAsRLE(currentSymbols, currentUncompressedLen);
                if (!tmp_rc.isSuccess()) // save
                    return tmp_rc;
                currentSymbols.clear();
                currentUncompressedLen = 0;
            }

            // if we met same symbol
            if (currentSymbols.size() > 0 && data == currentSymbols.get(currentSymbols.size() - 1).element) {
                currentSymbols.get(currentSymbols.size() - 1).amount++;
                currentUncompressedLen++;
                if (currentSymbols.get(currentSymbols.size() - 1).amount == maxCompressAmount) {
                    tmp_rc = WriteArrayAsRLE(currentSymbols, currentUncompressedLen);
                    if (!tmp_rc.isSuccess()) // save
                        return tmp_rc;
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
                    tmp_rc = WriteArrayAsRLE(currentSymbols, currentUncompressedLen);
                    if (!tmp_rc.isSuccess()) // save
                        return tmp_rc;
                    currentSymbols.clear();
                    currentUncompressedLen = 0;
                    currentSymbols.add(new RLEPair(data));
                    currentUncompressedLen++;
                }
            }
            index++;
        }

        // write all left data
        return RC_SUCCESS;
    }

    private RC Decompress(byte[] bytes) {
        int index = 0;
        // for every chunk
        while (index < bytes.length)
        {
            byte data = bytes[index];
            if (is_compressed_data) {
                if (compressed_amount == 0) {
                    compressed_amount = Byte.toUnsignedInt(data);
                    if (compressed_amount == 0) {   // if still 0 then we must change
                        is_compressed_data = false;
                        not_compressed_amount = 0;
                    }
                }
                else {
                    for (int j = 0; j < compressed_amount; j++) {
                        RC tmp_rc = OutBufferWriteByte(data);
                        if (!tmp_rc.isSuccess())
                            return tmp_rc;
                    }
                    compressed_amount = 0;
                }
            }
            else {
                if (not_compressed_amount == 0)
                    not_compressed_amount = Byte.toUnsignedInt(data);
                else {
                    RC tmp_rc = OutBufferWriteByte(data);
                    if (!tmp_rc.isSuccess())
                        return tmp_rc;
                    not_compressed_amount--;
                    if (not_compressed_amount == 0) {
                        is_compressed_data = true;
                        compressed_amount = 0;
                    }
                }
            }

            index++;
        }
        return RC_SUCCESS;
    }

    private RC OutBufferWriteByte(byte b) {
        outBuffer[outBufferIndex] = b;
        outBufferIndex++;
        if (outBufferIndex == outBufferSize)
        {
            RC tmp_rc = Next.consume(outBuffer);
            outBufferIndex = 0;
            return tmp_rc;
        }
        return RC.RC_SUCCESS;
    }

    private RC OutBufferClose() {
        byte[] tmp_buf = new byte[outBufferIndex];
        System.arraycopy(outBuffer,0,tmp_buf, 0, outBufferIndex);
        RC tmp_rc = Next.consume(tmp_buf);
        if (!tmp_rc.isSuccess())
            return tmp_rc;
        outBufferIndex = 0;
        Next.consume(null);
        return RC.RC_SUCCESS;
    }

    // helper function to print ArrayList<RLEPair> to file.
    private RC WriteArrayAsRLE( ArrayList<RLEPair> currentSymbols, int currentLength ) {
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
            RC tmp_rc;
            tmp_rc = OutBufferWriteByte((byte)0);
            if (!tmp_rc.isSuccess())
                return tmp_rc;
            tmp_rc = OutBufferWriteByte((byte)currentLength);
            if (!tmp_rc.isSuccess())
                return tmp_rc;
        }

        int cycleBorder = currentSymbols.size() - 1;

        if (!isWriteCompressed)
            cycleBorder = currentSymbols.size();

        for (int i = 0; i < cycleBorder; i++) {
                for (int j = 0; j < currentSymbols.get(i).amount; j++) {
                    RC tmp_rc;
                    tmp_rc = OutBufferWriteByte(currentSymbols.get(i).element);
                    if (!tmp_rc.isSuccess())
                        return tmp_rc;
                }
        }

        if (isWriteCompressed) {
            // write last (which must be RLE encodable)
            RC tmp_rc;
            tmp_rc = OutBufferWriteByte((byte)currentSymbols.get(currentSymbols.size() - 1).amount);
            if (!tmp_rc.isSuccess())
                return tmp_rc;
            tmp_rc = OutBufferWriteByte(currentSymbols.get(currentSymbols.size() - 1).element);
            if (!tmp_rc.isSuccess())
                return tmp_rc;
        }
        return RC_SUCCESS;
    }
}

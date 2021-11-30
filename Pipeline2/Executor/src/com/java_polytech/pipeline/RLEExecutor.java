package com.java_polytech.pipeline;

import com.java_polytech.config_support.SyntaxAnalyzer;
import com.java_polytech.pipeline_interfaces.*;
import javafx.util.Pair;

import java.util.ArrayList;

import static com.java_polytech.pipeline_interfaces.RC.*;

public class RLEExecutor implements IExecutor {
    boolean IsConsumerInited = false;
    boolean IsConfigInited = false;
    boolean IsTypeDecided = false;
    IProvider Prev;
    IMediator Mediator;

    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY};
    private TYPE decidedType;

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

    final private int byteMaxValue = 255;
    final private int naturalMinValue = 1;

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

    class ByteArrayMediator implements IMediator {
        public Object getData() {
            byte[] data = new byte[outBufferIndex];
            if (data.length == 0) {
                return null;
            }
            System.arraycopy(outBuffer,0,data, 0, outBufferIndex);
            return data;
        }
    }

    public RC setConfig(String s) {
        SyntaxAnalyzer config = new SyntaxAnalyzer(RC.RCWho.EXECUTOR, new RLEConfig());
        RC code = config.process(s);
        if (code.isSuccess())
        {
            try {
                Pair<RC, ArrayList<String>> val = config.GetField(RLEConfig.ConfigFields.MIN_AMOUNT_TO_COMPRESS.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();
                if (val.getValue().size() != 1)
                    return new RC(RCWho.EXECUTOR, RCType.CODE_CUSTOM_ERROR, "MIN_AMOUNT_TO_COMPRESS must be 1 value, not an array");

                minCompressAmount = Integer.parseInt(val.getValue().get(0));
            }
            catch (NumberFormatException ex) {
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "minCompressAmount is not a number");
            }

            try {
                Pair<RC, ArrayList<String>> val = config.GetField(RLEConfig.ConfigFields.OUT_BUFFER_SIZE.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();
                if (val.getValue().size() != 1)
                    return new RC(RCWho.EXECUTOR, RCType.CODE_CUSTOM_ERROR, "OUT_BUFFER_SIZE must be 1 value, not an array");

                outBufferSize = Integer.parseInt(val.getValue().get(0));
            }
            catch (NumberFormatException ex) {
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "outBufferSize is not a number");
            }

            outBuffer = new byte[outBufferSize];
            outBufferIndex = 0;

            try {
                Pair<RC, ArrayList<String>> val = config.GetField(RLEConfig.ConfigFields.MAX_AMOUNT_TO_COMPRESS.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();
                if (val.getValue().size() != 1)
                    return new RC(RCWho.EXECUTOR, RCType.CODE_CUSTOM_ERROR, "MAX_AMOUNT_TO_COMPRESS must be 1 value, not an array");

                maxCompressAmount = Integer.parseInt(val.getValue().get(0));
            }
            catch (NumberFormatException ex) {
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "maxCompressAmount is not a number");
            }

            try {
                Pair<RC, ArrayList<String>> val = config.GetField(RLEConfig.ConfigFields.MAX_UNCOMPRESSED_BLOCK_SIZE.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();
                if (val.getValue().size() != 1)
                    return new RC(RCWho.EXECUTOR, RCType.CODE_CUSTOM_ERROR, "MAX_UNCOMPRESSED_BLOCK_SIZE must be 1 value, not an array");

                maxUncompressedAmount = Integer.parseInt(val.getValue().get(0));
            }
            catch (NumberFormatException ex) {
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "maxUncompressedAmount is not a number");
            }

            Pair<RC, ArrayList<String>> val = config.GetField(RLEConfig.ConfigFields.MODE.asString());
            if (!val.getKey().isSuccess())
                return val.getKey();
            if (val.getValue().size() != 1)
                return new RC(RCWho.EXECUTOR, RCType.CODE_CUSTOM_ERROR, "MODE must be 1 value, not an array");


            mode = Mode.ToEnum(val.getValue().get(0));

            if (mode == Mode.UNKNOWN)
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "unknown MODE");

            // check if minCompressAmount is valid
            if (minCompressAmount < naturalMinValue){
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "minCompressAmount incorrect it must be 1 or greater");
            }

            // check if maxCompressAmount is valid
            if (maxCompressAmount < naturalMinValue || maxCompressAmount > byteMaxValue) {
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CONFIG_SEMANTIC_ERROR, "maxCompressAmount incorrect. it must be from 2 to 255");
            }

            // check if maxCompressAmount is valid
            if (maxUncompressedAmount < naturalMinValue || maxUncompressedAmount > byteMaxValue) {
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

    public RC setProvider(IProvider provider) {
        Prev = provider;
        TYPE provided_types[] = provider.getOutputTypes();
        for (int my_type = 0; my_type < supportedTypes.length && !IsTypeDecided; my_type++)
            for (int provided_type = 0; provided_type < provided_types.length && !IsTypeDecided; provided_type++)
                if (provided_types[provided_type].equals(supportedTypes[my_type]))
                {
                    decidedType = supportedTypes[my_type];
                    IsTypeDecided = true;
                }

        if (!IsTypeDecided)
        {
            return RC_EXECUTOR_TYPES_INTERSECTION_EMPTY_ERROR;
        }

        Mediator = Prev.getMediator(decidedType);
        return RC.RC_SUCCESS;
    }

    public RC setConsumer(IConsumer iConsumer) {
        Next = iConsumer;
        IsConsumerInited = true;
        RC rc = iConsumer.setProvider(this);
        if (!rc.isSuccess())
            return rc;
        return RC.RC_SUCCESS;
    }

    public TYPE[] getOutputTypes()
    {
        return supportedTypes;
    }

    public IMediator getMediator(TYPE Type)
    {
        if (Type.equals(TYPE.BYTE_ARRAY))
            return new ByteArrayMediator();
        else
            return null;
    }

    public RC consume() {
        if (!IsConfigInited)
            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "No config is set");

        if (!IsConsumerInited)
            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "No consumer inited");

        byte bytes[] = (byte[])Mediator.getData();
        if (bytes == null) {

            if (mode == Mode.COMPRESS && currentSymbols.size() != 0)
                WriteArrayAsRLE(currentSymbols, currentUncompressedLen);

            return OutBufferClose();
        }

        if (mode == Mode.COMPRESS)
            return Compress(bytes);
        else
            return Decompress(bytes);
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
            RC tmp_rc = Next.consume();
            outBufferIndex = 0;
            return tmp_rc;
        }
        return RC.RC_SUCCESS;
    }

    private RC OutBufferClose() {
        RC tmp_rc = Next.consume();
        if (!tmp_rc.isSuccess())
            return tmp_rc;
        outBufferIndex = 0;
        outBuffer = null;
        Next.consume();
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

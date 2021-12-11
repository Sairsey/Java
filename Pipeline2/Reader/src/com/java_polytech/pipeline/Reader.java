package com.java_polytech.pipeline;

import com.java_polytech.config_support.SyntaxAnalyzer;
import com.java_polytech.pipeline_interfaces.*;
import javafx.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static com.java_polytech.pipeline_interfaces.RC.*;

public class Reader implements IReader {
    boolean IsConsumerInited = false;
    boolean IsConfigInited = false;
    boolean IsInputStreamInited = false;

    InputStream inputStream;
    private byte buffer[];
    private int bufferSize = 0;
    private int readedLength = 0;

    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};

    IConsumer Next;

    class ByteArrayMediator implements IMediator {
        public Object getData() {
            byte[] data = new byte[readedLength];
            if (readedLength == 0) {
                return null;
            }
            System.arraycopy(buffer,0,data, 0, readedLength);
            return data;
        }
    }

    class CharArrayMediator implements IMediator {
        public Object getData() {
            byte[] data = new byte[readedLength];
            if (data.length == 0) {
                return null;
            }
            System.arraycopy(buffer,0,data, 0, readedLength);
            CharBuffer charBuf =
                    ByteBuffer.wrap(data)
                            .order(ByteOrder.BIG_ENDIAN)
                            .asCharBuffer();
            char[] array = new char[charBuf.remaining()];
            charBuf.get(array);
            return array;
        }
    }

    class IntArrayMediator implements IMediator {
        public Object getData() {
            byte[] data = new byte[readedLength];
            if (data.length == 0) {
                return null;
            }
            System.arraycopy(buffer,0,data, 0, readedLength);
            IntBuffer intBuf =
                    ByteBuffer.wrap(data)
                            .order(ByteOrder.BIG_ENDIAN)
                            .asIntBuffer();
            int[] array = new int[intBuf.remaining()];
            intBuf.get(array);
            return array;
        }
    }

    public RC setConfig(String s) {
        SyntaxAnalyzer config = new SyntaxAnalyzer(RCWho.READER, new ReaderConfig());
        RC code = config.process(s);
        if (code.isSuccess())
        {
            try {
                Pair<RC, ArrayList<String>> val = config.GetField(ReaderConfig.ConfigFields.BUFFER_SIZE.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();
                if (val.getValue().size() != 1)
                    return new RC(RCWho.READER, RCType.CODE_CUSTOM_ERROR, "BUFFER_SIZE must be 1 value, not an array");

                bufferSize = Integer.parseInt(val.getValue().get(0));
            }
            catch (NumberFormatException ex) {
                return RC_READER_CONFIG_SEMANTIC_ERROR;
            }

            if (bufferSize <= 0 || bufferSize % 4 != 0)
                return new RC(RCWho.READER, RCType.CODE_CUSTOM_ERROR, "Invalid BUFFER_SIZE. It must be positive number and dividable by 4.");

            buffer = new byte[bufferSize];
            this.readedLength = 0;

            IsConfigInited = true;
            return RC_SUCCESS;
        }
        return code;
    }

    public RC setConsumer(IConsumer iConsumer) {
        Next = iConsumer;
        RC rc = iConsumer.setProvider(this);
        if (!rc.isSuccess())
            return rc;
        IsConsumerInited = true;
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
        else if (Type.equals(TYPE.CHAR_ARRAY))
            return new CharArrayMediator();
        else if (Type.equals(TYPE.INT_ARRAY))
            return new IntArrayMediator();
        else
            return null;
    }

    public RC setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        IsInputStreamInited = true;
        return RC.RC_SUCCESS;
    }

    public RC run() {
        if (!IsConfigInited)
            return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "No config is set");

        if (!IsConsumerInited)
            return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "No consumer inited");

        if (!IsInputStreamInited)
            return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "No input stream inited");

        try {
            readedLength = inputStream.read(buffer, 0, bufferSize);
        } catch (IOException e) {
            return RC_READER_FAILED_TO_READ;
        }

        while (readedLength > 0)
        {
            RC tmp_rc;

            tmp_rc = Next.consume();
            if (!tmp_rc.isSuccess()) {
                buffer = null;
                readedLength = 0;
                Next.consume();
                return tmp_rc;
            }

            try {
                readedLength = inputStream.read(buffer, 0, bufferSize);
            } catch (IOException e) {
                return RC_READER_FAILED_TO_READ;
            }
        }

        readedLength = 0;
        Next.consume();

        return RC.RC_SUCCESS;
    }
}

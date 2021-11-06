package com.java_polytech.pipeline;

import com.java_polytech.config_support.SyntaxAnalyzer;
import com.java_polytech.pipeline_interfaces.IConsumer;
import com.java_polytech.pipeline_interfaces.IReader;
import com.java_polytech.pipeline_interfaces.RC;
import javafx.util.Pair;

import java.io.IOException;
import java.io.InputStream;

import static com.java_polytech.pipeline_interfaces.RC.*;

public class Reader implements IReader {
    boolean IsConsumerInited = false;
    boolean IsConfigInited = false;
    boolean IsInputStreamInited = false;

    InputStream inputStream;
    private byte buffer[];
    private int bufferSize = 0;
    private int readedLength = 0;

    IConsumer Next;


    public RC setConfig(String s) {
        SyntaxAnalyzer config = new SyntaxAnalyzer(RCWho.READER, new ReaderConfig());
        RC code = config.process(s);
        if (code.isSuccess())
        {
            try {
                Pair<RC, String> val = config.GetField(ReaderConfig.ConfigFields.BUFFER_SIZE.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();

                bufferSize = Integer.parseInt(val.getValue());
            }
            catch (NumberFormatException ex) {
                return RC_READER_CONFIG_SEMANTIC_ERROR;
            }

            buffer = new byte[bufferSize];
            this.bufferSize = bufferSize;
            this.readedLength = 0;

            IsConfigInited = true;
            return RC_SUCCESS;
        }
        return code;
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
            byte tmp_byte_buffer[] = new byte[readedLength];
            RC tmp_rc;
            System.arraycopy(buffer,0,tmp_byte_buffer, 0, readedLength);
            tmp_rc = Next.consume(tmp_byte_buffer);
            if (!tmp_rc.isSuccess()) {
                Next.consume(null);
                return tmp_rc;
            }

            try {
                readedLength = inputStream.read(buffer, 0, bufferSize);
            } catch (IOException e) {
                return RC_READER_FAILED_TO_READ;
            }
        }

        Next.consume(null);

        return RC.RC_SUCCESS;
    }

    public RC setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        IsInputStreamInited = true;
        return RC.RC_SUCCESS;
    }

    public RC setConsumer(IConsumer iConsumer) {
        Next = iConsumer;
        IsConsumerInited = true;
        return RC.RC_SUCCESS;
    }
}

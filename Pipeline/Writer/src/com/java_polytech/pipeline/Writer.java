package com.java_polytech.pipeline;

import com.java_polytech.config_support.SyntaxAnalyzer;
import com.java_polytech.pipeline_interfaces.IWriter;
import com.java_polytech.pipeline_interfaces.RC;
import javafx.util.Pair;

import java.io.IOException;
import java.io.OutputStream;

import static com.java_polytech.pipeline_interfaces.RC.*;

public class Writer implements IWriter {
    boolean IsConfigInited = false;
    boolean IsOutputStreamInited = false;

    OutputStream outputStream;
    private byte buffer[];
    private int bufferSize = 0;
    private int filledLength = 0;

    public RC setConfig(String s) {
        SyntaxAnalyzer config = new SyntaxAnalyzer(RCWho.WRITER, new WriterConfig());
        RC code = config.process(s);
        if (code.isSuccess())
        {
            try {
                Pair<RC, String> val = config.GetField(WriterConfig.ConfigFields.BUFFER_SIZE.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();

                bufferSize = Integer.parseInt(val.getValue());
            }
            catch (NumberFormatException ex) {
                return RC_WRITER_CONFIG_SEMANTIC_ERROR;
            }

            buffer = new byte[bufferSize];
            this.bufferSize = bufferSize;
            this.filledLength = 0;

            IsConfigInited = true;
            return RC_SUCCESS;
        }
        return code;
    }

    public RC consume(byte[] bytes) {
        if (bytes == null && filledLength != 0)
        {
            try {
                outputStream.write(buffer, 0, filledLength);
            } catch (IOException e) {
                return RC_WRITER_FAILED_TO_WRITE;
            }
            return RC_SUCCESS;
        }

        if (!IsConfigInited)
            return new RC(RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "No config is set");

        if (!IsOutputStreamInited)
            return new RC(RC.RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "No output stream inited");

        for (int bytes_index = 0; bytes_index < bytes.length; bytes_index++)
        {
            if (filledLength == bufferSize) {
                try {
                    outputStream.write(buffer, 0, filledLength);
                } catch (IOException e) {
                    return RC_WRITER_FAILED_TO_WRITE;
                }
                filledLength = 0;
            }

            buffer[filledLength] = bytes[bytes_index];
            filledLength++;
        }

        return RC.RC_SUCCESS;
    }

    public RC setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
        IsOutputStreamInited = true;
        return RC.RC_SUCCESS;
    }
}

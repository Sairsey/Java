package com.java_polytech.pipeline;

import com.java_polytech.config_support.SyntaxAnalyzer;
import com.java_polytech.pipeline_interfaces.*;
import javafx.util.Pair;
import org.omg.CORBA.Current;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import static com.java_polytech.pipeline_interfaces.RC.*;

public class Writer implements IWriter {
    boolean IsConfigInited = false;
    boolean IsOutputStreamInited = false;
    boolean IsTypeDecided = false;

    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.INT_ARRAY, TYPE.CHAR_ARRAY};

    private TYPE decidedType;

    RC CurrentState = RC_SUCCESS;
    public RC getCurrentState() {
        return CurrentState;
    }

    IProvider Prev;
    IMediator Mediator;

    OutputStream outputStream;
    private byte buffer[];
    private int bufferSize = 0;
    private int filledLength = 0;
    private ArrayDeque<Long> avaliablePackets = new ArrayDeque<>();

    public RC setConfig(String s) {
        SyntaxAnalyzer config = new SyntaxAnalyzer(RCWho.WRITER, new WriterConfig());
        RC code = config.process(s);
        if (code.isSuccess())
        {
            try {
                Pair<RC, ArrayList<String>> val = config.GetField(WriterConfig.ConfigFields.BUFFER_SIZE.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();
                if (val.getValue().size() != 1)
                    return new RC(RCWho.WRITER, RCType.CODE_CUSTOM_ERROR, "BUFFER_SIZE must be 1 value, not an array");

                bufferSize = Integer.parseInt(val.getValue().get(0));
            }
            catch (NumberFormatException ex) {
                return RC_WRITER_CONFIG_SEMANTIC_ERROR;
            }

            if (bufferSize <= 0 || bufferSize % 4 != 0)
                return new RC(RCWho.READER, RCType.CODE_CUSTOM_ERROR, "Invalid BUFFER_SIZE. It must be positive number and dividable by 4.");

            buffer = new byte[bufferSize];
            this.filledLength = 0;

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
            return RC_WRITER_TYPES_INTERSECTION_EMPTY_ERROR;
        }

        Mediator = Prev.getMediator(decidedType);
        return RC.RC_SUCCESS;
    }

    public RC consume(long packet_number) {
        if (!IsConfigInited)
            return new RC(RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "No config is set");

        if (!IsOutputStreamInited)
            return new RC(RC.RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "No output stream inited");

        avaliablePackets.addLast(packet_number);
        return CurrentState;
    }

    public void run()
    {
        while (avaliablePackets.isEmpty()) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
            }
        }

        long current_packet = avaliablePackets.removeFirst();
        while (current_packet != -1 && CurrentState.isSuccess()) {
            byte bytes[] = null;

            if (decidedType == TYPE.BYTE_ARRAY)
                bytes = (byte[]) Mediator.getData(current_packet);
            else if (decidedType == TYPE.CHAR_ARRAY) {
                char chars[] = (char[]) Mediator.getData(current_packet);
                ByteBuffer byteBuffer = ByteBuffer.allocate(chars.length * 2);
                CharBuffer charBuffer = byteBuffer.asCharBuffer();
                charBuffer.put(chars);
                bytes = byteBuffer.array();
            } else if (decidedType == TYPE.INT_ARRAY) {
                int ints[] = (int[]) Mediator.getData(current_packet);
                ByteBuffer byteBuffer = ByteBuffer.allocate(ints.length * 4);
                IntBuffer intBuffer = byteBuffer.asIntBuffer();
                intBuffer.put(ints);
                bytes = byteBuffer.array();
            }

            if (bytes == null && filledLength != 0) {
                try {
                    outputStream.write(buffer, 0, filledLength);
                } catch (IOException e) {
                    CurrentState = RC_WRITER_FAILED_TO_WRITE;
                    return;
                }
                CurrentState = RC_SUCCESS;
                return;
            } else if (bytes == null) {
                CurrentState = RC_SUCCESS;
                return;
            }
            for (int bytes_index = 0; bytes_index < bytes.length; bytes_index++) {
                if (filledLength == bufferSize) {
                    try {
                        outputStream.write(buffer, 0, filledLength);
                    } catch (IOException e) {
                        CurrentState = RC_WRITER_FAILED_TO_WRITE;
                        return;
                    }
                    filledLength = 0;
                }

                buffer[filledLength] = bytes[bytes_index];
                filledLength++;

            }
            while (avaliablePackets.isEmpty()) {
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                }
            }
            current_packet = avaliablePackets.removeFirst();
        }
        if (filledLength != 0 && CurrentState.isSuccess()) {
            try {
                outputStream.write(buffer, 0, filledLength);
            } catch (IOException e) {
                CurrentState = RC_WRITER_FAILED_TO_WRITE;
                return;
            }
        }
        return;
    }

    public RC setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
        IsOutputStreamInited = true;
        return RC.RC_SUCCESS;
    }
}

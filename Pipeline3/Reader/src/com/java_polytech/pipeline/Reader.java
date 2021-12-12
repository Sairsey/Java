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
import java.util.HashMap;

import static com.java_polytech.pipeline_interfaces.RC.*;

public class Reader implements IReader {
    boolean IsConsumerInited = false;
    boolean IsConfigInited = false;
    boolean IsInputStreamInited = false;

    RC CurrentState = RC_SUCCESS;

    public RC getCurrentState()
    {
        return CurrentState;
    }

    InputStream inputStream;

    int bufferSize = 0;
    private class Buffer
    {
        byte buffer[];
        int readedLength = 0;

        Buffer() {}

        Buffer(Buffer other)
        {
            buffer = new byte[other.buffer.length];
            readedLength = other.readedLength;
            System.arraycopy(other.buffer, 0, buffer, 0, other.buffer.length);
        }
    }
    Buffer currentBuffer;
    long current_packet_number = 0;
    private HashMap<Long, Buffer> readedBuffers = new HashMap<>();

    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY};
    // TODO: Many Consumers Support
    IConsumer Next;

    class ByteArrayMediator implements IMediator {
        public Object getData(long packet_number) {
            if (packet_number == IConsumer.END_OF_FILE_PACKET_NUMBER)
            {
                CurrentState = new RC(RCWho.READER, RCType.CODE_CUSTOM_ERROR, "Invalid index asked");
                return null;
            }
            while (!readedBuffers.containsKey(packet_number) &&
                    current_packet_number != IConsumer.END_OF_FILE_PACKET_NUMBER)
            {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {}
            }
            if (!readedBuffers.containsKey(packet_number) && current_packet_number == IConsumer.END_OF_FILE_PACKET_NUMBER) {
                CurrentState = new RC(RCWho.READER, RCType.CODE_CUSTOM_ERROR, "Invalid index asked");
                return null;
            }
            if (readedBuffers.containsKey(packet_number)) {
                Buffer b = readedBuffers.get(packet_number);
                byte[] data = new byte[b.readedLength];
                if (b.readedLength == 0) {
                    CurrentState = new RC(RCWho.READER, RCType.CODE_CUSTOM_ERROR, "Something wrong with file reading");
                    return null;
                }
                System.arraycopy(b.buffer, 0, data, 0, b.readedLength);
                // TODO: Many Consumers Support
                readedBuffers.remove(packet_number);
                return data;
            }
            CurrentState = new RC(RCWho.READER, RCType.CODE_CUSTOM_ERROR, "Something goes wrong in Mediator");
            return null;
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

            currentBuffer = new Buffer();
            currentBuffer.buffer = new byte[bufferSize];
            currentBuffer.readedLength = 0;

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
        else
            return null;
    }

    public RC setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        IsInputStreamInited = true;
        return RC.RC_SUCCESS;
    }

    public void run() {
        if (!IsConfigInited) {
            CurrentState = new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "No config is set");
            Next.consume(IConsumer.END_OF_FILE_PACKET_NUMBER);
            return;
        }

        if (!IsConsumerInited) {
            CurrentState = new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "No consumer inited");
            Next.consume(IConsumer.END_OF_FILE_PACKET_NUMBER);
            return;
        }
        if (!IsInputStreamInited) {
            CurrentState = new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "No input stream inited");
            Next.consume(IConsumer.END_OF_FILE_PACKET_NUMBER);
            return;
        }
        try {
            currentBuffer.readedLength = inputStream.read(currentBuffer.buffer, 0, bufferSize);
        } catch (IOException e) {
            CurrentState = RC_READER_FAILED_TO_READ;
            Next.consume(IConsumer.END_OF_FILE_PACKET_NUMBER);
            return;
        }

        while (currentBuffer.readedLength > 0 && CurrentState.isSuccess())
        {
            RC tmp_rc;
            Buffer clone = new Buffer(currentBuffer);
            readedBuffers.put(current_packet_number, clone);
            tmp_rc = Next.consume(current_packet_number);
            // TODO: Make more safe
            if (current_packet_number == IConsumer.MAX_PACKET_NUMBER)
                current_packet_number = -1;
            current_packet_number++;
            if (!tmp_rc.isSuccess()) {
                Next.consume(IConsumer.END_OF_FILE_PACKET_NUMBER);
                CurrentState = tmp_rc;
                return;
            }

            try {
                currentBuffer.readedLength = inputStream.read(currentBuffer.buffer, 0, bufferSize);
            } catch (IOException e) {
                Next.consume(IConsumer.END_OF_FILE_PACKET_NUMBER);
                CurrentState = RC_READER_FAILED_TO_READ;
                return;
            }
        }

        current_packet_number = IConsumer.END_OF_FILE_PACKET_NUMBER;
        Next.consume(current_packet_number);

        return;
    }
}

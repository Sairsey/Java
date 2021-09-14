import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class BufferedReader {
    private File inputFile;
    private FileInputStream inputFileStream;
    private byte buffer[];
    private int bufferSize = 0;
    private int indexInBuffer = 0;
    private int readedLength = 0;

    BufferedReader(String path) throws FileNotFoundException {
        inputFile = new File(path);
        inputFileStream = new FileInputStream(inputFile);
    }

    public void SetBuffer(int newBufferSize) {
        buffer = new byte[newBufferSize];
        this.bufferSize = newBufferSize;
        this.indexInBuffer = 0;
        this.readedLength = 0;
    }

    public void UpdateIfNecessary() throws IOException {
        if (indexInBuffer == readedLength)
        {
            readedLength = inputFileStream.read(buffer, 0, bufferSize);
            indexInBuffer = 0;
        }
    }

    public byte ReadByte() {
        indexInBuffer++;
        return buffer[indexInBuffer - 1];
    }

    public boolean NotEnded() {
        return readedLength > 0;
    }

    public void Close() throws IOException {
        inputFileStream.close();
    }
}

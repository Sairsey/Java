import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class BufferedWriter {
    private File outputFile;
    private FileOutputStream outputFileStream;
    private byte buffer[];
    private int bufferSize = 0;
    private int indexInBuffer = 0;

    BufferedWriter(String path) throws FileNotFoundException {
        outputFile = new File(path);
        outputFileStream = new FileOutputStream(outputFile);
    }

    public void SetBuffer(int newBufferSize) {
        buffer = new byte[newBufferSize];
        this.bufferSize = newBufferSize;
    }

    public void WriteByte(byte data) throws IOException {
        buffer[indexInBuffer] = data;
        indexInBuffer++;
        if (indexInBuffer == bufferSize) {
            outputFileStream.write(buffer);
            indexInBuffer = 0;
        }
    }

    public void Close() throws IOException {
        if (indexInBuffer != 0)
            outputFileStream.write(buffer, 0, indexInBuffer);
        outputFileStream.close();
    }
}

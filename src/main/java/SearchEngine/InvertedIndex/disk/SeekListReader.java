package SearchEngine.InvertedIndex.disk;

import SearchEngine.InvertedIndex.TermReader;

import java.io.*;
import java.util.zip.InflaterInputStream;

/**
 * Created by norman on 18.11.15.
 */
public class SeekListReader {

    public static SeekListEntry readSeekListEntry(DataInput stream) throws IOException {
        int offset = stream.readInt();
        int length = stream.readInt();
        int tokenCount = stream.readInt();
        String token = TermReader.readTerm(stream);
        return new SeekListEntry(token, offset, length, tokenCount);
    }

    public static SeekList readSeekList(DataInput stream) throws IOException {
        SeekList seekList = new SeekList();
        while (true) {
            try {
                seekList.add(readSeekListEntry(stream));
            } catch (EOFException e) {
                break;
            }
        }
        return seekList;
    }

    public static SeekList readSeekListFromFile(DataInput stream, int seekListByteLength) throws IOException {
        byte[] seekListBuffer = new byte[seekListByteLength];
        stream.readFully(seekListBuffer);

        DataInputStream seekListDataInput = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(seekListBuffer)));
        return SeekListReader.readSeekList(seekListDataInput);
    }

}

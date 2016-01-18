package SearchEngine.InvertedIndex.disk;

import SearchEngine.InvertedIndex.TermReader;

import java.io.*;
import java.util.zip.InflaterInputStream;

/**
 * Created by norman on 18.11.15.
 */
public class SeekListReader {

    public static SeekListEntry readSeekListEntry(DataInput stream) throws IOException {
        long offset = stream.readLong();
        int length = stream.readInt();
        int tokenCount = stream.readInt();
        String token = TermReader.readTerm(stream);
        return new SeekListEntry(token, offset, length, tokenCount);
    }

    public static SeekList readSeekList(DataInput stream) throws IOException {
        int seekListLength = stream.readInt();
        SeekList seekList = new SeekList(seekListLength);
        while (true) {
            try {
                seekList.add(readSeekListEntry(stream));
            } catch (EOFException e) {
                break;
            }
        }
        System.out.println(seekList.getLength());
        return seekList;
    }

    public static SeekList readSeekListFromFile(DataInput stream, int seekListByteLength) throws IOException {
        byte[] seekListBuffer = new byte[seekListByteLength];
        stream.readFully(seekListBuffer);

        DataInputStream seekListDataInput = new DataInputStream(
                new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(seekListBuffer))));
        return SeekListReader.readSeekList(seekListDataInput);
    }

}

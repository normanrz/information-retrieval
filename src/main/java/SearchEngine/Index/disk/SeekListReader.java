package SearchEngine.Index.disk;

import SearchEngine.Index.TermReader;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * Created by norman on 18.11.15.
 */
public class SeekListReader {

    public static SeekListEntry readSeekListEntry(DataInputStream stream) throws IOException {
        int offset = stream.readInt();
        int length = stream.readInt();
        int tokenCount = stream.readInt();
        String token = TermReader.readTerm(stream);
        return new SeekListEntry(token, offset, length, tokenCount);
    }

    public static SeekList readSeekList(DataInputStream stream) throws IOException {
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

}

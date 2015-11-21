package SearchEngine.Index.disk;

import SearchEngine.Index.TermReader;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by norman on 18.11.15.
 */
public class SeekListReader {

    public static SeekListEntry readSeekListEntry(DataInputStream stream) throws IOException {
        int offset = stream.readInt();
        int length = stream.readInt();
        String token = TermReader.readTerm(stream);
        return new SeekListEntry(token, offset, length);
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

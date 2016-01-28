package SearchEngine.InvertedIndex.seeklist;

import SearchEngine.InvertedIndex.TermReader;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;

/**
 * Created by norman on 18.11.15.
 */
public class SeekListReader {

    private SeekListReader() {
    }

    public static SeekListEntry readSeekListEntry(DataInput stream) throws IOException {
        long offset = stream.readLong();
        int length = stream.readInt();
        int tokenCount = stream.readInt();
        String token = TermReader.readTerm(stream);
        return new SeekListEntry(token, offset, length, tokenCount);
    }

    public static EntryListSeekList readSeekList(DataInput stream) throws IOException {
        int seekListLength = stream.readInt();
        EntryListSeekList seekList = new EntryListSeekList(seekListLength);
        for (int i = 0; i < seekListLength; i++) {
            try {
                seekList.add(readSeekListEntry(stream));
            } catch (EOFException e) {
                break;
            }
        }
        System.out.println(seekList.getLength());
        return seekList;
    }

    public static EntryListSeekList readSeekListFromFile(DataInput stream, int seekListByteLength) throws IOException {
        return SeekListReader.readSeekList(stream);
    }

}

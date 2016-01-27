package SearchEngine.InvertedIndex.seeklist;

import SearchEngine.InvertedIndex.TermWriter;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by norman on 18.11.15.
 */
public class SeekListWriter {

    private SeekListWriter() {}

    public static void writeSeekListEntry(DataOutput stream, SeekListEntry entry) throws IOException {
        stream.writeLong(entry.getOffset());
        stream.writeInt(entry.getLength());
        stream.writeInt(entry.getTokenCount());
        TermWriter.writeTerm(stream, entry.getToken());
    }

    public static void writeSeekList(DataOutput stream, SeekList list) throws IOException {
        stream.writeInt(list.getLength());
        for (SeekListEntry entry : list) {
            writeSeekListEntry(stream, entry);
        }
    }

}

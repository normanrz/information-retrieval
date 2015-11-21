package SearchEngine.Index.disk;

import SearchEngine.Index.TermWriter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.stream.Stream;

/**
 * Created by norman on 18.11.15.
 */
public class SeekListWriter {

    public static void writeSeekListEntry(DataOutputStream stream, SeekListEntry entry) throws IOException {
        stream.writeInt(entry.getOffset());
        stream.writeInt(entry.getLength());
        TermWriter.writeTerm(stream, entry.getToken());
    }

    public static void writeSeekList(DataOutputStream stream, SeekList list) throws IOException {
        for (SeekListEntry entry : list) {
            writeSeekListEntry(stream, entry);
        }
    }

    public static int seekListByteLength(SeekList list) {
        return seekListByteLength(list.stream().map(SeekListEntry::getToken));
    }

    public static int seekListByteLength(Stream<String> stream) {
        return stream
                .mapToInt(item -> 2 * Integer.BYTES + TermWriter.termByteLength(item))
                .sum();
    }
}

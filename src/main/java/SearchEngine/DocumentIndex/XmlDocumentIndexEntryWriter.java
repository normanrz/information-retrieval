package SearchEngine.DocumentIndex;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Created by norman on 01.12.15.
 */
public class XmlDocumentIndexEntryWriter {

    public static void writeDocumentIndexEntry(
            DataOutput stream, XmlDocumentIndexEntry entry, List<String> fileNames) throws IOException {
        stream.writeShort(fileNames.indexOf(entry.getFilename()));
        stream.writeInt(entry.getDocId());
        stream.writeInt(entry.getDocumentTokenCount());
        stream.writeLong(entry.getOffset());
    }

    public static void writeDocumentIndexEntries(
            DataOutput stream, Collection<XmlDocumentIndexEntry> entries, List<String> fileNames) throws IOException {
        for (XmlDocumentIndexEntry entry : entries) {
            writeDocumentIndexEntry(stream, entry, fileNames);
        }
    }

}

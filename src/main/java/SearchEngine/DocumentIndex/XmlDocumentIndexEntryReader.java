package SearchEngine.DocumentIndex;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by norman on 01.12.15.
 */
public class XmlDocumentIndexEntryReader {

    public static XmlDocumentIndexEntry readDocumentIndexEntry(
            DataInput stream, List<String> fileNames) throws IOException {
        short filenameIndex = stream.readShort();
        int docId = stream.readInt();
        int documentTokenCount = stream.readInt();
        long offset = stream.readLong();

        return new XmlDocumentIndexEntry(
                docId, documentTokenCount, fileNames.get(filenameIndex), offset);
    }

    public static List<XmlDocumentIndexEntry> readDocumentIndexEntries(
            DataInput stream, List<String> fileNames) throws IOException {
        List<XmlDocumentIndexEntry> entries = new ArrayList<>();
        while (true) {
            try {
                entries.add(XmlDocumentIndexEntryReader.readDocumentIndexEntry(stream, fileNames));
            } catch (EOFException e) {
                break;
            }
        }
        return entries;
    }
}

package SearchEngine.DocumentIndex;

/**
 * Created by norman on 30.11.15.
 */
class XmlDocumentIndexEntry implements Comparable<XmlDocumentIndexEntry> {

    private final int docId;
    private final int documentTokenCount;
    private final String filename;
    private final long offset;

    public XmlDocumentIndexEntry(int docId, int documentTokenCount, String filename, long offset) {
        this.docId = docId;
        this.documentTokenCount = documentTokenCount;
        this.filename = filename;
        this.offset = offset;
    }

    public int getDocId() {
        return docId;
    }

    public int getDocumentTokenCount() {
        return documentTokenCount;
    }

    public String getFilename() {
        return filename;
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public int compareTo(XmlDocumentIndexEntry o) {
        return Integer.compare(getDocId(), o.getDocId());
    }
}

package SearchEngine.DocumentIndex;

/**
 * Created by norman on 30.11.15.
 */
public class XmlDocumentIndexEntry implements Comparable<XmlDocumentIndexEntry> {

    private final int docId;
    private final int documentTokenCount;
    private final int titleTokenCount;
    private final String filename;
    private final long offset;
    private final double pageRank;

    XmlDocumentIndexEntry(
            int docId, int documentTokenCount, int titleTokenCount, String filename, long offset, double pageRank) {
        this.docId = docId;
        this.documentTokenCount = documentTokenCount;
        this.titleTokenCount = titleTokenCount;
        this.filename = filename;
        this.offset = offset;
        this.pageRank = pageRank;
    }

    public int getDocId() {
        return docId;
    }

    public int getDocumentTokenCount() {
        return documentTokenCount;
    }

    public int getTitleTokenCount() {
        return titleTokenCount;
    }

    public String getFilename() {
        return filename;
    }

    public long getOffset() {
        return offset;
    }

    public double getPageRank() {
        return pageRank;
    }

    @Override
    public int compareTo(XmlDocumentIndexEntry o) {
        return Integer.compare(getDocId(), o.getDocId());
    }
}

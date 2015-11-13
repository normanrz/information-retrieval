package SearchEngine;

/**
 * Created by norman on 23.10.15.
 */
public class PatentDocument {
    public final int docId;
    public final String title;
    public final String abstractText;

    public PatentDocument(String docId, String title, String abstractText) {
        this(Integer.parseInt(docId), title, abstractText);
    }
    public PatentDocument(int docId, String title, String abstractText) {
        this.docId = docId;
        this.title = title;
        this.abstractText = abstractText;
    }

    @Override
    public String toString() {
        return String.format("%08d\t%s", docId, title.substring(0, Math.min(100, title.length())));
    }
}

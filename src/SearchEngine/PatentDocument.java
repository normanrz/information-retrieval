package SearchEngine;

/**
 * Created by norman on 23.10.15.
 */
public class PatentDocument {
    public final String docNumber;
    public final String title;
    public final String abstractText;

    public PatentDocument(String docNumber, String title, String abstractText) {
        this.docNumber = docNumber;
        this.title = title;
        this.abstractText = abstractText;
    }

    @Override
    public String toString() {
        return docNumber + " " + title.substring(0, Math.min(100, title.length()));
    }
}

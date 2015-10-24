package SearchEngine;

/**
 * Created by norman on 23.10.15.
 */
public class DocNumberAndAbstract {
    public final String docNumber;
    public final String abstractText;

    public DocNumberAndAbstract(String docNumber, String abstractText) {
        this.docNumber = docNumber;
        this.abstractText = abstractText;
    }

    @Override
    public String toString() {
        return docNumber + " " + abstractText.substring(0, Math.min(100, abstractText.length()));
    }
}

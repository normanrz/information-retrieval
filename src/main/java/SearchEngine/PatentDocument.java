package SearchEngine;

import SearchEngine.Import.PatentDocumentPreprocessor;

import java.util.stream.Stream;

/**
 * Created by norman on 23.10.15.
 */
public class PatentDocument {
    private final int docId;
    private final String title;
    private final String abstractText;

    public PatentDocument(String docId, String title, String abstractText) {
        this(Integer.parseInt(docId), title, abstractText);
    }

    public PatentDocument(int docId, String title, String abstractText) {
        this.docId = docId;
        this.title = title;
        this.abstractText = abstractText;
    }

    public int getDocId() {
        return docId;
    }

    public String getTitle() {
        return title;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public String getTokenizableDocument() {
        return (getTitle() + " " + getAbstractText()).toLowerCase();
    }

    public Stream<String> getStemmedTitle() { return PatentDocumentPreprocessor.preprocess(getTitle()); }

    public Stream<String> getStemmedAbstract() { return PatentDocumentPreprocessor.preprocess(getAbstractText()); }

    public Stream<String> getStemmedTokens() {
        return PatentDocumentPreprocessor.preprocess(getTokenizableDocument());
    }

    @Override
    public String toString() {
        return String.format("%08d\t%s", docId, title.substring(0, Math.min(100, title.length())));
    }
}

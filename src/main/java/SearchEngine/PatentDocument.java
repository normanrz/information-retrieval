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
    private final String descriptionText;
    private final String claimsText;


    public PatentDocument(int docId, String title, String abstractText, String descriptionText, String claimsText) {
        this.docId = docId;
        this.title = title;
        this.abstractText = abstractText;
        this.descriptionText = descriptionText;
        this.claimsText = claimsText;
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

    public String getDescriptionText() {
        return descriptionText;
    }

    public String getClaimsText() {
        return claimsText;
    }

    public String getBody() {
        return String.join(" ",
                getAbstractText(),
                getDescriptionText(),
                getClaimsText());
    }

    public String getFulltext() {
        return String.join(" ",
                getTitle(),
                getBody()
        );
    }

    public Stream<String> getStemmedTokens() {
        return PatentDocumentPreprocessor.preprocess(getFulltext());
    }

    @Override
    public String toString() {
        return String.format("%08d\t%s", docId, title.substring(0, Math.min(100, title.length())));
    }
}

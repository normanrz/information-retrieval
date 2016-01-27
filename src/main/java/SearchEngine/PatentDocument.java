package SearchEngine;

import SearchEngine.Import.PatentDocumentPreprocessor;
import org.apache.commons.collections.primitives.IntList;

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
    private final IntList citations;


    public PatentDocument(int docId, String title, String abstractText, String descriptionText, String claimsText, IntList citations) {
        this.docId = docId;
        this.title = title;
        this.abstractText = abstractText;
        this.descriptionText = descriptionText;
        this.claimsText = claimsText;
        this.citations = citations;
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

    public int[] getCitations() {
        return citations.toArray();
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

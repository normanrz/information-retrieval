package SearchEngine.DocumentIndex;

import SearchEngine.PatentDocument;

import java.util.List;
import java.util.Optional;

/**
 * Created by norman on 01.12.15.
 */
public interface DocumentIndex {

    int getDocumentTokenCount(int docId);

    List<String> getPatentDocumentTokens(int docId);

    Optional<PatentDocument> getPatentDocument(int docId);

    Optional<String> getPatentDocumentTitle(int docId);

}

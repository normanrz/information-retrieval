package SearchEngine.Query;

import java.util.List;

/**
 * Created by norman on 26.11.15.
 */
public class DocumentSnippetsResult {

    private final int docId;
    private final List<String> snippets;

    public DocumentSnippetsResult(int docId, List<String> snippets) {
        this.docId = docId;
        this.snippets = snippets;
    }

    public int getDocId() {
        return docId;
    }

    public List<String> getSnippets() {
        return snippets;
    }

}

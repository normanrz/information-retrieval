package SearchEngine.Query;

import SearchEngine.PatentDocument;

import java.util.List;
import java.util.stream.Stream;

/**
 * Created by norman on 01.12.15.
 */
public class SnippetSearchResult extends SearchResult {

    private final PatentDocument patentDocument;
    private final List<String> snippets;

    public SnippetSearchResult(SearchResult result, PatentDocument patentDocument, List<String> snippets) {
        this(result.getDocId(), result.getRank(), patentDocument, snippets);
    }

    public SnippetSearchResult(int docId, double rank, PatentDocument patentDocument, List<String> snippets) {
        super(docId, rank);
        this.patentDocument = patentDocument;
        this.snippets = snippets;
    }

    public PatentDocument getPatentDocument() {
        return patentDocument;
    }

    public String getTitle() {
        return getPatentDocument().getTitle();
    }

    public List<String> getSnippets() {
        return snippets;
    }

    public String getText() {
        return getTitle() + "\n" + String.join("\n", getSnippets());
    }

    public Stream<String> getTokens() {
        return getPatentDocument().getStemmedTokens();
    }

    public String getCombinedSnippet() {
        return String.join(" ... ", getSnippets());
    }

    @Override
    public String toString() {
        return String.format("%s:\t%s\n%s", super.toString(), getTitle(), getCombinedSnippet());
    }
}

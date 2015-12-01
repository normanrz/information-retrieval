package SearchEngine.Query;

import java.util.List;

/**
 * Created by norman on 12.11.15.
 */
public class PostingSearchResult implements Comparable<PostingSearchResult> {

    protected final int docId;
    protected final double rank;

    protected String snippet = "";

    public PostingSearchResult(int docId, double rank) {
        this.docId = docId;
        this.rank = rank;
    }

    public int getDocId() {
        return docId;
    }

    public double getRank() {
        return rank;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getSnippet() {
        return snippet;
    }

    @Override
    public int compareTo(PostingSearchResult o) {
        return -Double.compare(rank, o.rank);
    }

    public static int[] getDocIds(List<PostingSearchResult> results) {
        return results.stream().mapToInt(PostingSearchResult::getDocId).toArray();
    }
}

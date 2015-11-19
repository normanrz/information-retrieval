package SearchEngine;

/**
 * Created by norman on 12.11.15.
 */
public class PostingSearchResult implements Comparable<PostingSearchResult> {

    protected final int docId;
    protected final double rank;

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

    @Override
    public int compareTo(PostingSearchResult o) {
        return -Double.compare(rank, o.rank);
    }
}

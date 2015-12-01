package SearchEngine.Query;

import java.util.List;

/**
 * Created by norman on 12.11.15.
 */
public class SearchResult implements Comparable<SearchResult> {

    protected final int docId;
    protected final double rank;

    public SearchResult(int docId, double rank) {
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
    public int compareTo(SearchResult o) {
        return -Double.compare(rank, o.rank);
    }

    public static int[] getDocIds(List<? extends SearchResult> results) {
        return results.stream().mapToInt(SearchResult::getDocId).toArray();
    }

    @Override
    public String toString() {
        return String.format("%08d\t(%8f)", getDocId(), getRank());
    }
}

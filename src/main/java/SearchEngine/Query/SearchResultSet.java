package SearchEngine.Query;

import java.util.List;

/**
 * Created by norman on 01.12.15.
 */
public class SearchResultSet {

    private final int[] docIds;
    private final List<String> queryTokens;

    public SearchResultSet(int[] docIds, List<String> queryTokens) {
        this.docIds = docIds;
        this.queryTokens = queryTokens;
    }

    public int[] getDocIds() {
        return docIds;
    }

    public List<String> getQueryTokens() {
        return queryTokens;
    }
}

package SearchEngine;

import SearchEngine.Index.PostingIndex;

import java.util.List;

/**
 * Created by norman on 05.11.15.
 */
public class SearchPostingIndex {

    private final PostingIndex index;

    public SearchPostingIndex(PostingIndex index) {
        this.index = index;
    }

    public List<PatentDocument> search(String query) {

    }


    public List<PatentDocument> phraseQuerySearch(String query) {
        // Split tokens

        // searchAnd(tokens)

        // Validate positions

        // for all tokens...
    }



    private List<PatentDocument> prefixSearchToken(String token) {

    }

    private List<PatentDocument> searchToken(String token) {

    }



    private List<PatentDocument> searchOr(String... tokens) {
        // Find all token matches

        // Distinct union
    }

    private List<PatentDocument> searchAnd(String... tokens) {
        // Find first token

        // Find second token in same documents

        // continue for all
    }

}

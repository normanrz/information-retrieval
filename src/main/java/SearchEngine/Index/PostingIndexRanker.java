package SearchEngine.Index;

import SearchEngine.Importer.PatentDocumentPreprocessor;
import SearchEngine.Posting;
import SearchEngine.PostingSearchResult;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by norman on 13.11.15.
 */
public class PostingIndexRanker {

    private final PostingIndex index;

    public PostingIndexRanker(PostingIndex index) {
        this.index = index;
    }

    public List<PostingSearchResult> rank(String query, int[] docIds, int mu) {
        List<String> tokens = PatentDocumentPreprocessor.tokenizeAsStrings(query);
        tokens = PatentDocumentPreprocessor.removeAsteriskTokens(tokens);

        // Preprocess tokens
        tokens = PatentDocumentPreprocessor.lowerCaseTokens(tokens);
        tokens = PatentDocumentPreprocessor.removeStopwords(tokens);
        tokens = PatentDocumentPreprocessor.stemmedTokens(tokens);

        final List<String> finalTokens = tokens;

        return Arrays.stream(docIds)
                .mapToObj(docId -> new PostingSearchResult(docId, queryLikelihood(finalTokens, docId, mu)))
                .sorted(PostingSearchResult::compareTo)
                .collect(Collectors.toList());
    }


    public double queryLikelihood(List<String> tokens, int docId, int mu) {
        return tokens.stream()
                .mapToDouble(token ->  (index.documentTokenFrequency(token, docId) +
                                mu * ((double)index.collectionTokenFrequency(token) / (double)index.collectionTokenCount())) /
                                (index.documentTokenCount(docId) + mu)
                )
                .map(Math::log)
                .sum();

    }
}

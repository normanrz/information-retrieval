package SearchEngine.Index.Query;

import SearchEngine.Importer.PatentDocumentPreprocessor;
import SearchEngine.Index.DocumentIndex;
import SearchEngine.Index.PostingIndex;
import SearchEngine.PostingSearchResult;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by norman on 13.11.15.
 */
public class PostingIndexRanker {

    private final PostingIndex index;

    public PostingIndexRanker(PostingIndex index) {
        this.index = index;
    }


    public List<PostingSearchResult> rankWithRelevanceFeedback(List<String> queryTokens, int[] docIds, List<PostingSearchResult> topRankedDocs, DocumentIndex documentIndex) {

        // TODO
        int mu = 2000;
        int numberOfQueryTokens = 20;

        List<String> topCollectionTokens = topRankedDocs.stream()
                .flatMap(result -> documentIndex.getPatentDocumentTokens(result.getDocId()).stream())
                .distinct()
                .collect(Collectors.toList());


        Map<String, Double> relevanceModelProbabilities = topCollectionTokens.stream()
                .collect(Collectors.toMap(Function.identity(), token ->
                                topRankedDocs.stream()
                                        .mapToDouble(doc ->
                                                tokenProbability(token, doc.getDocId(), mu) *
                                                        queryTokens.stream()
                                                                .mapToDouble(queryToken -> tokenProbability(queryToken, doc.getDocId(), mu))
                                                                .reduce(1, (a, b) -> a * b))
                                        .sum()
                ));

        double relevanceModelNormalizer =
                relevanceModelProbabilities.values().stream().mapToDouble(a -> a).sum();


        List<String> highProbabilityQueryTokens = relevanceModelProbabilities.entrySet().stream()
                .sorted(Comparator.comparingDouble(entry -> -entry.getValue()))
                .limit(numberOfQueryTokens)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return Arrays.stream(docIds)
                .mapToObj(docId -> new PostingSearchResult(docId,
                        highProbabilityQueryTokens.stream()
                                .mapToDouble(token ->
                                        relevanceModelProbabilities.get(token) /
                                                relevanceModelNormalizer *
                                                Math.log(tokenProbability(token, docId, mu)))
                                .sum()
                ))
                .sorted(PostingSearchResult::compareTo)
                .collect(Collectors.toList());


    }

    public List<PostingSearchResult> rank(List<String> queryTokens, int[] docIds, int mu) {

        return Arrays.stream(docIds)
                .mapToObj(docId -> new PostingSearchResult(docId, queryLikelihood(queryTokens, docId, mu)))
                .sorted(PostingSearchResult::compareTo)
                .collect(Collectors.toList());
    }


    public double queryLikelihood(List<String> tokens, int docId, int mu) {
        return tokens.stream()
                .mapToDouble(token -> tokenProbability(token, docId, mu))
                .map(Math::log)
                .sum();

    }

    private double tokenProbability(String token, int docId, int mu) {
        return (index.documentTokenCount(token, docId) +
                mu * ((double) index.collectionTokenCount(token) / (double) index.collectionTokenCount())) /
                (index.documentTokenCount(docId) + mu);
    }
}

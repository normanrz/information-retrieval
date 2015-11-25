package SearchEngine.Query;

import SearchEngine.Index.DocumentIndex;
import SearchEngine.Index.PostingIndex;

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
    private final DocumentIndex documentIndex;
    private int mu = 2000;
    private int numberOfQueryTokens = 10;


    public PostingIndexRanker(PostingIndex index, DocumentIndex documentIndex) {
        this.index = index;
        this.documentIndex = documentIndex;
    }

    public void setMu(int mu) {
        this.mu = mu;
    }

    public void setNumberOfQueryTokens(int numberOfQueryTokens) {
        this.numberOfQueryTokens = numberOfQueryTokens;
    }


    public Map<String, Double> pseudoRelevanceModel(List<String> queryTokens, int[] topRankedDocIds) {
        List<String> topCollectionTokens = Arrays.stream(topRankedDocIds)
                .mapToObj(documentIndex::getPatentDocumentTokens)
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, Double> queryTokenProbabilities = Arrays.stream(topRankedDocIds)
                .boxed()
                .collect(Collectors.toMap(
                        Function.identity(),
                        docId -> queryTokens.stream()
                                .mapToDouble(queryToken -> tokenProbability(queryToken, docId))
                                .reduce(1, (a, b) -> a * b)));

        Map<String, Double> relevanceModelProbabilities = topCollectionTokens.stream()
                .collect(Collectors.toMap(Function.identity(), token ->
                                Arrays.stream(topRankedDocIds)
                                        .mapToDouble(docId ->
                                                tokenProbability(token, docId) * queryTokenProbabilities.get(docId))
                                        .sum()
                ));

//        double relevanceModelNormalizer =
//                relevanceModelProbabilities.values().stream().mapToDouble(a -> a).sum();

        return relevanceModelProbabilities.entrySet().stream()
                .sorted(Comparator.comparingDouble(entry -> -entry.getValue()))
                .limit(numberOfQueryTokens)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<PostingSearchResult> rankWithRelevanceModel(int[] docIds, Map<String, Double> relevanceModel) {

        return Arrays.stream(docIds)
                .mapToObj(docId -> new PostingSearchResult(docId,
                        relevanceModel.keySet().stream()
                                .mapToDouble(token ->
                                        relevanceModel.get(token) * Math.log(tokenProbability(token, docId)))
                                .sum()
                ))
                .sorted(PostingSearchResult::compareTo)
                .collect(Collectors.toList());
    }

    public List<PostingSearchResult> rank(List<String> queryTokens, int[] docIds) {

        return Arrays.stream(docIds)
                .mapToObj(docId -> new PostingSearchResult(docId, queryLikelihood(queryTokens, docId)))
                .sorted(PostingSearchResult::compareTo)
                .collect(Collectors.toList());
    }


    public double queryLikelihood(List<String> tokens, int docId) {
        return tokens.stream()
                .mapToDouble(token -> Math.log(tokenProbability(token, docId)))
                .sum();

    }

    private double tokenProbability(String token, int docId) {
        return (index.documentTokenCount(token, docId) +
                mu * ((double) index.collectionTokenCount(token) / (double) index.collectionTokenCount())) /
                (index.documentTokenCount(docId) + mu);
    }

    public static String getQueryFromRelevanceModel(Map<String, Double> relevanceModel) {
        return String.join(" OR ", relevanceModel.keySet());
    }
}

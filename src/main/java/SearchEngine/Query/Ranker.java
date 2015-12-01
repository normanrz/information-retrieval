package SearchEngine.Query;

import SearchEngine.DocumentIndex.XmlDocumentIndex;
import SearchEngine.Importer.PatentDocumentPreprocessor;
import SearchEngine.InvertedIndex.InvertedIndex;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by norman on 13.11.15.
 */
public class Ranker {

    private final InvertedIndex index;
    private final XmlDocumentIndex documentIndex;
    private int mu = 2000;
    private int numberOfQueryTokens = 10;


    public Ranker(InvertedIndex index, XmlDocumentIndex documentIndex) {
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
        List<String> tokens = Arrays.stream(topRankedDocIds)
                .mapToObj(documentIndex::getPatentDocumentTokens)
                .flatMap(List::stream)
                .distinct()
                .filter(PatentDocumentPreprocessor::isNoStopword)
                .collect(Collectors.toList());
        return pseudoRelevanceModel(queryTokens, topRankedDocIds, tokens);
    }


    public Map<String, Double> pseudoRelevanceModel(List<String> queryTokens, List<DocumentSnippetsResult> documentSnippetsResults) {

        int[] topRankedDocIds = documentSnippetsResults.stream().mapToInt(DocumentSnippetsResult::getDocId).toArray();
        List<String> stemmedTokens = documentSnippetsResults.stream()
                .flatMap(documentSnippetsResult -> documentSnippetsResult.getSnippets().stream())
                .flatMap(snippet -> PatentDocumentPreprocessor.tokenize(snippet).stream())
                .map(String::toLowerCase)
                .map(PatentDocumentPreprocessor::stem)
                .distinct()
                .filter(PatentDocumentPreprocessor::isNoStopword)
                .collect(Collectors.toList());

        return pseudoRelevanceModel(queryTokens, topRankedDocIds, stemmedTokens);
    }

    public Map<String, Double> pseudoRelevanceModel(List<String> queryTokens, int[] topRankedDocIds, List<String> tokens) {

        Map<Integer, Double> queryTokenProbabilities = Arrays.stream(topRankedDocIds)
                .boxed()
                .collect(Collectors.toMap(
                        Function.identity(),
                        docId -> queryTokens.stream()
                                .mapToDouble(queryToken -> tokenProbability(queryToken, docId))
                                .reduce(1, (a, b) -> a * b)));

        Map<String, Double> relevanceModelProbabilities = tokens.stream()
                .collect(Collectors.toMap(Function.identity(), token ->
                                Arrays.stream(topRankedDocIds)
                                        .mapToDouble(docId ->
                                                tokenProbability(token, docId) * queryTokenProbabilities.get(docId))
                                        .sum()
                ));

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
                (documentIndex.getDocumentTokenCount(docId) + mu);
    }

    public static String getQueryFromRelevanceModel(Map<String, Double> relevanceModel) {
        return String.join(" OR ", relevanceModel.keySet());
    }

    public static String expandQueryFromRelevanceModel(Map<String, Double> relevanceModel, List<String> queryTokens) {
        List<String> expandedQueryTokens = Stream.of(relevanceModel.keySet(), queryTokens)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
        return String.join(" OR ", expandedQueryTokens);
    }
}

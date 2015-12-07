package SearchEngine.Query;

import SearchEngine.DocumentIndex.XmlDocumentIndex;
import SearchEngine.Import.PatentDocumentPreprocessor;
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
    private int mu = 1000;
    private int numberOfQueryTokens = 10;
    private double titleTokenFactor = 0.7;
    private double docTokenFactor = 0.3;


    public Ranker(InvertedIndex index, XmlDocumentIndex documentIndex) {
        this.index = index;
        this.documentIndex = documentIndex;
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


    public Map<String, Double> pseudoRelevanceModel(List<String> queryTokens, List<SnippetSearchResult> snippetSearchResults) {

        int[] topRankedDocIds = SearchResult.getDocIds(snippetSearchResults);
        List<String> stemmedTokens = snippetSearchResults.stream()
                .flatMap(SnippetSearchResult::getTokens)
                .distinct()
                .collect(Collectors.toList());

        return pseudoRelevanceModel(queryTokens, topRankedDocIds, stemmedTokens);
    }

    public Map<String, Double> pseudoRelevanceModel(List<String> queryTokens, int[] topRankedDocIds, List<String> tokens) {

        Map<Integer, Double> queryTokenProbabilities = Arrays.stream(topRankedDocIds)
                .boxed()
                .collect(Collectors.toMap(
                        Function.identity(),
                        docId -> queryTokens.stream()
                                .mapToDouble(queryToken -> docTokenProbability(queryToken, docId))
                                .reduce(1, (a, b) -> a * b)));

        Map<String, Double> relevanceModelProbabilities = tokens.stream()
                .collect(Collectors.toMap(Function.identity(), token ->
                                Arrays.stream(topRankedDocIds)
                                        .mapToDouble(docId ->
                                                docTokenProbability(token, docId) * queryTokenProbabilities.get(docId))
                                        .sum()
                ));

        return relevanceModelProbabilities.entrySet().stream()
                .sorted(Comparator.comparingDouble(entry -> -entry.getValue()))
                .limit(numberOfQueryTokens)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<SearchResult> rankWithRelevanceModel(int[] docIds, Map<String, Double> relevanceModel) {

        return Arrays.stream(docIds)
                .mapToObj(docId -> new SearchResult(docId,
                        relevanceModel.keySet().stream()
                                .mapToDouble(token ->
                                        relevanceModel.get(token) * queryLikelihood(token, docId))
                                .sum()
                ))
                .sorted(SearchResult::compareTo)
                .collect(Collectors.toList());
    }


    public Stream<SearchResult> rank(SearchResultSet searchResultSet) {
        return rank(searchResultSet.getQueryTokens(), searchResultSet.getDocIds());
    }

    public Stream<SearchResult> rank(List<String> queryTokens, int[] docIds) {

        return Arrays.stream(docIds)
                .mapToObj(docId -> new SearchResult(docId, queryLikelihood(queryTokens, docId)))
                .sorted(SearchResult::compareTo);
    }


    public double queryLikelihood(List<String> queryTokens, int docId) {
        return queryTokens.stream()
                .mapToDouble(queryToken -> queryLikelihood(queryToken, docId))
                .sum();
    }

    public double queryLikelihood(String token, int docId) {
        return Math.log(titleTokenFactor * titleTokenProbability(token, docId) +
                docTokenFactor * docTokenProbability(token, docId));
    }

    private double docTokenProbability(String token, int docId) {
        return (index.getDocumentTokenCount(token, docId) +
                mu * ((double) index.getCollectionTokenCount(token) / (double) index.getCollectionTokenCount())) /
                (documentIndex.getDocumentTokenCount(docId) + mu);
    }

    private double titleTokenProbability(String token, int docId) {

        int docTitleTokenCount = documentIndex.getDocumentTitleTokenCount(docId);
        return (5 * index.getDocumentTitleTokenCount(token, docTitleTokenCount, docId) +
                mu * ((double) index.getCollectionTokenCount(token) / (double) index.getCollectionTokenCount())) /
                (docTitleTokenCount + mu);

    }

    public static List<String> expandQueryFromRelevanceModel(Map<String, Double> relevanceModel, List<String> queryTokens) {
        return Stream.of(relevanceModel.keySet(), queryTokens)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }
}

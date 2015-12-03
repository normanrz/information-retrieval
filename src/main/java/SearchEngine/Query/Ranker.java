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
    private double titleTokenWeight = 10;


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

    public List<SearchResult> rankWithRelevanceModel(int[] docIds, Map<String, Double> relevanceModel) {

        return Arrays.stream(docIds)
                .mapToObj(docId -> new SearchResult(docId,
                        relevanceModel.keySet().stream()
                                .mapToDouble(token ->
                                        relevanceModel.get(token) * Math.log(tokenProbability(token, docId)))
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


    public double queryLikelihood(List<String> tokens, int docId) {
//        System.out.println(String.format("%.20f %.20f %.20f\t%s\t%s",
//                tokens.stream()
//                        .mapToDouble(token -> tokenProbability(token, docId))
//                        .reduce(1, (a, b) -> a * b),
//                tokens.stream()
//                        .mapToDouble(token -> titleTokenProbability(token, docId))
//                        .reduce(1, (a, b) -> a * b),
//                tokens.stream()
//                        .mapToDouble(token -> 0.7 * titleTokenProbability(token, docId) + 0.3 * tokenProbability(token, docId))
//                        .reduce(1, (a, b) -> a * b),
//                String.join("+", tokens),
//                documentIndex.getPatentDocumentTitle(docId).get()));

        return tokens.stream()
                .mapToDouble(token -> Math.log(0.7 * titleTokenProbability(token, docId) + 0.3 * tokenProbability(token, docId)))
                .sum();
    }

    private double tokenProbability(String token, int docId) {
        return (index.getDocumentTokenCount(token, docId) +
                mu * ((double) index.getCollectionTokenCount(token) / (double) index.getCollectionTokenCount())) /
                (documentIndex.getDocumentTokenCount(docId) + mu);
    }

    private double titleTokenProbability(String token, int docId) {

        int docTitleTokenCount = documentIndex.getDocumentTitleTokenCount(docId);
//        System.out.println(String.format("%d\t%f\t%f\t%s\t%s",
//                5 * index.getDocumentTitleTokenCount(token, docTitleTokenCount, docId),
//                (index.getDocumentTitleTokenCount(token, docTitleTokenCount, docId) +
//                        mu * ((double) index.getCollectionTokenCount(token) / (double) index.getCollectionTokenCount())) /
//                        (docTitleTokenCount + mu),
//                mu * ((double) index.getCollectionTokenCount(token) / (double) index.getCollectionTokenCount()),
//                token,
//                documentIndex.getPatentDocumentTitle(docId).get()));

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

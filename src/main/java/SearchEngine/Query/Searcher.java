package SearchEngine.Query;

import SearchEngine.Importer.PatentDocumentPreprocessor;
import SearchEngine.InvertedIndex.DocumentPostings;
import SearchEngine.InvertedIndex.InvertedIndex;
import SearchEngine.InvertedIndex.Posting;
import SearchEngine.utils.IntArrayUtils;
import SearchEngine.utils.LevenshteinDistance;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Searcher {

    private final int[] emptyArray = new int[0];
    private final InvertedIndex index;

    private List<String> stemmedQueryTokens;
    private boolean shouldCorrectSpelling = false;

    public Searcher(InvertedIndex index) {
        this.index = index;
    }

    public void setShouldCorrectSpelling(boolean state) {
        shouldCorrectSpelling = state;
    }


    public List<String> getStemmedQueryTokens() {
        return stemmedQueryTokens;
    }

    public int[] search(String query) {

        // Tokenize query
        List<String> tokens = PatentDocumentPreprocessor.tokenize(query);
        tokens = PatentDocumentPreprocessor.mergeAsteriskTokens(tokens);

        // Detect SearchType
        SearchType searchType;
        if (tokens.contains("AND")) {
            searchType = SearchType.AND;
        } else if (tokens.contains("OR")) {
            searchType = SearchType.OR;
        } else if (tokens.contains("NOT")) {
            searchType = SearchType.NOT;
        } else {
            searchType = SearchType.PHRASE;
        }

        // Preprocess tokens
        tokens = PatentDocumentPreprocessor.lowerCaseTokens(tokens);
        tokens = PatentDocumentPreprocessor.removeStopwords(tokens);

        stemmedQueryTokens = PatentDocumentPreprocessor.stemmedTokens(tokens);

        // Execute search
        switch (searchType) {
            case AND:
                return searchAnd(tokens);
            case OR:
                return searchOr(tokens);
            case NOT:
                return searchNot(tokens);
            case PHRASE:
                return searchPhrase(tokens);
            default:
                return new int[]{};
        }
    }


    private int[] searchPhrase(List<String> tokens) {

        if (tokens.size() == 0) {
            return emptyArray;
        } else {
            int tokenCount = 0;
            List<Posting> results = null;
            for (String token : tokens) {
                if (results == null) {
                    // First token
                    results = searchToken(token);
                } else {

                    // Subsequent tokens
                    final int finalTokenCount = tokenCount;
                    final int[] docIds = postingsDocIds(results);
                    List<Posting> tokenResults = searchTokenInDocs(token, docIds);

                    // Shrink result set based on subsequent token matches
                    results = results.stream()
                            .filter(posting ->
                                            tokenResults.stream()
                                                    // Current token is in same document
                                                    .filter(posting1 -> posting1.docId() == posting.docId())
                                                            // Current token position matches expected position
                                                    .anyMatch(posting1 -> posting1.pos() == posting.pos() + finalTokenCount)
                            )
                            .collect(Collectors.toList());
                }
                tokenCount += 1;
            }
            return postingsDocIds(results);
        }
    }

    private Optional<String> findCorrectSpelledToken(String originalToken) {
        final int tokenLengthTolerance = 1;
        final double distanceThreshold = 0.4;

        return index.getTokensByPrefix(originalToken.substring(0, 1))
                .filter(candidateToken ->
                        originalToken.length() - tokenLengthTolerance < candidateToken.length() &&
                                candidateToken.length() < originalToken.length() + tokenLengthTolerance)
                .collect(Collectors.toMap(Function.identity(),
                        candidateToken -> LevenshteinDistance.distance(candidateToken, originalToken)))
                .entrySet().stream()
                .filter(entry -> entry.getValue() < distanceThreshold)
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .findFirst()
                .map(Map.Entry::getKey);
    }


    private List<Posting> searchToken(String token) {
        Stream<DocumentPostings> results;
        if (token.endsWith("*")) {
            // Prefix search (no stemming)
            token = token.substring(0, token.length() - 1);
            results = index.getByPrefix(token);

        } else {
            // Regular search with stemming
            String stemmedToken = PatentDocumentPreprocessor.stem(token);
            results = index.get(stemmedToken);

            List<DocumentPostings> tmpResults = results.collect(Collectors.toList());
            if (shouldCorrectSpelling && tmpResults.size() == 0) {
                Optional<String> correctedTokenOptional = findCorrectSpelledToken(stemmedToken);
                if (correctedTokenOptional.isPresent()) {
                    stemmedQueryTokens.set(stemmedQueryTokens.indexOf(stemmedToken), correctedTokenOptional.get());
                    results = index.get(correctedTokenOptional.get());
                } else {
                    results = Stream.empty();
                }
            } else {
                results = tmpResults.stream();
            }
        }
        return results
                .flatMap(documentPostings -> documentPostings.toPostings().stream())
                .collect(Collectors.toList());
    }

    private List<Posting> searchTokenInDocs(String token, int[] docIds) {
        Stream<DocumentPostings> results;
        if (token.endsWith("*")) {
            // Prefix search (no stemming)
            token = token.substring(0, token.length() - 1);
            results = index.getByPrefixInDocs(token, docIds);

        } else {
            // Regular search with stemming
            String stemmedToken = PatentDocumentPreprocessor.stem(token);
            results = index.getInDocs(stemmedToken, docIds);
        }
        return results
                .flatMap(documentPostings -> documentPostings.toPostings().stream())
                .collect(Collectors.toList());

    }


    private int[] searchOr(List<String> tokens) {
        return postingsDocIds(tokens.stream()
                .map(this::searchToken)
                .flatMap(postings -> postings.stream())
                .collect(Collectors.toList()));
    }

    private int[] searchAnd(List<String> tokens) {
        if (tokens.isEmpty()) {
            return emptyArray;
        } else {
            int[] results = null;

            for (String token : tokens) {
                if (results == null) {
                    // First token
                    results = postingsDocIds(searchToken(token));
                } else {
                    // Subsequent tokens in intersecting documents
                    results = postingsDocIds(searchTokenInDocs(token, results));
                }

                if (results.length == 0) {
                    break;
                }
            }
            return results;
        }
    }

    private int[] searchNot(List<String> tokens) {
        if (tokens.size() != 2) {
            return emptyArray;
        } else {

            // Remove intersecting documents
            int[] docIds0 = postingsDocIds(searchToken(tokens.get(0)));
            int[] docIds1 = postingsDocIds(searchTokenInDocs(tokens.get(1), docIds0));

            return Arrays.stream(docIds0)
                    .filter(docId -> !IntArrayUtils.intArrayContains(docIds1, docId))
                    .toArray();
        }
    }


    private int[] postingsDocIds(List<Posting> postings) {
        return postings.stream()
                .mapToInt(posting -> posting.docId())
                .distinct()
                .toArray();
    }


}

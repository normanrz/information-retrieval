package SearchEngine.Query;

import SearchEngine.Import.PatentDocumentPreprocessor;
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

    public SearchResultSet search(String query) {

        // Tokenize query
        List<String> tokens = PatentDocumentPreprocessor.tokenizeKeepAsterisks(query);
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

        int[] results;

        // Execute search
        switch (searchType) {
            case AND:
                results = searchAnd(tokens);
                break;
            case OR:
                results = searchOr(tokens);
                break;
            case NOT:
                results = searchNot(tokens);
                break;
            case PHRASE:
                results = searchPhrase(tokens);
                break;
            default:
                results = new int[]{};
                break;
        }

        return new SearchResultSet(results, getStemmedQueryTokens());
    }


    private int[] searchPhrase(List<String> tokens) {

        if (tokens.size() == 0) {
            return emptyArray;
        } else {
            int tokenCount = 0;
            List<DocumentPostings> results = null;
            for (String token : tokens) {
                if (results == null) {
                    // First token
                    results = searchToken(token);
                } else {

                    // Subsequent tokens
                    final int finalTokenCount = tokenCount;
                    final int[] docIds = docIds(results);
                    List<DocumentPostings> tokenResults = searchTokenInDocs(token, docIds);
                    final int[] tokenDocIds = docIds(tokenResults);

                    // Shrink result set based on subsequent token matches
                    results = results.stream()
                            .filter(documentPostings -> IntArrayUtils.intArrayContains(tokenDocIds, documentPostings.getDocId()))
                            .filter(documentPostings -> tokenResults.stream()
                                            // Current token is in same document
                                            .filter(posting1 -> posting1.getDocId() == documentPostings.getDocId())
                                                    // Current token position matches expected position
                                            .anyMatch(posting1 -> {
                                                for (int pos1 : documentPostings.getPositions().toArray()) {
                                                    for (int pos2 : posting1.getPositions().toArray()) {
                                                        if (pos1 + finalTokenCount == pos2) {
                                                            return true;
                                                        }
                                                    }
                                                }
                                                return false;
                                            })
                            )
                            .collect(Collectors.toList());
                }
                tokenCount += 1;
            }
            return docIds(results);
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


    private List<DocumentPostings> searchToken(String token) {
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
        return results.collect(Collectors.toList());
    }

    private List<DocumentPostings> searchTokenInDocs(String token, int[] docIds) {
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
        return results.collect(Collectors.toList());

    }


    private int[] searchOr(List<String> tokens) {
        return docIds(tokens.stream()
                .map(this::searchToken)
                .flatMap(list -> list.stream()));
    }

    private int[] searchAnd(List<String> tokens) {
        if (tokens.isEmpty()) {
            return emptyArray;
        } else {
            int[] results = null;

            for (String token : tokens) {
                if (results == null) {
                    // First token
                    results = docIds(searchToken(token));
                } else {
                    // Subsequent tokens in intersecting documents
                    results = docIds(searchTokenInDocs(token, results));
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
            int[] docIds0 = docIds(searchToken(tokens.get(0)));
            int[] docIds1 = docIds(searchTokenInDocs(tokens.get(1), docIds0));

            return Arrays.stream(docIds0)
                    .filter(docId -> !IntArrayUtils.intArrayContains(docIds1, docId))
                    .toArray();
        }
    }


    private int[] postingsDocIds(List<Posting> postings) {
        return postings.stream()
                .mapToInt(Posting::docId)
                .distinct()
                .toArray();
    }

    private int[] docIds(List<DocumentPostings> documentPostings) {
        return docIds(documentPostings.stream());
    }

    private int[] docIds(Stream<DocumentPostings> documentPostingsStream) {
        return documentPostingsStream
                .mapToInt(DocumentPostings::getDocId)
                .toArray();
    }


    public static SearchResultSet search(String query, InvertedIndex index, boolean enableSpellingCorrection) {
        Searcher searcher = new Searcher(index);
        searcher.setShouldCorrectSpelling(enableSpellingCorrection);
        return searcher.search(query);
    }


}

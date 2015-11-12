package SearchEngine.Index;

import SearchEngine.Importer.PatentDocumentPreprocessor;
import SearchEngine.Posting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PostingIndexSearcher {

    private final int[] emptyArray = new int[0];
    private final PostingIndex index;


    public PostingIndexSearcher(PostingIndex index) {
        this.index = index;
    }


    public int[] search(String query) {
        // Tokenize query
        List<String> tokens = PatentDocumentPreprocessor.tokenizeAsStrings(query);
        tokens = mergeAsteriskTokens(tokens);

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
        tokens = lowerCaseTokens(tokens);
        tokens = removeStopwords(tokens);

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


    private List<Posting> searchToken(String token) {
        if (token.endsWith("*")) {
            // Prefix search (no stemming)
            token = token.substring(0, token.length() - 1);
            return index.getByPrefix(token).collect(Collectors.toList());
        } else {
            // Regular search with stemming
            String stemmedToken = PatentDocumentPreprocessor.stem(token);
            return index.get(stemmedToken).collect(Collectors.toList());
        }
    }

    private List<Posting> searchTokenInDocs(String token, int[] docIds) {
        return searchToken(token).stream()
                .filter(posting -> intArrayContains(docIds, posting.docId()))
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
                    .filter(docId -> !intArrayContains(docIds1, docId))
                    .toArray();
        }
    }


    private int[] postingsDocIds(List<Posting> postings) {
        return postings.stream()
                .mapToInt(posting -> posting.docId())
                .distinct()
                .toArray();
    }

    private boolean intArrayContains(final int[] array, final int key) {
        for (final int i : array) {
            if (i == key) {
                return true;
            }
        }
        return false;
    }


    private List<String> mergeAsteriskTokens(List<String> tokens) {
        List<String> outputTokens = new ArrayList<>();
        int i = 0;
        for (String token : tokens) {
            if (token.equals("*")) {
                if (i > 0) {
                    outputTokens.set(i - 1, outputTokens.get(i - 1) + "*");
                }
            } else {
                outputTokens.add(token);
                i++;
            }

        }
        return outputTokens;
    }


    private List<String> removeStopwords(List<String> tokens) {
        return tokens.stream()
                .filter(PatentDocumentPreprocessor::isNoStopword)
                .collect(Collectors.toList());
    }


    private List<String> lowerCaseTokens(List<String> tokens) {
        return tokens.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }


}

package SearchEngine.Index;

import SearchEngine.Importer.PatentDocumentPreprocessor;
import SearchEngine.Posting;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by norman on 05.11.15.
 */
public class PostingIndexSearcher {

    private final int[] emptyArray = new int[0];
    private final PostingIndex index;
    private final SnowballStemmer stemmer = new englishStemmer();


    public PostingIndexSearcher(PostingIndex index) {
        this.index = index;
    }


    public int[] search(String query) {
        List<String> tokens = PatentDocumentPreprocessor.tokenizeAsStrings(query);

        tokens = mergeAsteriskTokens(tokens);

        if (tokens.contains("AND")) {
            tokens = lowerCaseTokens(tokens);
            tokens = removeStopwords(tokens);
            return searchAnd(tokens.toArray(new String[tokens.size()]));
        } else if (tokens.contains("OR")) {
            tokens = lowerCaseTokens(tokens);
            tokens = removeStopwords(tokens);
            return searchOr(tokens.toArray(new String[tokens.size()]));
        } else if (tokens.contains("NOT")) {
            tokens = lowerCaseTokens(tokens);
            tokens = removeStopwords(tokens);
            return searchNot(tokens.toArray(new String[tokens.size()]));
        } else {
            tokens = lowerCaseTokens(tokens);
            tokens = removeStopwords(tokens);
            return searchPhrase(tokens.toArray(new String[tokens.size()]));
        }

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


    private int[] searchPhrase(String... tokens) {

        final int spaceLength = 1;

        if (tokens.length == 0) {
            return emptyArray;
        } else {
            int previousTokensLength = 0;
            List<Posting> results = null;
            for (String token : tokens) {
                if (results == null) {
                    results = searchToken(token);
                } else {

                    final int finalPreviousTokensLength = previousTokensLength;
                    final int[] docIds = postingsDocIds(results);
                    List<Posting> tokenResults = searchTokenInDocs(token, docIds);

                    results = results.stream()
                            .filter(posting ->
                                            tokenResults.stream()
                                                    .filter(posting1 -> posting1.docId() == posting.docId())
                                                    .anyMatch(posting1 -> posting1.pos() == posting.pos() + finalPreviousTokensLength)
                            )
                            .collect(Collectors.toList());
                }
                previousTokensLength += token.length() + spaceLength;
            }
            return postingsDocIds(results);
        }
    }


    private List<Posting> searchToken(String token) {
        if (token.endsWith("*")) {
            // Prefix search
            token = token.substring(0, token.length() - 1);
            return index.getByPrefix(token).collect(Collectors.toList());
        } else {
            // Regular search
            String stemmedToken = PatentDocumentPreprocessor.stem(token);
            return index.get(stemmedToken).collect(Collectors.toList());
        }
    }

    private List<Posting> searchTokenInDocs(String token, int[] docIds) {
        return searchToken(token).stream()
                .filter(posting -> intArrayContains(docIds, posting.docId()))
                .collect(Collectors.toList());
    }


    private int[] searchOr(String... tokens) {
        return postingsDocIds(Arrays.stream(tokens)
                .map(this::searchToken)
                .flatMap(postings -> postings.stream())
                .collect(Collectors.toList()));
    }

    // Returns postings with the positions of the last token
    private int[] searchAnd(String... tokens) {
        if (tokens.length == 0) {
            return emptyArray;
        } else {
            int[] results = null;

            for (String token : tokens) {
                if (results == null) {
                    // First token
                    results = postingsDocIds(searchToken(token));
                } else {
                    // Subsequent tokens
                    results = postingsDocIds(searchTokenInDocs(token, results));
                }

                if (results.length == 0) {
                    break;
                }
            }
            return results;
        }
    }

    private int[] searchNot(String... tokens) {
        if (tokens.length != 2) {
            return emptyArray;
        } else {

            List<Posting> results0 = searchToken(tokens[0]);
            int[] docIds0 = postingsDocIds(results0);

            List<Posting> results1 = searchTokenInDocs(tokens[1], docIds0);
            int[] docIds1 = postingsDocIds(results1);

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

}

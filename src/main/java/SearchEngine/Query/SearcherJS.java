package SearchEngine.Query;

import SearchEngine.Import.PatentDocumentPreprocessor;
import SearchEngine.InvertedIndex.DocumentPostings;
import SearchEngine.InvertedIndex.InvertedIndex;
import SearchEngine.InvertedIndex.Posting;
import SearchEngine.LinkIndex.LinkIndex;
import SearchEngine.Query.QueryParser.QueryParserJS;
import SearchEngine.utils.IntArrayUtils;
import SearchEngine.utils.LevenshteinDistance;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearcherJS {

    private final int[] emptyArray = new int[0];
    private final InvertedIndex index;
    private final LinkIndex linkIndex;


    private List<String> stemmedQueryTokens = new ArrayList<>();
    private boolean shouldCorrectSpelling = false;

    public SearcherJS(InvertedIndex index, LinkIndex linkIndex) {
        this.index = index;
        this.linkIndex = linkIndex;
    }

    public void setShouldCorrectSpelling(boolean state) {
        shouldCorrectSpelling = state;
    }


    public List<String> getStemmedQueryTokens() {
        return stemmedQueryTokens;
    }

    public SearchResultSet search(ScriptObjectMirror query) {
        int[] docIds = execQuery((ScriptObjectMirror) query.get("query"));
        int[] results = execNot(((ScriptObjectMirror) query.get("not")).values(), docIds);
        return new SearchResultSet(results, getStemmedQueryTokens());
    }

    private Collection<Object> getValues(ScriptObjectMirror obj) {
        return ((ScriptObjectMirror) obj.get("values")).values();
    }

    private int[] execPhraseQuery(Collection<Object> values) {

        if (values.size() == 0) {
            return emptyArray;
        } else {
            int tokenCount = 0;
            List<DocumentPostings> results = null;
            for (Object _tokenObj : values) {
                ScriptObjectMirror tokenObj = (ScriptObjectMirror)_tokenObj;
                if (results == null) {
                    // First token
                    results = execTokenWithPostings(tokenObj);
                } else {

                    // Subsequent tokens
                    final int finalTokenCount = tokenCount;
                    final int[] docIds = docIds(results);
                    List<DocumentPostings> tokenResults = execTokenWithPostingsInDocs(tokenObj, docIds);
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

    private int[] execToken(ScriptObjectMirror obj) {
        return docIds(execTokenWithPostings(obj));
    }

    private List<DocumentPostings> execTokenWithPostingsInDocs(ScriptObjectMirror tokenObj, int[] docIds) {
        return execTokenWithPostings(tokenObj)
                .stream()
                .filter(documentPostings -> IntArrayUtils.intArrayContains(docIds, documentPostings.getDocId()))
                .collect(Collectors.toList());
    }

    private List<DocumentPostings> execTokenWithPostings(ScriptObjectMirror obj) {
        String token = (String) obj.get("value");
        boolean isPrefix = (Boolean) obj.get("isPrefix");
        token = token.toLowerCase();

        Stream<DocumentPostings> results;
        if (isPrefix) {
            // Prefix search (no stemming)
            results = index.getByPrefix(token);
            stemmedQueryTokens.add(token);
        } else {
            // Regular search with stemming
            String stemmedToken = PatentDocumentPreprocessor.stem(token);
            results = index.get(stemmedToken);

            List<DocumentPostings> tmpResults = results.collect(Collectors.toList());
            if (shouldCorrectSpelling && tmpResults.size() == 0) {
                Optional<String> correctedTokenOptional = findCorrectSpelledToken(stemmedToken);
                if (correctedTokenOptional.isPresent()) {
                    stemmedToken = correctedTokenOptional.get();
                    results = index.get(stemmedToken);
                    stemmedQueryTokens.add(stemmedToken);
                } else {
                    results = Stream.empty();
                }
            } else {
                results = tmpResults.stream();
                stemmedQueryTokens.add(stemmedToken);
            }
        }
        return results.collect(Collectors.toList());
    }

    private int[] execOr(Collection<Object> values) {
        return values.stream()
                .map(value -> this.execQuery((ScriptObjectMirror) value))
                .flatMapToInt(Arrays::stream)
                .distinct()
                .toArray();
    }

    private int[] execAnd(Collection<Object> values) {
        return values.stream()
                .map(value -> this.execQuery((ScriptObjectMirror) value))
                .reduce(IntArrayUtils::intersection).get();
    }

    private int[] execQuery(ScriptObjectMirror obj) {
        String type = (String) obj.get("type");
        switch (type) {
            case "and":
                return execAnd(getValues(obj));
            case "or":
                return execOr(getValues(obj));
            case "phrase":
                return execPhraseQuery(getValues(obj));
            case "token":
                return execToken(obj);
            case "link":
                int docId = ((Double) obj.get("value")).intValue();
                return linkIndex.get(docId);
        }
        return emptyArray;
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


    private int[] execNot(Collection<Object> values, int[] docIds0) {

            // Remove intersecting documents
            int[] docIds1 = execOr(values);

            return Arrays.stream(docIds0)
                    .filter(docId -> !IntArrayUtils.intArrayContains(docIds1, docId))
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


    public static SearchResultSet search(
            ScriptObjectMirror query, InvertedIndex index, LinkIndex linkIndex, boolean enableSpellingCorrection) {
        SearcherJS searcher = new SearcherJS(index, linkIndex);
        searcher.setShouldCorrectSpelling(enableSpellingCorrection);
        return searcher.search(query);
    }


}

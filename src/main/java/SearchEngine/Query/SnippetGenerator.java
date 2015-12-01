package SearchEngine.Query;

import SearchEngine.DocumentIndex.XmlDocumentIndex;
import SearchEngine.Importer.PatentDocumentPreprocessor;
import SearchEngine.PatentDocument;
import SearchEngine.utils.ArrayUtils;
import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.IntList;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by norman on 26.11.15.
 */
public class SnippetGenerator {

    private final int defaultTokenMargin = 4;
    private final XmlDocumentIndex documentIndex;

    public SnippetGenerator(XmlDocumentIndex documentIndex) {
        this.documentIndex = documentIndex;
    }


    private int[][] partitionPositions(int[] source, int minOffset) {

        List<IntList> output = new ArrayList<>();

        IntList currentPartition = null;

        int lastPos = Integer.MIN_VALUE;
        for (int pos : source) {

            if (lastPos + minOffset < pos) {
                if (currentPartition != null) {
                    output.add(currentPartition);
                }
                currentPartition = new ArrayIntList();
                currentPartition.add(pos);
            } else {
                if (currentPartition == null) {
                    currentPartition = new ArrayIntList();
                }
                currentPartition.add(pos);
            }

            lastPos = pos;
        }

        if (currentPartition != null && currentPartition.size() > 0) {
            output.add(currentPartition);
        }

        return output.stream().map(IntList::toArray).toArray(size -> new int[size][]);

    }

    private boolean containsQueryToken(List<String> tokens, List<String> queryTokens) {
        return tokens.stream()
                .map(PatentDocumentPreprocessor::stem)
                .filter(token -> queryTokens.contains(token))
                .count() > 0;
    }

    private String shortenSnippetByPunctuation(String snippet, List<String> queryTokens, int tokenMargin) {

        String[] splitSnippet = snippet.split("[\\.!\\?]");

        if (splitSnippet.length > 1) {
            String firstSplit = ArrayUtils.first(splitSnippet);
            String lastSplit = ArrayUtils.last(splitSnippet);
            List<String> firstSplitTokens = PatentDocumentPreprocessor.tokenize(firstSplit);
            List<String> lastSplitTokens = PatentDocumentPreprocessor.tokenize(lastSplit);

            int startIndex = 0;
            int endIndex = snippet.length();

            if (firstSplitTokens.size() <= tokenMargin && !containsQueryToken(firstSplitTokens, queryTokens)) {
                startIndex = firstSplit.length() + 1;
            }

            if (lastSplitTokens.size() <= tokenMargin && !containsQueryToken(lastSplitTokens, queryTokens)) {
                endIndex = endIndex - lastSplit.length() + 1;
            }

            return snippet.substring(startIndex, endIndex).trim();
        } else {
            return snippet;
        }
    }

    public List<String> getSnippets(String body, List<String> queryTokens, int tokenMargin) {

        List<Pair<Integer, String>> bodyTokens = PatentDocumentPreprocessor.tokenizeWithOffset(body);
        List<String> bodyStemmedTokens = bodyTokens.stream()
                .map(Pair::getRight)
                .map(String::toLowerCase)
                .map(PatentDocumentPreprocessor::stem)
                .collect(Collectors.toList());

        int[][] partitions = partitionPositions(queryTokens.stream()
                .flatMapToInt(queryToken ->
                                IntStream.range(0, bodyStemmedTokens.size())
                                        .filter(i -> bodyStemmedTokens.get(i).equals(queryToken))
                )
                .flatMap(pos -> IntStream.range(pos - tokenMargin, pos + tokenMargin))
                .distinct()
                .sorted()
                .filter(pos -> pos > 0 && pos < bodyStemmedTokens.size())
                .toArray(), 1);

        return Arrays.stream(partitions)
                .map(partition -> {
                    Pair<Integer, String> first = bodyTokens.get(ArrayUtils.first(partition));
                    Pair<Integer, String> last = bodyTokens.get(ArrayUtils.last(partition));
                    return body.substring(first.getLeft(), last.getLeft() + last.getRight().length());
                })
                .map(snippet -> shortenSnippetByPunctuation(snippet, queryTokens, tokenMargin))
                .collect(Collectors.toList());
    }


    public List<String> getSnippets(int docId, List<String> queryTokens) {
        return getSnippets(docId, queryTokens, defaultTokenMargin);
    }

    public List<String> getSnippets(int docId, List<String> queryTokens, int tokenMargin) {
        return getSnippets(
                documentIndex.getPatentDocument(docId).map(PatentDocument::getAbstractText).get(),
                queryTokens, tokenMargin);
    }

}

package SearchEngine;

import SearchEngine.DocumentIndex.XmlDocumentIndex;
import SearchEngine.Importer.PatentDocumentImporter;
import SearchEngine.InvertedIndex.InvertedIndexMerger;
import SearchEngine.InvertedIndex.disk.DiskInvertedIndex;
import SearchEngine.InvertedIndex.memory.MemoryInvertedIndex;
import SearchEngine.Query.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author: JasperRzepka
 * @dataset: US patent grants : ipg files from http://www.google.com/googlebooks/uspto-patents-grants-text.html
 * @course: Information Retrieval and Web Search, Hasso-Plattner Institut, 2015
 * <p>
 * This is your file! implement your search engine here!
 * <p>
 * Describe your search engine briefly:
 * - multi-threaded?
 * - stemming?
 * - stopword removal?
 * - index algorithm?
 * - etc.
 * <p>
 * Keep in mind to include your implementation decisions also in the pdf file of each assignment
 */


public class SearchEngineJasperRzepka implements AutoCloseable {

    private static final String invertedIndexFileName = "inverted.index";
    private static final String documentIndexFileName = "document.index";

    protected DiskInvertedIndex index;
    protected XmlDocumentIndex docIndex;

    public static void index(String dataDirectory, String outputDirectory) {

        XmlDocumentIndex documentIndex = new XmlDocumentIndex(dataDirectory);
        AtomicInteger subIndexCounter = new AtomicInteger(0);
        List<File> subIndexFiles = new ArrayList<>();

        File dir = new File(dataDirectory);
        Stream.of(dir.listFiles()).parallel()
                .filter(file -> file.getName().endsWith((".xml.gz")))
                .forEach(file -> {

                    MemoryInvertedIndex localIndex = new MemoryInvertedIndex();

                    PatentDocumentImporter.importPatentDocuments(file, localIndex, documentIndex);

                    File indexFile = new File(outputDirectory,
                            String.format("%s.%02d", invertedIndexFileName, subIndexCounter.getAndIncrement()));
                    subIndexFiles.add(indexFile);

                    System.out.println(indexFile.getName());
                    localIndex.printStats();
                    try {
                        localIndex.save(indexFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        try {
            documentIndex.save(new File(outputDirectory, documentIndexFileName));
            InvertedIndexMerger.merge(subIndexFiles, new File(outputDirectory, invertedIndexFileName));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void indexTest(String sourceFileName, String outputDirectory) {

        File sourceFile = new File(sourceFileName);
        XmlDocumentIndex documentIndex = new XmlDocumentIndex(sourceFile.getParent());
        MemoryInvertedIndex testIndex = new MemoryInvertedIndex();

        PatentDocumentImporter.importPatentDocuments(sourceFile, testIndex, documentIndex);

        System.out.println("Imported test index");
        testIndex.printStats();
        try {
            testIndex.save(new File(outputDirectory, invertedIndexFileName));
            documentIndex.save(new File(outputDirectory, documentIndexFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void loadIndex(String indexDirectory, String dataDirectory) {
        try {
            docIndex = XmlDocumentIndex.load(dataDirectory, new File(indexDirectory, documentIndexFileName));
            index = new DiskInvertedIndex(new File(indexDirectory, invertedIndexFileName));
            index.printStats();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private SnippetSearchResult createSnippetSearchResult(
            SearchResult result, XmlDocumentIndex docIndex,
            SnippetGenerator snippetGenerator, List<String> queryTokens) {

        PatentDocument doc = docIndex.getPatentDocument(result.getDocId()).get();
        return new SnippetSearchResult(
                result, doc,
                snippetGenerator.getSnippets(doc, queryTokens));
    }

    public Stream<SnippetSearchResult> search(String query, int prf) {

        // Set up
        Ranker ranker = new Ranker(index, docIndex);
        SnippetGenerator snippetGenerator = new SnippetGenerator();

        // Search
        SearchResultSet searchResultSet = Searcher.search(query, index, true);
        List<String> queryTokens = searchResultSet.getQueryTokens();

        // Rank first-pass
        Stream<SearchResult> rankResults = ranker.rank(searchResultSet);

        if (prf == 0) {
            return rankResults
                    .map(result -> createSnippetSearchResult(result, docIndex, snippetGenerator, queryTokens));
        } else {
            // Generate snippets
            List<SnippetSearchResult> prfDocumentSnippetsResults = rankResults
                    .limit(prf)
                    .map(result -> createSnippetSearchResult(result, docIndex, snippetGenerator, queryTokens))
                    .collect(Collectors.toList());

            // Pseudo relevance feedback model with snippets
            Map<String, Double> relevanceModel =
                    ranker.pseudoRelevanceModel(queryTokens, prfDocumentSnippetsResults);

//            // Pseudo relevance feedback model with whole documents
//            int[] topRankedDocIds = SearchResult.getDocIds(rankResults.subList(0, Math.min(prf, rankResults.size())));
//            Map<String, Double> relevanceModel =
//                    ranker.pseudoRelevanceModel(queryTokens, topRankedDocIds);

            List<String> newQueryTokens = Ranker.expandQueryFromRelevanceModel(relevanceModel, queryTokens);
            String newQuery = String.join(" OR ", newQueryTokens);

            // Search and rank again
            return ranker.rankWithRelevanceModel(Searcher.search(newQuery, index, false).getDocIds(), relevanceModel).stream()
                    .map(result -> createSnippetSearchResult(result, docIndex, snippetGenerator, newQueryTokens));
        }

    }

    public List<String> search(String query, int topK, int prf) {

        return search(query, prf)
                .limit(topK)
                .map(result -> result.toString())
                .collect(Collectors.toList());

    }

    public void close() throws IOException {
        index.close();
    }
}

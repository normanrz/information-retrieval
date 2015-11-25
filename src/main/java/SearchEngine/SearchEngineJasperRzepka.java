package SearchEngine;

import SearchEngine.Importer.PatentDocumentImporter;
import SearchEngine.Index.DocumentIndex;
import SearchEngine.Index.MemoryPostingIndex;
import SearchEngine.Query.PostingIndexRanker;
import SearchEngine.Query.PostingIndexSearcher;
import SearchEngine.Index.disk.DiskPostingIndex;
import SearchEngine.Query.PostingSearchResult;

import java.io.File;
import java.io.IOException;
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


public class SearchEngineJasperRzepka extends SearchEngine implements AutoCloseable {

    protected static String baseDirectory = "data/";
    protected static int numberOfThreads = Runtime.getRuntime().availableProcessors();

    protected MemoryPostingIndex index = new MemoryPostingIndex();
    protected DocumentIndex docIndex = new DocumentIndex("docs");

    public SearchEngineJasperRzepka() {
        // This should stay as is! Don't add anything here!
        super();
    }

    @Override
    void index(String directory) {

        AtomicInteger subIndexCounter = new AtomicInteger(0);

        File dir = new File(directory);
        Stream.of(dir.listFiles()).parallel()
                .filter(file -> file.getName().endsWith((".xml")))
                .map(PatentDocumentImporter::readCompressedPatentDocuments)
                .forEach(patentDocumentStream -> {
                    MemoryPostingIndex localIndex = new MemoryPostingIndex();

                    patentDocumentStream.forEach(doc ->
                            PatentDocumentImporter.importPatentDocument(doc, localIndex, docIndex));

                    localIndex.printStats();
                    try {
                        localIndex.save(new File(
                                String.format("index.%02d.bin.gz", subIndexCounter.getAndIncrement())));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    void indexTest(String sourceFile, String outputFile) {

        final MemoryPostingIndex testIndex = new MemoryPostingIndex();

        PatentDocumentImporter.readPatentDocuments(new File(sourceFile))
                .forEach(doc ->
                        PatentDocumentImporter.importPatentDocument(doc, testIndex, docIndex));

        System.out.println("Imported test index");
        testIndex.printStats();
        try {
            testIndex.save(new File(outputFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    boolean loadIndex(String directory) {
        try {
            System.out.println("Load index");
            index = MemoryPostingIndex.load(new File(directory));
            index.printStats();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    void compressIndex(String directory) {
        // Do nothing
    }

    @Override
    boolean loadCompressedIndex(String directory) {
        return loadIndex(directory);
    }

    Stream<PostingSearchResult> _search(String query, int prf) {
        try (DiskPostingIndex diskIndex = new DiskPostingIndex("index.bin.gz", docIndex)) {

            // Set up
            PostingIndexSearcher searcher = new PostingIndexSearcher(diskIndex);
            PostingIndexRanker ranker = new PostingIndexRanker(diskIndex, docIndex);

//            searcher.setShouldCorrectSpelling(true);

            // Search
            int[] searchResults = searcher.search(query);

            List<String> queryTokens = searcher.getStemmedQueryTokens();

            // Rank first-pass
            List<PostingSearchResult> rankResults = ranker.rank(queryTokens, searchResults);

            if (prf == 0) {
                return rankResults.stream();
            } else {
                // Pseudo relevance feedback model
                Map<String, Double> relevanceModel =
                        ranker.pseudoRelevanceModel(queryTokens,
                                PostingSearchResult.getDocIds(rankResults.subList(0, Math.min(rankResults.size(), prf))));

                String newQuery = PostingIndexRanker.getQueryFromRelevanceModel(relevanceModel);
                System.out.println(newQuery);

                // Search and rank again
                return ranker.rankWithRelevanceModel(searcher.search(newQuery), relevanceModel).stream();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    @Override
    List<String> search(String query, int topK, int prf) {

        return _search(query, prf)
                .limit(topK)
                .map(result -> String.format("%08d\t%s", result.getDocId(),
                        docIndex.getPatentDocumentTitle(result.getDocId())))
                .collect(Collectors.toList());

    }

    public void close() {
        docIndex.close();
    }
}

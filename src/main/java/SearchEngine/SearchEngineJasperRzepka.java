package SearchEngine;

import SearchEngine.Importer.PatentDocumentImporter;
import SearchEngine.Importer.PatentDocumentPreprocessor;
import SearchEngine.Index.DocumentIndex;
import SearchEngine.Index.MemoryPostingIndex;
import SearchEngine.Index.PostingIndexRanker;
import SearchEngine.Index.PostingIndexSearcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

        AtomicInteger docCounter = new AtomicInteger(0);

        File dir = new File(directory);
        Stream.of(dir.listFiles()).parallel()
                .filter(file -> file.getName().endsWith((".xml")))
                .map(PatentDocumentImporter::readCompressedPatentDocuments)
                .forEach(patentDocumentStream -> {
                    MemoryPostingIndex localIndex = new MemoryPostingIndex();

                    patentDocumentStream.forEach(doc -> {
                        AtomicInteger tokenPosition = new AtomicInteger(0);
                        ArrayList<String> tokens = new ArrayList<>();

                        String tokenizableDocument = (doc.title + " " + doc.abstractText).toLowerCase();
                        PatentDocumentPreprocessor.tokenize(tokenizableDocument).stream()
                                .filter(PatentDocumentPreprocessor::isNoStopword)
                                .forEach(token -> {
                                    String stemmedToken = PatentDocumentPreprocessor.stem(token.value());
                                    localIndex.putPosting(stemmedToken, doc, tokenPosition.getAndIncrement());
                                    tokens.add(token.value());
                                });

                        docIndex.storePatentDocument(doc);
                    });

                    localIndex.printStats();
                    localIndex.saveCompressed(new File(String.format("index.%02d.bin.gz", docCounter.getAndIncrement())));
                });


        System.out.println("Imported index");
        index.printStats();
        index.saveCompressed(new File("index.big.gz"));

    }

    void indexTest(String sourceFile, String outputFile) {

        final MemoryPostingIndex testIndex = new MemoryPostingIndex();

        PatentDocumentImporter.readPatentDocuments(new File(sourceFile))
                .forEach(doc -> {
                    AtomicInteger tokenPosition = new AtomicInteger(0);
                    ArrayList<String> tokens = new ArrayList<>();

                    String tokenizableDocument = (doc.title + " " + doc.abstractText).toLowerCase();
                    PatentDocumentPreprocessor.tokenize(tokenizableDocument).stream()
                            .filter(PatentDocumentPreprocessor::isNoStopword)
                            .forEach(token -> {
                                String stemmedToken = PatentDocumentPreprocessor.stem(token.value());
                                testIndex.putPosting(stemmedToken, doc, tokenPosition.getAndIncrement());
                                tokens.add(token.value());
                            });

                    docIndex.storePatentDocument(doc);
                });

        System.out.println("Imported test index");
        testIndex.printStats();
        testIndex.saveCompressed(new File(outputFile));

    }

    @Override
    boolean loadIndex(String directory) {
        System.out.println("Load index");
        index = MemoryPostingIndex.load(new File(directory));
        index.printStats();
        return false;
    }

    @Override
    void compressIndex(String directory) {
        index.saveCompressed(new File("index.bin.gz"));
    }

    @Override
    boolean loadCompressedIndex(String directory) {
        System.out.println("Load compressed index");
        index = MemoryPostingIndex.loadCompressed(new File(directory));
        index.printStats();
        return false;
    }

    @Override
    List<String> search(String query, int topK, int prf) {
        PostingIndexSearcher searcher = new PostingIndexSearcher(index);
        PostingIndexRanker ranker = new PostingIndexRanker(index);

        return ranker.rank(query, searcher.search(query), 2000).stream()
                .limit(topK)
                .map(result -> String.format("%08d\t%.8f\t%s", result.docId, result.rank,
                        docIndex.getPatentDocumentTitle(result.docId)))
                .collect(Collectors.toList());

    }

    public void close() {
        docIndex.close();
    }
}

package SearchEngine;

import SearchEngine.Importer.PatentDocumentImporter;
import SearchEngine.Importer.PatentDocumentPreprocessor;
import SearchEngine.Index.MemoryPostingIndex;
import SearchEngine.Index.PostingIndexRanker;
import SearchEngine.Index.PostingIndexSearcher;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.iq80.leveldb.impl.Iq80DBFactory.*;

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


public class SearchEngineJasperRzepka extends SearchEngine {

    protected static String baseDirectory = "data/";
    protected static int numberOfThreads = Runtime.getRuntime().availableProcessors();

    protected MemoryPostingIndex index = new MemoryPostingIndex();

    public SearchEngineJasperRzepka() {
        // This should stay as is! Don't add anything here!
        super();
    }

    @Override
    void index(String directory) {
//        try {
//            Options options = new Options();
//            options.createIfMissing(true);
//            final DB db = factory.open(new File("docs"), options);

        AtomicInteger i = new AtomicInteger(0);

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

//                        storeDoc(doc, db);
                    });

                    localIndex.printStats();
                    localIndex.saveCompressed(new File(String.format("index.%02d.gz", i.getAndIncrement())));

                });


//


        System.out.println("Imported index");
        index.printStats();
        index.saveCompressed(new File("index.big.gz"));

//            db.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    void indexTest(String sourceFile, String outputFile) {

        Options options = new Options();
        options.createIfMissing(true);

        try (DB db = factory.open(new File("docs"), options)) {

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

                        storeDoc(doc, db);
                    });

            System.out.println("Imported test index");
            testIndex.printStats();
            testIndex.saveCompressed(new File(outputFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
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
        Options options = new Options();
        options.createIfMissing(true);
        try (DB db = factory.open(new File("docs"), options)) {

            PostingIndexSearcher searcher = new PostingIndexSearcher(index);
            PostingIndexRanker ranker = new PostingIndexRanker(index);

            return ranker.rank(query, searcher.search(query), 2000).stream()
                    .limit(topK)
                    .map(postingSearchResult -> String.format("%08d", postingSearchResult.docId))
                    .map(docId -> String.format("%s\t%s", docId, loadDocTitle(docId, db)))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public static String loadDocTitle(String docId, DB db) {
        return asString(db.get(bytes(String.format("%s:title", docId))));
    }

    public static synchronized void storeDoc(PatentDocument doc, DB db) {
        db.put(bytes(String.format("%s:title", doc.docNumber)), bytes(doc.title));
        db.put(bytes(String.format("%s:abstract", doc.docNumber)), bytes(doc.abstractText));
    }
}

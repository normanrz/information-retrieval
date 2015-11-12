package SearchEngine;

import SearchEngine.Importer.PatentDocumentImporter;
import SearchEngine.Importer.PatentDocumentPreprocessor;
import SearchEngine.Index.PostingIndex;
import SearchEngine.Index.PostingIndexSearcher;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

    protected PostingIndex index = new PostingIndex();

    public SearchEngineJasperRzepka() {
        // This should stay as is! Don't add anything here!
        super();
    }

    @Override
    void index(String directory) {
//        try {
        Options options = new Options();
        options.createIfMissing(true);
//            final DB db = factory.open(new File("docs"), options);

        File dir = new File(directory);
        Stream.of(dir.listFiles()).parallel()
                .filter(file -> file.getName().endsWith((".xml")))
                .map(PatentDocumentImporter::readPatentDocuments)
                .flatMap(value -> value)
                .forEach(doc -> {
                    AtomicInteger tokenPosition = new AtomicInteger(0);
                    ArrayList<String> tokens = new ArrayList<String>();

                    String tokenizableDocument = (doc.title + " " + doc.abstractText).toLowerCase();
                    PatentDocumentPreprocessor.tokenize(tokenizableDocument).stream()
                            .filter(PatentDocumentPreprocessor::isNoStopword)
                            .forEach(token -> {
                                String stemmedToken = PatentDocumentPreprocessor.stem(token.value());
                                index.putPosting(stemmedToken, doc, tokenPosition.getAndIncrement());
                                tokens.add(token.value());
                            });


//                        String processedDocument = String.join(" ", tokens);

//                        db.put(bytes(String.format("%s:title", doc.docNumber)), bytes(doc.title));
//                        db.put(bytes(String.format("%s:abstract", doc.docNumber)), bytes(doc.abstractText));

                });


        System.out.println("Imported index");
        index.printStats();
//            index.save(new File("index.bin"));
        index.saveCompressed(new File("index.bin.gz"));

//            db.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    boolean loadIndex(String directory) {
        System.out.println("Load index");
        index = PostingIndex.load(new File("index.bin"));
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
        index = PostingIndex.loadCompressed(new File("index.bin.gz"));
        index.printStats();
        return false;
    }

    @Override
    ArrayList<String> search(String query, int topK, int prf) {
        Options options = new Options();
        options.createIfMissing(true);
        ArrayList<String> result = null;
        try {
            final DB db = factory.open(new File("docs"), options);

            PostingIndexSearcher searcher = new PostingIndexSearcher(index);
            result = Arrays.stream(searcher.search(query))
                    .mapToObj(docId -> String.format("%08d", docId))
                    .map(docId -> String.format("%s\t%s", docId, loadDocTitle(docId, db)))
                    .collect(Collectors.toCollection(ArrayList::new));

            searcher.rankResults(query, searcher.search(query), 2000).forEach(postingSearchResult -> {
                System.out.println(String.format("%08d %.8f", postingSearchResult.docId, postingSearchResult.rank));
            });

            db.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static String loadDocTitle(String docId, DB db) {
        return asString(db.get(bytes(String.format("%s:title", docId))));
    }
}

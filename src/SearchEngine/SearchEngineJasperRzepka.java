package SearchEngine;

/**
 *
 * @author: JasperRzepka
 * @dataset: US patent grants : ipg files from http://www.google.com/googlebooks/uspto-patents-grants-text.html
 * @course: Information Retrieval and Web Search, Hasso-Plattner Institut, 2015
 *
 * This is your file! implement your search engine here!
 * 
 * Describe your search engine briefly:
 *  - multi-threaded?
 *  - stemming?
 *  - stopword removal?
 *  - index algorithm?
 *  - etc.  
 * 
 * Keep in mind to include your implementation decisions also in the pdf file of each assignment
 */

import SearchEngine.SaxImporter.SaxImporter;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


public class SearchEngineJasperRzepka extends SearchEngine {


    protected static String baseDirectory = "data/";
    protected static int numberOfThreads = Runtime.getRuntime().availableProcessors();

    public SearchEngineJasperRzepka() {
        // This should stay as is! Don't add anything here!
        super();
    }

    @Override
    void index(String directory) {

            File dir = new File(directory);
            Stream.of(dir.listFiles()).parallel()
                    .filter(file -> file.getName().endsWith((".xml.gz")))
                    .map(file -> SaxImporter.readDocNumberFromGzip(file))
                    .filter(Optional::isPresent)
                    .flatMap(Optional::get)
                    .forEach(doc -> System.out.println(doc));

    }

    @Override
    boolean loadIndex(String directory) {
        return false;
    }
    
    @Override
    void compressIndex(String directory) {
    }

    @Override
    boolean loadCompressedIndex(String directory) {
        return false;
    }
    
    @Override
    ArrayList<String> search(String query, int topK, int prf) {
        return null;
    }
    
}

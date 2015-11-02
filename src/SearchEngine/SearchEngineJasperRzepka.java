package SearchEngine;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

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

import SearchEngine.SaxImporter.SaxImporter;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;


public class SearchEngineJasperRzepka extends SearchEngine {

    // http://www.uspto.gov/patft//help/stopword.htm
    protected static List<String> stopwords = Arrays.asList(new String[] {
            "a", "has", "such", "accordance", "have", "suitable", "according", "having", "than", "all", "herein",
            "that", "also", "however", "the", "an", "if", "their", "and", "in", "then", "another", "into", "there",
            "are", "invention", "thereby", "as", "is", "therefore", "at", "it", "thereof", "be", "its", "thereto",
            "because", "means", "these", "been", "not", "they", "being", "now", "this", "by", "of", "those", "claim",
            "on", "thus", "comprises", "onto", "to", "corresponding", "or", "use", "could", "other", "various",
            "described", "particularly", "was", "desired", "preferably", "were", "do", "preferred", "what", "does",
            "present", "when", "each", "provide", "where", "embodiment", "provided", "whereby", "fig", "provides",
            "wherein", "figs", "relatively", "which", "for", "respectively", "while", "from", "said", "who", "further",
            "should", "will", "generally", "since", "with", "had", "some", "would"
    });

    protected static String baseDirectory = "data/";
    protected static int numberOfThreads = Runtime.getRuntime().availableProcessors();

    protected final PostingIndex index = new PostingIndex();
    protected final SnowballStemmer stemmer = new englishStemmer();

    public SearchEngineJasperRzepka() {
        // This should stay as is! Don't add anything here!
        super();
    }

    @Override
    void index(String directory) {

        File dir = new File(directory);
        Stream.of(dir.listFiles()).parallel()
                .filter(file -> file.getName().endsWith((".xml")))
                .map(SaxImporter::readDocNumberAndTitle)
                .flatMap(value -> value)
                .forEach(doc -> {

                    PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<>(
                            new StringReader((doc.title + " " + doc.abstractText).toLowerCase()), new CoreLabelTokenFactory(), "");

                    tokenizerAsStream(tokenizer)
                            .filter(token ->  !stopwords.contains(token.value()))
                            .forEach(token -> {
                                String stemmedToken = stem(token.value());
                                index.put(stemmedToken, new Posting(doc, token.beginPosition()));
                            });

                });


        index.printStats();

        index.save(new File("index.bin"));
    }

    @Override
    boolean loadIndex(String directory) {
        PostingIndex idx = PostingIndex.load(new File(directory));
        idx.printStats();
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
        String stemmedQuery = stem(query.toLowerCase());
        return index.get(stemmedQuery)
                .map(posting -> posting.doc())
                .distinct()
                .map(doc -> doc + ": " + doc.title + ": " + doc.abstractText)
                .collect(Collectors.toCollection(ArrayList::new));

    }

    private synchronized String stem(String word) {
        stemmer.setCurrent(word);
        if (stemmer.stem()) {
            return stemmer.getCurrent();
        } else {
            return word;
        }
    }

    private static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<T>() {
                            public T next() {
                                return e.nextElement();
                            }

                            public boolean hasNext() {
                                return e.hasMoreElements();
                            }
                        },
                        Spliterator.ORDERED), false);
    }

    private static <T> Stream<T> tokenizerAsStream(Tokenizer<T> e) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        e, Spliterator.ORDERED), false);
    }

}

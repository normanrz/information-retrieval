package SearchEngine;

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
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.io.File;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class SearchEngineJasperRzepka extends SearchEngine {

    // http://snowball.tartarus.org/algorithms/english/stop.txt
    protected static List<String> stopwords = Arrays.asList(new String[] {
            "i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself",
            "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself",
            "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that",
            "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "having", "do", "does", "did", "doing", "would", "should", "could", "ought", "i'm", "you're", "he's",
            "she's", "it's", "we're", "they're", "i've", "you've", "we've", "they've", "i'd", "you'd", "he'd",
            "she'd", "we'd", "they'd", "i'll", "you'll", "he'll", "she'll", "we'll", "they'll", "isn't", "aren't",
            "wasn't", "weren't", "hasn't", "haven't", "hadn't", "doesn't", "don't", "didn't", "won't", "wouldn't",
            "shan't", "shouldn't", "can't", "cannot", "couldn't", "mustn't", "let's", "that's", "who's", "what's",
            "here's", "there's", "when's", "where's", "why's", "how's", "a", "an", "the", "and", "but", "if", "or",
            "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into",
            "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on",
            "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how",
            "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only",
            "own", "same", "so", "than", "too", "very"
    });

    protected static String baseDirectory = "data/";
    protected static int numberOfThreads = Runtime.getRuntime().availableProcessors();

    protected final IndexJasperRzepka<Tuple<PatentDocument, Integer>> index = new IndexJasperRzepka<>();
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
                            new StringReader(doc.title + " " + doc.abstractText), new CoreLabelTokenFactory(), "");

                    tokenizerAsStream(tokenizer)
                            .filter(token ->  !stopwords.contains(token.value()))
                            .forEach(token -> {
                                String stemmedToken = stem(token.value());
                                index.put(stemmedToken, new Tuple<PatentDocument, Integer>(doc, token.beginPosition()));
                            });

                });


        index.printStats();
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
        String stemmedQuery = stem(query);
        return index.get(stemmedQuery)
                .map(tuple -> tuple.x)
                .distinct()
                .map(doc -> doc.docNumber + ": " + doc.title + ": " + doc.abstractText)
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

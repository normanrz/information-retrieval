package SearchEngine.Importer;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by norman on 05.11.15.
 */
public class PatentDocumentPreprocessor {

    // http://www.uspto.gov/patft//help/stopword.htm
    protected static List<String> stopwords = Arrays.asList(new String[]{
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

    protected static final SnowballStemmer stemmer = new englishStemmer();

    public static synchronized String stem(String word) {
        stemmer.setCurrent(word);
        if (stemmer.stem()) {
            return stemmer.getCurrent();
        } else {
            return word;
        }
    }

    public static List<CoreLabel> tokenize(String query) {
        return new PTBTokenizer<>(new StringReader(query), new CoreLabelTokenFactory(), "").tokenize();
    }

    public static List<String> tokenizeAsStrings(String query) {
        return tokenize(query)
                .stream()
                .map(CoreLabel::value)
                .collect(Collectors.toList());
    }

    public static boolean isNoStopword(CoreLabel token) {
        return isNoStopword(token.value());
    }

    public static boolean isNoStopword(String token) {
        return !stopwords.contains(token);
    }

}

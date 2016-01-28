package SearchEngine.Import;

import org.apache.commons.lang3.tuple.Pair;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by norman on 05.11.15.
 */
public class PatentDocumentPreprocessor {

    protected static final SnowballStemmer stemmer = new englishStemmer();
    final private static int MIN_TOKEN_LENGTH = 2;
    private final static Pattern whitespacePattern = Pattern.compile("[^\\p{L}\\p{Nd}\\-]+");
    private final static Pattern tokenPattern = Pattern.compile("[\\p{L}\\p{Nd}\\-]+");

    protected static List<String> stopwords = Arrays.asList(new String[]{
            // Own selection
            "may", "one", "first", "can",
            // From: http://www.uspto.gov/patft//help/stopword.htm
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

    public static synchronized String stem(String word) {
        stemmer.setCurrent(word);
        if (stemmer.stem()) {
            return stemmer.getCurrent();
        } else {
            return word;
        }
    }

    public static String cleanToken(String token) {
        return token;
    }

    public static List<String> tokenizeAsList(String query) {
        return tokenize(query).collect(Collectors.toList());
    }

    public static Stream<String> tokenize(String query) {
        return Arrays.stream(whitespacePattern.split(query))
                .filter(token -> token.length() > MIN_TOKEN_LENGTH)
                .map(PatentDocumentPreprocessor::cleanToken);
    }


    public static List<Pair<Integer, String>> tokenizeWithOffset(String query) {

        Matcher matcher = tokenPattern.matcher(query);
        List<Pair<Integer, String>> output = new ArrayList<>();

        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() > MIN_TOKEN_LENGTH) {
                output.add(Pair.of(matcher.start(), token));
            }
        }

        return output;
    }

    public static boolean isNoStopword(String token) {
        return !stopwords.contains(token);
    }

    public static Stream<String> preprocess(String text) {
        return PatentDocumentPreprocessor.tokenize(text)
                .map(String::toLowerCase)
                .filter(PatentDocumentPreprocessor::isNoStopword)
                .map(PatentDocumentPreprocessor::stem);
    }


}

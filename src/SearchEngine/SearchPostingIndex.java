package SearchEngine;

import SearchEngine.Index.PostingIndex;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

import java.io.StringReader;
import java.util.*;import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by norman on 05.11.15.
 */
public class SearchPostingIndex {

    private final PostingIndex index;
    private final SnowballStemmer stemmer = new englishStemmer();


    public SearchPostingIndex(PostingIndex index) {
        this.index = index;
    }


    public List<PatentDocument> search(String query) {
        List<String> tokens = new PTBTokenizer<>(
                new StringReader(query), new CoreLabelTokenFactory(), "").tokenize()
        		.stream()
        		.map(t -> t.value())
        		.collect(Collectors.toList());
        
        if(tokens.contains("AND")) {
        	searchAnd(tokens.toArray(new String[tokens.size()]));
        } else if(tokens.contains("OR")) {
        	searchOr(tokens.toArray(new String[tokens.size()]));
        } else if(tokens.contains("NOT")) {
        	searchNot(tokens.toArray(new String[tokens.size()]));
        } else {
        	searchPhrase(tokens.toArray(new String[tokens.size()]));
        }
        
        
        return null;

    }


    private List<Posting> searchPhrase(String... tokens) {
        // Split tokens

        // searchAnd(tokens)

        // Validate positions

        // for all tokens...
    }



    private synchronized String stem(String word) {
        stemmer.setCurrent(word);
        if (stemmer.stem()) {
            return stemmer.getCurrent();
        } else {
            return word;
        }
    }

    private List<Posting> searchToken(String token) {
        token = token.toLowerCase();
        if (token.contains("*")) {
            return index.getByPrefix(token).collect(Collectors.toList());
        } else {
            String stemmedToken = stem(token.toLowerCase());
            return index.get(stemmedToken).collect(Collectors.toList());
        }
    }

    private List<Posting> searchTokenInDocs(String token, int[] docIds) {
        return searchToken(token).stream()
                .filter(posting -> intArrayContains(docIds, posting.docId()))
                .collect(Collectors.toList());
    }


    private List<Posting> searchOr(String... tokens) {
        return Arrays.stream(tokens)
                .map(this::searchToken)
                .flatMap(postings -> postings.stream())
                .distinct()
                .collect(Collectors.toList());
    }

    // Returns postings with the positions of the last token
    private List<Posting> searchAnd(String... tokens) {
        if (tokens.length == 0) {
            return Collections.emptyList();
        } else {
            List<Posting> results = null;

            for (String token : tokens) {
                if (results == null) {
                    // First token
                    // Create result set and add all search results
                    results = searchToken(token);
                } else {
                    // Subsequent tokens
                    // Remove results from the result set that are not in current search results

                    int[] previousDocIds = results.stream()
                            .mapToInt(posting -> posting.docId())
                            .distinct()
                            .toArray();
                    results = searchTokenInDocs(token, previousDocIds);
                }

                if (results.isEmpty()) {
                    break;
                }
            }
            return results;
        }
    }

    private List<Posting> searchNot(String... tokens) {
        if (tokens.length != 2) {
            return Collections.emptyList();
        } else {

            String token1 = tokens[0];
            String token2 = tokens[1];

            searchToken(tokens[0]);
            searchToken(tokens[1]);
        }
    }




    private boolean intArrayContains(final int[] array, final int key) {
        for (final int i : array) {
            if (i == key) {
                return true;
            }
        }
        return false;
    }

}

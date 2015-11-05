package SearchEngine;

import SearchEngine.Index.PostingIndex;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

import java.io.StringReader;
import java.util.*;import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by norman on 05.11.15.
 */
public class SearchPostingIndex {

    private final PostingIndex index;

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



    private List<PatentDocument> prefixSearchToken(String token) {

    }

    private List<PatentDocument> searchToken(String token) {

    }



    private List<PatentDocument> searchOr(String... tokens) {
        // Find all token matches

        // Distinct union
    }

    private List<PatentDocument> searchAnd(String... tokens) {
        // Find first token

        // Find second token in same documents

        // continue for all
    }
    
    private List<PatentDocument> searchNot(String... tokens) {
    	
    }

}

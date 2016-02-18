package SearchEngine;

/**
 *
 * @author: Your team name
 * @dataset: US patent utility grants : ipg files from http://www.google.com/googlebooks/uspto-patents-grants-text.html from 2011 to 2015
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

import java.util.ArrayList;
import com.normanrz.SearchEngine.SearchEngineInternal;

public class SearchEngineRzepkaJasper extends SearchEngine { // Replace 'Template' with your search engine's name, i.e. SearchEngineMyTeamName

    SearchEngineInternal engine = new SearchEngineInternal();
    
    public SearchEngineRzepkaJasper() { // Replace 'Template' with your search engine's name, i.e. SearchEngineMyTeamName
        // This should stay as is! Don't add anything here!
        super();
    }

    @Override
    void index() {
    }

    @Override
    boolean loadIndex() {
        engine.loadIndex(teamDirectory, dataDirectory);
        return true;
    }
    
    @Override
    void compressIndex() {
    }

    @Override
    boolean loadCompressedIndex() {
        return loadIndex();
    }
    
    @Override
    ArrayList<String> search(String query, int topK) {
        return new ArrayList<>(engine.search(query, topK));
    }
    
	// returns the normalized discounted cumulative gain at a particular rank position 'p'
	@Override
	Double computeNdcg(ArrayList<String> goldRanking, ArrayList<String> ranking, int p){
		return 0.0;
	}
}

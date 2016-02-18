package SearchEngine;

import java.util.ArrayList;

/**
 *
 * @author: Your team name
 * @dataset: US patent utility grants : ipg files from http://www.google.com/googlebooks/uspto-patents-grants-text.html from 2011 to 2015
 * @course: Information Retrieval and Web Search, Hasso-Plattner Institut, 2015
 *
 * You can run your search engine using this file
 * You can use/change this file during the development of your search engine.
 * Any changes you make here will be ignored for the final test!
 */

public class SearchEngineTest {
    
    
    public static void main(String args[]) throws Exception {

         SearchEngine myEngine = new SearchEngineRzepkaJasper();
                
		 long start = System.currentTimeMillis();
		
         myEngine.loadCompressedIndex();
		
		 long time = System.currentTimeMillis() - start;
        
         System.out.print("Loading Time:\t" + time + "\tms\n");
        
         String query = "\"3-D miniatures\"";
        
		 start = System.currentTimeMillis();
		
         ArrayList<String> results = myEngine.search(query, 10);
		
		 time = System.currentTimeMillis() - start;
        
         System.out.print("Querying Time:\t" + time + "\tms\n");
        
    }

}

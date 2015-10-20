package SearchEngine;

import SearchEngine.SaxImporter.SaxImporter;


/**
 *
 * @author: JasperRzepka
 * @dataset: US patent grants : ipg files from http://www.google.com/googlebooks/uspto-patents-grants-text.html
 * @course: Information Retrieval and Web Search, Hasso-Plattner Institut, 2015
 *
 * You can run your search engine using this file
 * You can use/change this file during the development of your search engine.
 * Any changes you make here will be ignored for the final test!
 */

public class SearchEngineTest {
    
    
    public static void main(String args[]) throws Exception {

        // SaxImporter.readDocNumberAndTitle("data/testData.xml");

        // SaxImporter.readDocNumberFromGzip("ipg150106.xml.gz");

        SearchEngine myEngine = new SearchEngineJasperRzepka();
        
        long start = System.currentTimeMillis();
        
        myEngine.index("data");
        
        long time = System.currentTimeMillis() - start;
        
        System.out.print("Indexing Time:\t" + time + "\tms\n");
        
        // myEngine.loadIndex(String directory)
        
        // String query = "";
        
        // ArrayList <String> results = new ArrayList <> ();
        
        // results = myEngine.search(String query, int topK, int prf)
        
    }

}

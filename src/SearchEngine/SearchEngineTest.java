package SearchEngine;

import java.util.ArrayList;


/**
 * @author: JasperRzepka
 * @dataset: US patent grants : ipg files from http://www.google.com/googlebooks/uspto-patents-grants-text.html
 * @course: Information Retrieval and Web Search, Hasso-Plattner Institut, 2015
 * <p>
 * You can run your search engine using this file
 * You can use/change this file during the development of your search engine.
 * Any changes you make here will be ignored for the final test!
 */

public class SearchEngineTest {


    public static void main(String args[]) throws Exception {

        // PatentDocumentImporter.readPatentDocuments("data/testData.xml");

        // PatentDocumentImporter.readDocNumberFromGzip("ipg150106.xml.gz");

    	SearchEngineJasperRzepka myEngine = new SearchEngineJasperRzepka();

        long start = System.currentTimeMillis();

        myEngine.index("data");

        long time = System.currentTimeMillis() - start;

        System.out.print("Indexing Time:\t" + time + "\tms\n");

        myEngine.loadIndex("index.bin");
        myEngine.loadCompressedIndex("index.bin.gz");

        // new PostingIndexMerger().merge(Arrays.asList(new File[] { new File("index.bin") }), new File("index.bin2"));

        String[] queries = { "secure application", "comprises AND consists", "methods NOT inventions",
                "data OR method", "prov* NOT free", "inc* OR memory", "the presented invention", "mobile devices" };

        for (String query : queries) {

            System.out.println("Query: " + query);
            ArrayList<String> results = myEngine.search(query, 0, 0);
            results.forEach(System.out::println);

            System.out.println();
        }



    }

}

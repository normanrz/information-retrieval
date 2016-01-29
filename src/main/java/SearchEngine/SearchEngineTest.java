package SearchEngine;

import SearchEngine.utils.RunUtils;

import java.util.List;


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

        try (SearchEngineJasperRzepka myEngine = new SearchEngineJasperRzepka()) {

            RunUtils.runTimedBlock(() -> {

                SearchEngineJasperRzepka.index("data_test", "index_test");

            }, "Build Test Index");

            RunUtils.runTimedBlock(() -> {

                myEngine.loadIndex("index_test", "data_test");

            }, "Load Test Index");

//            RunUtils.runTimedBlock(() -> {
//
//                SearchEngineJasperRzepka.index("data", "index");
//
//            }, "Build Full Index");

            RunUtils.runTimedBlock(() -> {

                myEngine.loadIndex("index", "data");

            }, "Load Full Index");

//            myEngine.index.allTokens()
//                    .sorted(Comparator.comparingInt(token -> -myEngine.index.getCollectionTokenCount(token)))
//                    .limit(50)
//                    .forEach(token -> System.out.println(token + ": " + myEngine.index.getCollectionTokenCount(token)));

            String[] queries = {
                    "linkTo:07920906", "reviewboa*", "review guidelines", "on-chip OR OCV"
            };

            for (String query : queries) {

                RunUtils.runTimedBlock(() -> {
                    List<String> results = myEngine.search(query, 20, 0);
                    System.out.println(String.format("Query: \'%s\' (%d)", query, results.size()));
                    results.forEach(System.out::println);

                    System.out.println();
                }, "Query Index: " + query);
            }


        }
    }

}

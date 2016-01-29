package SearchEngine;

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

            runTimed(() -> {

                SearchEngineJasperRzepka.index("data_test", "index_test");

            }, "Build Test Index");

            runTimed(() -> {

                myEngine.loadIndex("index_test", "data_test");

            }, "Load Test Index");

//            runTimed(() -> {
//
//                SearchEngineJasperRzepka.index("data", "index");
//
//            }, "Build Full Index");

            runTimed(() -> {

                myEngine.loadIndex("index", "data");

            }, "Load Full Index");

//            myEngine.index.allTokens()
//                    .sorted(Comparator.comparingInt(token -> -myEngine.index.getCollectionTokenCount(token)))
//                    .limit(50)
//                    .forEach(token -> System.out.println(token + ": " + myEngine.index.getCollectionTokenCount(token)));

            String[] queries = {
                    "linkTo:08365115", "reviewboa*", "review OR guidelines", "on-chip OR OCV"
            };

            for (String query : queries) {

                runTimed(() -> {
                    List<String> results = myEngine.search(query, 20, 0);
                    System.out.println(String.format("Query: \'%s\' (%d)", query, results.size()));
                    results.forEach(System.out::println);

                    System.out.println();
                }, "Query Index: " + query);
            }


        }
    }


    public static void runTimed(Runnable action, String label) {
        System.out.println();
        System.out.println("-----------------------------------------------------");
        System.out.println(label);

        long start = System.currentTimeMillis();
        try {
            action.run();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long time = System.currentTimeMillis() - start;

        System.out.println(String.format("%s Time:\t%d\tms", label, time));
        System.out.println("-----------------------------------------------------");
    }

}

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

        SearchEngineJasperRzepka myEngine = new SearchEngineJasperRzepka();

        runTimed(() -> {

            myEngine.indexTest("data/testData.xml", "index.bin.gz");

        }, "Indexing");

//        runTimed(() -> {
//            try {
//                new PostingIndexMerger().mergeCompressed(Arrays.asList(new File[]{
//                                new File("index.00.gz"), new File("index.01.gz"), new File("index.02.gz"),
//                                new File("index.03.gz"), new File("index.04.gz"), new File("index.05.gz")}),
//                        new File("index.bin.gz"));
//            } catch (IOException e) {
//                // Empty
//            }
//        }, "Merging");


        runTimed(() -> {

            myEngine.loadCompressedIndex("index.bin.gz");

        }, "Load Index");


        runTimed(() -> {
//            String[] queries = {"comprises AND consists", "methods NOT inventions",
//                    "data OR method", "prov* NOT free", "inc* OR memory", "the presented invention", "mobile devices"};
            String[] queries = {"processing", "computers", "mobile devices", "data"};

            for (String query : queries) {

                List<String> results = myEngine.search(query, 10, 0);
                System.out.println(String.format("Query: %s (%d)", query, results.size()));
                results.forEach(System.out::println);

                System.out.println();
            }
        }, "Query Index");

//        new DiskPostingIndex();


    }

    public static void runTimed(Runnable action, String label) {
        long start = System.currentTimeMillis();
        try {
            action.run();
        } catch (Exception e) {
            // Empty
        }

        long time = System.currentTimeMillis() - start;

        System.out.print(label + " Time:\t" + time + "\tms\n");
    }

}

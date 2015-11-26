package SearchEngine;

import SearchEngine.Query.QueryParser.QueryParser;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

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
                String[] queriesSamples = {
                        "data AND info OR mobile", "data AND (info OR mobile)",
                        "data info", "data AND information", "mobile OR \"data processing\"", "data NOT info*",
                        "\"data proces*\" NOT processing", "\"data processing\" #2", "\"data proces*\" #4",
                        "\"data processing\" mobile #2", "mobile data #3", "mobile dat* #2"};

                for (String input : queriesSamples) {
                    QueryParser parser = Parboiled.createParser(QueryParser.class);
                    ParsingResult<?> result = new ReportingParseRunner(parser.FullQuery()).run(input);
//                System.out.println(ParseTreeUtils.printNodeTree(result));
                }
            }, "Query Index");


            runTimed(() -> {
                String[] queries = {"asdjlis", "commom", "kontrol", "incluce", "streem", "digital", "rootkits", "network OR access"};

                for (String query : queries) {

                    List<String> results = myEngine.search(query, 100, 2);
                    System.out.println(String.format("Query: %s (%d)", query, results.size()));
                    results.forEach(System.out::println);

                    System.out.println();
                }
            }, "Query Index");

        }

    }


    public static void runTimed(Runnable action, String label) {
        long start = System.currentTimeMillis();
        try {
            action.run();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long time = System.currentTimeMillis() - start;

        System.out.print(label + " Time:\t" + time + "\tms\n");
    }

}

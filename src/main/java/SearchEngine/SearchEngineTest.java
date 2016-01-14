package SearchEngine;

import SearchEngine.Query.QueryParser.QueryParserJS;

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

//            runTimed(() -> {
//
//                myEngine.loadIndex("index", "data");
//
//            }, "Load Full Index");


            runTimed(() -> {
                String[] queriesSamples = {
                        "data AND info OR mobile", "data AND (info OR mobile)",
                        "data info", "data AND information", "mobile OR \"data processing\"", "data NOT info*",
                        "\"data proces*\" NOT processing", "\"data processing\" #2", "\"data proces*\" #4",
                        "\"data processing\" mobile #2", "mobile data #3", "mobile dat* #2"};

                try {
                    QueryParserJS queryParserJS = new QueryParserJS();
                    for (String input : queriesSamples) {
//                    QueryParser parser = Parboiled.createParser(QueryParser.class);
//                    ParsingResult<?> result = new ReportingParseRunner(parser.FullQuery()).run(input);
//                    System.out.println(ParseTreeUtils.printNodeTree(result));

                        System.out.println(queryParserJS.runJS(input));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "Query Index");


//            String[] queries = {"asdjlis", "commom", "kontrol", "incluce", "streem", "digital", "rootkits", "network OR access"};
            String[] queries = {
//                    "access OR control", "computers", "data OR processing", "web OR servers",
//                    "vulnerability OR information",
//                    "computer OR readable OR media"
//                    "cloud OR computing OR security OR issues", "cloud NOT smart", "3-D miniatures", "healthcare AND services"
                    "add-on OR module", "digital OR signature", "data OR processing", "scanning"
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

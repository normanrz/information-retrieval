package com.normanrz.SearchEngine;

import com.normanrz.SearchEngine.utils.RunUtils;

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

        try (SearchEngineInternal myEngine = new SearchEngineInternal()) {

//            RunUtils.runTimedBlock(() -> {
//
//                SearchEngineInternal.index("data_test", "index_test");
//
//            }, "Build Test Index");
//
//            RunUtils.runTimedBlock(() -> {
//
//                myEngine.loadIndex("index_test", "data_test");
//
//            }, "Load Test Index");

//            RunUtils.runTimedBlock(() -> {
//
//                SearchEngineInternal.index("data", "index");
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
//                    "data AND information",
//                    "mobile OR \"data processing\"",
//                    "data NOT info*",
//                    "\"data proces*\" NOT processing",
//                    "\"data processing\" #2",
//                    "\"data proces*\" #4",
//                    "\"data processing\" mobile #2",
//                    "mobile data #3",
//                    "mobile dat* #2",

                    "LinkTo:098754",
                    "LinkTo:098754 AND LinkTo:034567",
                    "LinkTo:098754 NOT LinkTo:034567",
                    "LinkTo:098754 AND data",

                    "Marker pen holder",
                    "sodium polyphosphates",
                    "\"ionizing radiation\"",
                    "solar coronal holes",
                    "patterns in scale-free networks",
                    "\"nail polish\"",
                    "\"keyboard shortcuts\"",
                    "radiographic NOT ventilator",
                    "multi-label AND learning",
                    "LinkTo:07866385"
            };

            for (String query : queries) {

                RunUtils.runTimedBlock(() -> {
                    List<String> results = myEngine.searchWithNDCG(query, 20, 0);
                    System.out.println(String.format("Query: \'%s\' (%d)", query, results.size()));
                    results.forEach(System.out::println);

                    System.out.println();
                }, "Query Index: " + query);
            }


        }
    }

}

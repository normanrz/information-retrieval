package com.normanrz.SearchEngine;

import com.normanrz.SearchEngine.DocumentIndex.XmlDocumentIndex;
import com.normanrz.SearchEngine.InvertedIndex.InvertedIndexMerger;
import com.normanrz.SearchEngine.LinkIndex.LinkIndex;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by norman on 10.01.16.
 */
public class SearchEngineCli {
    public static void main(String args[]) throws Exception {
        if (args.length < 1) {
            printUsage();
        } else {
            switch (args[0]) {
                case "index":
                    if (args.length < 6) {
                        printUsage();
                    } else {
                        index(args);
                    }
                    break;
                case "merge-inv":
                    if (args.length < 3) {
                        printUsage();
                    } else {
                        mergeInvertedIndex(args);
                    }
                    break;
                case "merge-doc":
                    if (args.length < 5) {
                        printUsage();
                    } else {
                        mergeDocumentIndex(args);
                    }
                    break;
                case "merge-link":
                    if (args.length < 3) {
                        printUsage();
                    } else {
                        mergeLinkIndex(args);
                    }
                    break;
                default:
                    printUsage();
                    break;
            }
        }
    }

    static void printUsage() {
        System.out.println("search-engine-cli index <data directory> <input xmlfile> <output inv-index> <output doc-index> <output link-index>");
        System.out.println("search-engine-cli merge-inv <input index1> <input index2> ... <output index>");
        System.out.println("search-engine-cli merge-doc <data directory> <link-index> <input index1> <input index2> ... <output index>");
        System.out.println("search-engine-cli merge-link <input index1> <input index2> ... <output index>");
    }

    static void index(String args[]) {
        String dataDirectory = args[1];
        File inputFile = new File(args[2]);
        File outputInvertedIndexFile = new File(args[3]);
        File outputDocumentIndexFile = new File(args[4]);
        File outputLinkIndexFile = new File(args[5]);

        System.out.println("Start indexing");
        SearchEngineInternal.indexSingle(
                dataDirectory, inputFile, outputInvertedIndexFile, outputDocumentIndexFile, outputLinkIndexFile);
        System.out.println("Finish indexing");

    }

    static void mergeInvertedIndex(String args[]) {
        File outputFile = new File(args[args.length - 1]);
        List<File> inputFiles = Arrays.stream(args)
                .skip(1)
                .limit(args.length - 2)
                .map(File::new)
                .collect(Collectors.toList());

        System.out.println("Start merging inverted indexes");
        try {
            InvertedIndexMerger.merge(inputFiles, outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Finish merging inverted indexes");
    }

    static void mergeDocumentIndex(String args[]) {
        String directory = args[1];
        String linkIndexFileName = args[2];
        File outputFile = new File(args[args.length - 1]);
        List<File> inputFiles = Arrays.stream(args)
                .skip(3)
                .limit(args.length - 4)
                .map(File::new)
                .collect(Collectors.toList());

        System.out.println("Start merging document indexes + PageRank");
        try {
            LinkIndex linkIndex = LinkIndex.load(new File(linkIndexFileName));
            XmlDocumentIndex.merge(directory, inputFiles, outputFile);
            XmlDocumentIndex documentIndex = XmlDocumentIndex.load(directory, outputFile);
            PageRankComputer.injectIntoDocumentIndex(
                    documentIndex, PageRankComputer.computePageRank(documentIndex, linkIndex))
                    .save(outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Finish merging document indexes + PageRank");
    }

    static void mergeLinkIndex(String args[]) {
        File outputFile = new File(args[args.length - 1]);
        List<File> inputFiles = Arrays.stream(args)
                .skip(1)
                .limit(args.length - 2)
                .map(File::new)
                .collect(Collectors.toList());

        System.out.println("Start merging link indexes");
        try {
            LinkIndex.merge(inputFiles, outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Finish merging link indexes");
    }
}

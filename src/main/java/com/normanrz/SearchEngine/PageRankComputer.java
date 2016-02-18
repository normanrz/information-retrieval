package com.normanrz.SearchEngine;

import com.normanrz.SearchEngine.DocumentIndex.DocumentIndex;
import com.normanrz.SearchEngine.DocumentIndex.XmlDocumentIndex;
import com.normanrz.SearchEngine.LinkIndex.LinkIndex;

import java.util.*;

/**
 * Created by norman on 28.01.16.
 */
public class PageRankComputer {

    private final double dampening = 0.85;
    private DocumentIndex documentIndex;
    private LinkIndex linkIndex;

    private SortedMap<Integer, Double> pageRankCache = new TreeMap<>();
    private SortedMap<Integer, Integer> invertedLinkIndex = new TreeMap<>();

    PageRankComputer(DocumentIndex documentIndex, LinkIndex linkIndex) {
        this.documentIndex = documentIndex;
        this.linkIndex = linkIndex;

        // initialize pageRankCache and invertedLinkIndex
        documentIndex.allDocIds()
                .forEach(docId -> pageRankCache.put(docId, 1.0 / documentIndex.getDocumentCount()));
        linkIndex.all()
                .forEach(entry ->
                        Arrays.stream(entry.getValue().toArray()).forEach(citedBy ->
                                invertedLinkIndex.put(citedBy, invertedLinkIndex.getOrDefault(citedBy, 0) + 1)));
    }

    public static SortedMap<Integer, Double> computePageRank(DocumentIndex documentIndex, LinkIndex linkIndex) {
        PageRankComputer computer = new PageRankComputer(documentIndex, linkIndex);
        computer.compute();
        return computer.pageRankCache;
    }

    public static XmlDocumentIndex injectIntoDocumentIndex(XmlDocumentIndex inputIndex, SortedMap<Integer, Double> pageRanks) {
        XmlDocumentIndex outputIndex = new XmlDocumentIndex(inputIndex.getDirectory());

        inputIndex.allEntries().map(Map.Entry::getValue).forEach(entry ->
                        outputIndex.add(
                                entry.getDocId(), entry.getTitleTokenCount(), entry.getDocumentTokenCount(),
                                entry.getOffset(), entry.getFilename(), pageRanks.get(entry.getDocId()))
        );

        return outputIndex;
    }

    double pageRank(int docId) {
        return (1 - dampening) / documentIndex.getDocumentCount() +
                dampening * Arrays.stream(linkIndex.get(docId))
                        .filter(pageRankCache::containsKey)
                        .mapToDouble(docId2 ->
                                pageRankCache.get(docId2) /
                                        invertedLinkIndex.getOrDefault(docId2, 1))
                        .sum();
    }

    double iteration() {
        SortedMap<Integer, Double> newPageRankCache = Collections.synchronizedSortedMap(new TreeMap<>());
        documentIndex.allDocIds().parallel()
                .forEach(docId ->
                        newPageRankCache.put(docId, pageRank(docId)));

        double difference = documentIndex.allDocIds()
                .mapToDouble(docId ->
                        pageRankCache.containsKey(docId) && newPageRankCache.containsKey(docId) ?
                                Math.abs(pageRankCache.get(docId) - newPageRankCache.get(docId)) :
                                0)
                .sum();
        pageRankCache = newPageRankCache;
        return difference;
    }

    private void compute() {
        int i = 0;
        double difference = 100;
        do {
            difference = iteration();
            System.out.println(i + ": " + difference);
            i++;
        } while (difference > 0);
    }
}

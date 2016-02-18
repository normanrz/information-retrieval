package com.normanrz.SearchEngine.DocumentIndex;

import com.normanrz.SearchEngine.PatentDocument;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Created by norman on 01.12.15.
 */
public interface DocumentIndex {

    int getDocumentTokenCount(int docId);

    int getDocumentTitleTokenCount(int docId);

    double getDocumentPageRank(int docId);

    int getDocumentCount();

    List<String> getPatentDocumentTokens(int docId);

    Optional<PatentDocument> getPatentDocument(int docId);

    Optional<String> getPatentDocumentTitle(int docId);


    IntStream allDocIds();

}

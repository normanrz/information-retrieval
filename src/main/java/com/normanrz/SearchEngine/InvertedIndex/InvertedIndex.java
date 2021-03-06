package com.normanrz.SearchEngine.InvertedIndex;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by norman on 13.11.15.
 */
public interface InvertedIndex {

    Optional<DocumentPostings> get(String token, int docId);

    Stream<DocumentPostings> get(String token);

    Stream<DocumentPostings> getByPrefix(String token);

    Stream<DocumentPostings> getInDocs(String token, int[] docIds);

    Stream<DocumentPostings> getByPrefixInDocs(String token, int[] docIds);

    Stream<String> getTokensByPrefix(String token);

    int getCollectionTokenCount();

    int getCollectionTokenCount(String token);

    int getDocumentTokenCount(String token, int docId);

    int getDocumentTitleTokenCount(String token, int docTitleTokenCount, int docId);
}

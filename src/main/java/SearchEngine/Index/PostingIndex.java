package SearchEngine.Index;

import SearchEngine.DocumentPostings;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by norman on 13.11.15.
 */
public interface PostingIndex {

    Optional<DocumentPostings> get(String token, int docId);

    Stream<DocumentPostings> get(String token);

    Stream<DocumentPostings> getByPrefix(String token);

    Stream<DocumentPostings> getInDocs(String token, int[] docIds);

    Stream<DocumentPostings> getByPrefixInDocs(String token, int[] docIds);

    Stream<String> getTokensByPrefix(String token);

    int collectionTokenCount();

    int documentTokenCount(int docId);

    int collectionTokenCount(String token);

    int documentTokenCount(String token, int docId);

}

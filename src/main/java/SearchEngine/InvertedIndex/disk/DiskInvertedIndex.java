package SearchEngine.InvertedIndex.disk;

import SearchEngine.InvertedIndex.DocumentPostings;
import SearchEngine.InvertedIndex.InvertedIndex;
import SearchEngine.InvertedIndex.PostingReader;
import SearchEngine.utils.IntArrayUtils;
import org.apache.commons.collections4.map.LRUMap;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.InflaterInputStream;

public class DiskInvertedIndex implements InvertedIndex, AutoCloseable {

    private SeekList seekList;
    private RandomAccessFile file;
    private int seekListByteLength;
    private int collectionTokenCount;

    private final int LRU_CACHE_SIZE = 500;
    private LRUMap<Integer, List<DocumentPostings>> lruDocumentPostingsCache = new LRUMap<>(LRU_CACHE_SIZE);


    public DiskInvertedIndex(File indexFile) throws IOException {
        this.file = new RandomAccessFile(indexFile, "r");
        this.seekList = readSeekList(file);
    }

    private SeekList readSeekList(RandomAccessFile indexFile) throws IOException {
        indexFile.seek(0);
        seekListByteLength = indexFile.readInt();
        collectionTokenCount = indexFile.readInt();

        return SeekListReader.readSeekListFromFile(indexFile, seekListByteLength);
    }

    private Optional<SeekListEntry> getSeekListEntry(String token) {
        return seekList.get(token).findFirst();
    }

    public Optional<DocumentPostings> get(String token, int docId) {
        return seekList.get(token)
                .flatMap(this::loadDocumentPostings)
                .filter(documentPostings -> documentPostings.getDocId() == docId)
                .findFirst();
    }

    public Stream<DocumentPostings> get(String token) {
        return seekList.get(token)
                .flatMap(this::loadDocumentPostings);
    }

    public Stream<DocumentPostings> getByPrefix(String token) {
        return seekList.getByPrefix(token)
                .flatMap(this::loadDocumentPostings);
    }

    public Stream<DocumentPostings> getInDocs(String token, int[] docIds) {
        return get(token)
                .filter(documentPostings ->
                        IntArrayUtils.intArrayContains(docIds, documentPostings.getDocId()));
    }

    public Stream<DocumentPostings> getByPrefixInDocs(String token, int[] docIds) {
        return getByPrefix(token)
                .filter(documentPostings ->
                        IntArrayUtils.intArrayContains(docIds, documentPostings.getDocId()));
    }

    public Stream<DocumentPostings> all() {
        return seekList.stream()
                .flatMap(entry -> loadDocumentPostings(entry.offset, entry.length));
    }

    public Stream<String> allTokens() {
        return seekList.stream()
                .map(SeekListEntry::getToken);
    }

    public Stream<String> getTokensByPrefix(String prefixToken) {
        return allTokens()
                .filter(token -> token.startsWith(prefixToken));
    }

    public int collectionTokenCount() {
        return collectionTokenCount;
    }

    public int collectionTokenCount(String token) {
        return seekList.get(token)
                .mapToInt(SeekListEntry::getTokenCount)
                .sum();
    }

    public int documentTokenCount(String token, int docId) {
        return getSeekListEntry(token)
                .map(this::loadDocumentPostingsList)
                .flatMap(documentPostingsList -> {
                    int i = Collections.binarySearch(documentPostingsList, DocumentPostings.searchDummy(docId));
                    if (i >= 0) {
                        return Optional.ofNullable(documentPostingsList.get(i));
                    } else {
                        return Optional.empty();
                    }
                })
                .map(DocumentPostings::getTokenCount)
                .orElse(0);
    }

    private Stream<DocumentPostings> loadDocumentPostings(SeekListEntry entry) {
        return loadDocumentPostingsList(entry).stream();
    }

    private List<DocumentPostings> loadDocumentPostingsList(SeekListEntry entry) {
        return loadDocumentPostingsList(entry.getOffset(), entry.getLength());
    }

    private synchronized Stream<DocumentPostings> loadDocumentPostings(int offset, int length) {
        return loadDocumentPostingsList(offset, length).stream();
    }

    private synchronized List<DocumentPostings> loadDocumentPostingsList(int offset, int length) {
        if (lruDocumentPostingsCache.containsKey(offset)) {
            return lruDocumentPostingsCache.get(offset);
        }

        try {
            System.out.println(String.format("[DiskInvertedIndex] Load block %d %d", offset, length));

            // Move file pointer
            file.seek(2 * Integer.BYTES + seekListByteLength + offset);
            byte[] buffer = new byte[length];
            file.readFully(buffer);

            DataInputStream stream = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(buffer)));

            List<DocumentPostings> list = PostingReader.readDocumentPostingsList(stream);
            lruDocumentPostingsCache.put(offset, list);

            return list;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }


    @Override
    public void close() throws IOException {
        file.close();
    }

    public void printStats() {
        System.out.println("Terms in index: " + seekList.stream().count());
        System.out.println("Postings in index: " + collectionTokenCount());
    }
}

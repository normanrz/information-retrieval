package SearchEngine.InvertedIndex.disk;

import SearchEngine.InvertedIndex.DocumentPostings;
import SearchEngine.InvertedIndex.InvertedIndex;
import SearchEngine.InvertedIndex.PostingReader;
import SearchEngine.InvertedIndex.seeklist.*;
import SearchEngine.utils.IntArrayUtils;
import org.apache.commons.collections4.map.LRUMap;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.InflaterInputStream;

public class DiskInvertedIndex implements InvertedIndex, AutoCloseable {

    private final int LRU_CACHE_SIZE = 5;
    private SeekList seekList;
    private RandomAccessFile file;
    private int seekListByteLength;
    private int collectionTokenCount;
    private LRUMap<Long, List<DocumentPostings>> lruDocumentPostingsCache = new LRUMap<>(LRU_CACHE_SIZE);


    public DiskInvertedIndex(File indexFile) throws IOException {
        this.file = new RandomAccessFile(indexFile, "r");
    }

    private void readHeader(RandomAccessFile indexFile) throws IOException {
        indexFile.seek(0);
        seekListByteLength = indexFile.readInt();
        collectionTokenCount = indexFile.readInt();
    }

    private void readByteArraySeekList(RandomAccessFile indexFile) throws IOException {
        readHeader(indexFile);
        byte[] seekListByteArray = new byte[seekListByteLength];
        indexFile.readFully(seekListByteArray);
        seekList = ByteArraySeekList.read(new DataInputStream(new ByteArrayInputStream(seekListByteArray)));
    }

    private void readEntryListSeekList(RandomAccessFile indexFile) throws IOException {
        readHeader(indexFile);
        seekList = SeekListReader.readSeekListFromFile(
                new DataInputStream(new BufferedInputStream(new FileInputStream(indexFile.getFD()))),
                seekListByteLength);
    }

    public boolean has(String token) {
        return seekList.has(token);
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

    public Stream<String> allTokens() {
        return seekList.stream()
                .map(SeekListEntry::getToken);
    }

    public Stream<String> getTokensByPrefix(String prefixToken) {
        return seekList.getByPrefix(prefixToken)
                .map(SeekListEntry::getToken);
    }

    public int getCollectionTokenCount() {
        return collectionTokenCount;
    }

    public int getCollectionTokenCount(String token) {
        return seekList.get(token)
                .mapToInt(SeekListEntry::getTokenCount)
                .sum();
    }

    private Optional<DocumentPostings> getDocumentPostings(String token, int docId) {

        return getSeekListEntry(token)
                .map(this::loadDocumentPostingsList)
                .flatMap(documentPostingsList -> {
                    int i = Collections.binarySearch(documentPostingsList, DocumentPostings.searchDummy(docId));
                    if (i >= 0) {
                        return Optional.ofNullable(documentPostingsList.get(i));
                    } else {
                        return Optional.empty();
                    }
                });
    }

    public int getDocumentTokenCount(String token, int docId) {
        return getDocumentPostings(token, docId)
                .map(DocumentPostings::getTokenCount)
                .orElse(0);
    }

    public int getDocumentTitleTokenCount(String token, int docTitleTokenCount, int docId) {
        return getDocumentPostings(token, docId)
                .map(documentPostings -> documentPostings.getTitleTokenCount(docTitleTokenCount))
                .orElse(0);
    }

    private Stream<DocumentPostings> loadDocumentPostings(SeekListEntry entry) {
        return loadDocumentPostingsList(entry).stream();
    }

    private List<DocumentPostings> loadDocumentPostingsList(SeekListEntry entry) {
        return loadDocumentPostingsList(entry.getOffset(), entry.getLength());
    }

    private synchronized Stream<DocumentPostings> loadDocumentPostings(long offset, int length) {
        return loadDocumentPostingsList(offset, length).stream();
    }

    private synchronized List<DocumentPostings> loadDocumentPostingsList(long offset, int length) {
        if (lruDocumentPostingsCache.containsKey(offset)) {
//            System.out.println(String.format("[DiskInvertedIndex] Cache hit %d %d", offset, length));
            return lruDocumentPostingsCache.get(offset);
        }

        try {
//            System.out.println(String.format("[DiskInvertedIndex] Load block %d %d", offset, length));

            // Move file pointer
            file.seek(2 * Integer.BYTES + seekListByteLength + offset);
            byte[] buffer = new byte[length];
            file.readFully(buffer);

            DataInputStream stream = new DataInputStream(
                    new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(buffer))));

            List<DocumentPostings> list = PostingReader.readDocumentPostingsList(stream);
            lruDocumentPostingsCache.put(offset, list);

            return list;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public void clearCache() {
        lruDocumentPostingsCache.clear();
    }


    @Override
    public void close() throws IOException {
        file.close();
    }

    public void printStats() {
        System.out.println("Terms in index: " + seekList.getLength());
        System.out.println("Postings in index: " + getCollectionTokenCount());
    }

    public static DiskInvertedIndex withEntryListSeekList(File indexFile) throws IOException {
        DiskInvertedIndex index = new DiskInvertedIndex(indexFile);
        index.readEntryListSeekList(index.file);
        return index;
    }

    public static DiskInvertedIndex withByteArraySeekList(File indexFile) throws IOException {
        DiskInvertedIndex index = new DiskInvertedIndex(indexFile);
        index.readByteArraySeekList(index.file);
        return index;
    }
}

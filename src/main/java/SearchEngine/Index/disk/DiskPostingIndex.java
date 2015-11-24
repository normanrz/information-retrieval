package SearchEngine.Index.disk;

import SearchEngine.DocumentPostings;
import SearchEngine.Index.DocumentIndex;
import SearchEngine.Index.PostingIndex;
import SearchEngine.Index.PostingReader;
import SearchEngine.utils.IntArrayUtils;
import org.apache.commons.collections4.map.LRUMap;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.InflaterInputStream;

public class DiskPostingIndex implements PostingIndex, AutoCloseable {


    private DocumentIndex docIndex;
    private SeekList seekList;
    private RandomAccessFile file;
    private int seekListByteLength;
    private int collectionTokenCount;

    private LRUMap<Integer, List<DocumentPostings>> lruDocumentPostingsCache = new LRUMap<>(100);

    public DiskPostingIndex(String indexFile, DocumentIndex docIndex) throws IOException {
        this.file = new RandomAccessFile(indexFile, "r");
        this.docIndex = docIndex;
        this.seekList = readSeekList(file);
    }

    private SeekList readSeekList(RandomAccessFile indexFile) throws IOException {
        indexFile.seek(0);
        seekListByteLength = indexFile.readInt();
        collectionTokenCount = indexFile.readInt();

        byte[] seekListBuffer = new byte[seekListByteLength];
        indexFile.readFully(seekListBuffer);

        DataInputStream seekListDataInput = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(seekListBuffer)));
        return SeekListReader.readSeekList(seekListDataInput);
    }

    public Optional<DocumentPostings> get(String token, int docId) {
        return seekList.get(token)
                .flatMap(this::loadDocumentPostings)
                .filter(documentPostings -> documentPostings.docId() == docId)
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
        return seekList.get(token)
                .flatMap(this::loadDocumentPostings)
                .filter(documentPostings ->
                        IntArrayUtils.intArrayContains(docIds, documentPostings.docId()));
    }

    public Stream<DocumentPostings> getByPrefixInDocs(String token, int[] docIds) {
        return seekList.getByPrefix(token)
                .flatMap(this::loadDocumentPostings)
                .filter(documentPostings ->
                        IntArrayUtils.intArrayContains(docIds, documentPostings.docId()));
    }

    public Stream<DocumentPostings> all() {
        return seekList.stream()
                .flatMap(entry -> loadDocumentPostings(entry.offset, entry.length));
    }

    public Stream<String> getTokensByPrefix(String prefixToken) {
        return seekList.stream()
                .map(SeekListEntry::getToken)
                .filter(token -> token.startsWith(prefixToken));
    }

    public int collectionTokenCount() {
        return collectionTokenCount;
    }

    public int documentTokenCount(int docId) {
        return docIndex.getPatentDocumentTokens(docId).size();
    }

    public int collectionTokenCount(String token) {
        return seekList.get(token)
                .mapToInt(SeekListEntry::getTokenCount)
                .sum();
    }

    public int documentTokenCount(String token, int docId) {
        return (int) docIndex.getPatentDocumentTokens(docId).stream()
                .filter(docToken -> docToken.equals(token))
                .count();
    }

    private Stream<DocumentPostings> loadDocumentPostings(SeekListEntry entry) {
        return loadDocumentPostings(entry.getOffset(), entry.getLength());
    }

    private synchronized Stream<DocumentPostings> loadDocumentPostings(int offset, int length) {

        if (lruDocumentPostingsCache.containsKey(offset)) {
            return lruDocumentPostingsCache.get(offset).stream();
        }

        try {
//            System.out.println(String.format("Load block %d %d", offset, length));

            // Move file pointer
            file.seek(2 * Integer.BYTES + seekListByteLength + offset);
            byte[] buffer = new byte[length];
            file.readFully(buffer);

            DataInputStream stream = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(buffer)));

            List<DocumentPostings> list = PostingReader.readDocumentPostingsList(stream);
            lruDocumentPostingsCache.put(offset, list);

            return list.stream();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    @Override
    public void close() throws Exception {
        file.close();
    }
}

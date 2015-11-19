package SearchEngine.Index.disk;

import SearchEngine.DocumentPostings;
import SearchEngine.Index.MemoryPostingIndex;
import SearchEngine.Index.PostingIndex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class DiskPostingIndex implements PostingIndex, AutoCloseable {
    @Override
    public Stream<String> getTokensByPrefix(String token) {
        return null;
    }

    private SeekList seekList = new SeekList();
    private RandomAccessFile file;

    public DiskPostingIndex(String indexFile) throws IOException {

        file = new RandomAccessFile(indexFile, "r");

        // dummy data
        seekList.insert(new SeekListEntry("process", 12, 12));
        seekList.insert(new SeekListEntry("process", 32, 12));
        seekList.insert(new SeekListEntry("tex", 24, 12));
        seekList.insert(new SeekListEntry("add", 0, 12));
    }

    public Optional<DocumentPostings> get(String token, int docId) {

        return seekList.get(token).stream()
                .map(entry -> loadBlock(entry.offset, entry.length))
                .map(index -> index.get(token, docId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

    }

    public Stream<DocumentPostings> get(String token) {
        return seekList.get(token).stream()
                .map(entry -> loadBlock(entry.offset, entry.length))
                .flatMap(index -> index.get(token));
    }

    public Stream<DocumentPostings> getByPrefix(String token) {
        return seekList.getByPrefix(token).stream()
                .map(entry -> loadBlock(entry.offset, entry.length))
                .flatMap(index -> index.getByPrefix(token));
    }

    public Stream<DocumentPostings> getInDocs(String token, int[] docIds) {
        return seekList.get(token).stream()
                .map(entry -> loadBlock(entry.offset, entry.length))
                .flatMap(index -> index.getInDocs(token, docIds));
    }

    public Stream<DocumentPostings> getByPrefixInDocs(String token, int[] docIds) {
        return seekList.getByPrefix(token).stream()
                .map(entry -> loadBlock(entry.offset, entry.length))
                .flatMap(index -> index.getByPrefixInDocs(token, docIds));
    }

    public int collectionTokenCount() {
        return 0;
    }

    public int documentTokenCount(int docId) {
        return 0;
    }

    public int collectionTokenCount(String token) {
        return 0;
    }

    public int documentTokenCount(String token, int docId) {
        return 0;
    }



    private PostingIndex loadBlock(int offset, int length) {
        System.out.println(String.format("Load index %d %d", offset, length));

        try {
            // Move file pointer
//            file.seek(offset);
//            byte[] buffer = new byte[length];
//            file.readFully(buffer, 0, length);

            // Mocked block loading
            byte[] buffer = new byte[(int) file.length()];
            file.seek(0);
            file.readFully(buffer, 0, (int) file.length());

            InputStream stream = new ByteArrayInputStream(buffer);

            return MemoryPostingIndex.load(new GZIPInputStream(stream));

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

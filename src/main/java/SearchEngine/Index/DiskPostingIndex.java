package SearchEngine.Index;

import SearchEngine.DocumentPostings;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

class SeekListEntry implements Comparable<SeekListEntry> {
    final String token;
    final int offset;
    final int length;

    public SeekListEntry(String token, int offset, int length) {
        this.token = token;
        this.offset = offset;
        this.length = length;
    }

    public int compareTo(SeekListEntry o) {
        return Comparator
                .comparing((SeekListEntry s) -> s.token)
                .thenComparing(s -> s.offset)
                .thenComparing(s -> s.length)
                .compare(this, o);
    }
}


public class DiskPostingIndex implements PostingIndex {

    private List<SeekListEntry> seekList;

    public DiskPostingIndex() {
        seekList = new ArrayList<>();

        // dummy data
        insertSeekListEntry(new SeekListEntry("process", 12, 12));
        insertSeekListEntry(new SeekListEntry("process", 32, 12));
        insertSeekListEntry(new SeekListEntry("tex", 24, 12));
        insertSeekListEntry(new SeekListEntry("add", 0, 12));

        getSeekListEntries("process").forEach(entry -> System.out.println(entry.token + " " + entry.offset));
    }

    public Optional<DocumentPostings> get(String token, int docId) {

        return getSeekListEntries(token).stream()
                .map(entry -> loadBlock(entry.offset, entry.length))
                .map(index -> index.get(token, docId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

    }

    public Stream<DocumentPostings> get(String token) {
        return getSeekListEntries(token).stream()
                .map(entry -> loadBlock(entry.offset, entry.length))
                .flatMap(index -> index.get(token));
    }

    public Stream<DocumentPostings> getByPrefix(String token) {
        return getSeekListEntriesByPrefix(token).stream()
                .map(entry -> loadBlock(entry.offset, entry.length))
                .flatMap(index -> index.getByPrefix(token));
    }

    public Stream<DocumentPostings> getInDocs(String token, int[] docIds) {
        return getSeekListEntries(token).stream()
                .map(entry -> loadBlock(entry.offset, entry.length))
                .flatMap(index -> index.getInDocs(token, docIds));
    }

    public Stream<DocumentPostings> getByPrefixInDocs(String token, int[] docIds) {
        return getSeekListEntriesByPrefix(token).stream()
                .map(entry -> loadBlock(entry.offset, entry.length))
                .flatMap(index -> index.getByPrefixInDocs(token, docIds));
    }

    public int collectionTokenCount() {
        return 0;
    }

    public int documentTokenCount(int docId) {
        return 0;
    }

    public int collectionTokenFrequency(String token) {
        return 0;
    }

    public int documentTokenFrequency(String token, int docId) {
        return 0;
    }


    private void insertSeekListEntry(SeekListEntry entry) {
        int insertIndex = Collections.binarySearch(seekList, entry);
        if (insertIndex < 0) {
            seekList.add(-insertIndex - 1, entry);
        } else {
            seekList.add(insertIndex, entry);
        }
    }

    private List<SeekListEntry> getSeekListEntries(String token) {
        List<SeekListEntry> results = new ArrayList<>();

        SeekListEntry lastEntry = null;
        for (SeekListEntry entry : seekList) {
            if (entry.token.compareTo(token) >= 0) {
                if (lastEntry != null) {
                    results.add(lastEntry);
                    lastEntry = null;
                }
            } else {
                lastEntry = entry;
            }
            if (entry.token.compareTo(token) == 0) {
                results.add(entry);
            }
            if (entry.token.compareTo(token) > 0) {
                break;
            }
        }
        return results;
    }

    private List<SeekListEntry> getSeekListEntriesByPrefix(String token) {
        return null;
    }

    private PostingIndex loadBlock(int offset, int length) {
        // Mocked block loading
        return MemoryPostingIndex.loadCompressed(new File("index.gz"));
    }

}

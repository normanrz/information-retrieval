package SearchEngine.InvertedIndex;


import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.IntList;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by norman on 12.11.15.
 */
public class DocumentPostings implements Comparable<DocumentPostings> {

    private final int docId;
    private final ArrayIntList positions = new ArrayIntList();

    public DocumentPostings(int docId, IntList initialPositions) {
        this(docId, initialPositions.toArray());
    }

    public DocumentPostings(int docId, int... initialPositions) {
        this.docId = docId;
        for (int pos : initialPositions) {
            this.addPosition(pos);
        }
    }

    public int getDocId() {
        return this.docId;
    }

    public IntList getPositions() {
        return this.positions;
    }

    public synchronized void addPosition(int pos) {
        this.positions.add(pos);
    }

    public int getTokenCount() {
        return this.positions.size();
    }

    public List<Posting> toPostings() {
        return Arrays.stream(positions.toArray())
                .mapToObj(pos -> new Posting(getDocId(), pos))
                .collect(Collectors.toList());
    }

    @Override
    public int compareTo(DocumentPostings o) {
        return Integer.compare(getDocId(), o.getDocId());
    }

    public static DocumentPostings merge(DocumentPostings a, DocumentPostings b) {
        if (a.getDocId() != b.getDocId()) {
            return null;
        } else {
            IntList mergedPositions = new ArrayIntList(a.getPositions().size() + b.getPositions().size());

            Stream.of(a.getPositions(), b.getPositions())
                    .flatMapToInt(value -> Arrays.stream(value.toArray()))
                    .sorted()
                    .forEach(mergedPositions::add);

            return new DocumentPostings(a.getDocId(), mergedPositions);
        }
    }

    public static DocumentPostings searchDummy(int docId) {
        return new DocumentPostings(docId);
    }
}

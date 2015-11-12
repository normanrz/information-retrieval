package SearchEngine;


import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.IntList;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by norman on 12.11.15.
 */
public class DocumentPostings {

    private final int docId;
    private final ArrayIntList positions = new ArrayIntList();

    public DocumentPostings(int doc_id, int... initialPositions) {
        this.docId = doc_id;
        for (int pos : initialPositions) {
            this.addPosition(pos);
        }
    }

    public int docId() {
        return this.docId;
    }

    public IntList positions() {
        return this.positions;
    }

    public synchronized void addPosition(int pos) {
        this.positions.add(pos);
    }

    public int tokenFrequency() {
        return this.positions.size();
    }

    public List<Posting> toPostings() {
        return Arrays.stream(positions.toArray())
                .mapToObj(pos -> new Posting(docId(), pos))
                .collect(Collectors.toList());
    }
}

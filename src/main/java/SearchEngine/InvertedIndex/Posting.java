package SearchEngine.InvertedIndex;


import SearchEngine.PatentDocument;

public class Posting implements Comparable<Posting> {
    private final int docId;
    private final int pos;

    public Posting(PatentDocument doc, int pos) {
        this(doc.getDocId(), pos);
    }

    public Posting(int doc_id, int pos) {
        this.docId = doc_id;
        this.pos = pos;
    }

    public int docId() {
        return this.docId;
    }

    public int pos() {
        return this.pos;
    }

    @Override
    public String toString() {
        return String.format("(%s:%s)", docId, pos);
    }

    @Override
    public int compareTo(Posting other) {
        if (Integer.compare(this.docId(), other.docId()) == 0) {
            return Integer.compare(this.pos(), other.pos());
        } else {
            return Integer.compare(this.docId(), other.docId());
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other.getClass() == Posting.class) {
            return compareTo((Posting) other) == 0;
        } else {
            return super.equals(other);
        }
    }

}

package SearchEngine;

public class Posting implements Comparable<Posting> {
	private final long docId;
	private final Integer pos;

	public Posting(PatentDocument doc, Integer pos) {
		this(Long.parseLong(doc.docNumber), pos);
	}
	
	public Posting(long doc_id, Integer pos) {
		this.docId = doc_id;
		this.pos = pos;
	}
	
	public long docId() {
		return this.docId;
	}
	
	public Integer pos() {
		return this.pos;
	}
	
	public String toString() {
		return String.format("(%s:%s)", docId, pos);
	}

	@Override
	public int compareTo(Posting o) {
		if (Long.compare(this.docId(), o.docId()) == 0) {
			return Long.compare(this.pos(), o.pos());
		} else {
			return Long.compare(this.docId(), o.docId());
		}
	}

}

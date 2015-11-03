package SearchEngine;

public class Posting {
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
}

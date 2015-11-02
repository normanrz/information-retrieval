package SearchEngine;

public class Posting {
	private final long doc_id;
	private final Integer pos;
	private final PatentDocument doc;

	public Posting(PatentDocument doc, Integer pos) {
		this.doc = doc;
		this.doc_id = Long.parseLong(doc.docNumber);
		this.pos = pos;
	}
	
	public PatentDocument doc() {
		return this.doc;
	}
	
	public long doc_id() {
		return this.doc_id;
	}
	
	public Integer pos() {
		return this.pos;
	}
	
	public String toString() {
		return String.format("(%s:%s)", doc, pos);
	}
}

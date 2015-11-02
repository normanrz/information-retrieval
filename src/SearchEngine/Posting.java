package SearchEngine;

public class Posting extends Tuple<PatentDocument, Integer> {

	public Posting(PatentDocument doc, Integer pos) {
		super(doc, pos);
	}
	
	public PatentDocument doc() {
		return x;
	}
	
	public Integer pos() {
		return y;
	}

}

class Tuple<X, Y> {
    public final X x;
    public final Y y;
    public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    public String toString() {
        return String.format("%s -> %s", x, y);
    }
}

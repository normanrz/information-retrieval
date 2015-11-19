package SearchEngine.Index.disk;

import java.util.Comparator;

class SeekListEntry implements Comparable<SeekListEntry> {
    protected final String token;
    protected final int offset;
    protected int length;

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

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public String getToken() {
        return token;
    }
}
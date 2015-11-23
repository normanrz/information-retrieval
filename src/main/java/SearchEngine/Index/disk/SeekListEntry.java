package SearchEngine.Index.disk;

import java.util.Comparator;

public class SeekListEntry implements Comparable<SeekListEntry> {
    protected final String token;
    protected final int offset;
    protected int length;
    protected int tokenCount;

    public SeekListEntry(String token, int offset, int length, int tokenCount) {
        this.token = token;
        this.offset = offset;
        this.length = length;
        this.tokenCount = tokenCount;
    }

    public SeekListEntry(String token, int offset, int length) {
        this(token, offset, length, 0);
    }

    public int compareTo(SeekListEntry o) {
        return Comparator
                .comparing((SeekListEntry s) -> s.token)
                .compare(this, o);
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public String getToken() {
        return token;
    }
}
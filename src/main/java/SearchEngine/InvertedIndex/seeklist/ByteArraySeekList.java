package SearchEngine.InvertedIndex.seeklist;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by norman on 27.01.16.
 */
public class ByteArraySeekList implements SeekList {

    protected final int[] tokenOffsets;
    protected final DataInputStream buffer;

    public ByteArraySeekList(DataInputStream buffer, int[] tokenOffsets) {
        this.buffer = buffer;
        this.tokenOffsets = tokenOffsets;
    }

    public static ByteArraySeekList read(DataInputStream buffer) throws IOException {
        int tokenCount = buffer.readInt();
        int[] tokenOffsets = new int[tokenCount];
        int position = Integer.BYTES;
        for (int i = 0; i < tokenCount; i++) {
            tokenOffsets[i] = position;
            buffer.skipBytes(Long.BYTES + 2 * Integer.BYTES);
            int stringLength = buffer.readUnsignedShort();
            buffer.skipBytes(stringLength);
            position += Long.BYTES + 2 * Integer.BYTES + Short.BYTES + stringLength;
        }
        return new ByteArraySeekList(buffer, tokenOffsets);
    }

    private Optional<SeekListEntry> getSingle(String token) {
        try {
            // From: http://algs4.cs.princeton.edu/11model/BinarySearch.java.html
            int lo = 0;
            int hi = getLength() - 1;
            while (lo <= hi) {
                // Key is in a[lo..hi] or not present.
                int mid = lo + (hi - lo) / 2;
                SeekListEntry midEntry = getAt(mid).get();
                int comparison = token.compareTo(midEntry.getToken());
                if (comparison < 0) {
                    hi = mid - 1;
                } else if (comparison > 0) {
                    lo = mid + 1;
                } else {
                    return Optional.of(midEntry);
                }
            }
        } catch (NoSuchElementException e) {
            // pass
        }
        return Optional.empty();
    }

    private int indexOf(String token) {
        try {
            // From: http://algs4.cs.princeton.edu/11model/BinarySearch.java.html
            int lo = 0;
            int hi = getLength() - 1;
            while (lo <= hi) {
                // Key is in a[lo..hi] or not present.
                int mid = lo + (hi - lo) / 2;
                SeekListEntry midEntry = getAt(mid).get();
                int comparison = token.compareTo(midEntry.getToken());
                if (comparison < 0) {
                    hi = mid - 1;
                } else if (comparison > 0) {
                    lo = mid + 1;
                } else {
                    return mid;
                }
            }
            return -Math.min(lo, hi);
        } catch (NoSuchElementException e) {
            // pass
        }
        return -1;
    }

    private Optional<SeekListEntry> getAtOffset(int tokenOffset) {
        try {
            buffer.reset();
            buffer.skipBytes(tokenOffset);
            return Optional.of(SeekListReader.readSeekListEntry(buffer));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Optional<SeekListEntry> getAt(int index) {
        return getAtOffset(tokenOffsets[index]);
    }

    public int getLength() {
        return tokenOffsets.length;
    }

    public Stream<SeekListEntry> get(String token) {
        return getSingle(token).map(Stream::of).orElseGet(Stream::empty);
    }

    public Stream<SeekListEntry> getByPrefix(String prefixToken) {
        int index = indexOf(prefixToken);
        index = index < 0 ? 1 - index : index;
        List<SeekListEntry> output = new ArrayList<>();
        while (index < getLength()) {
            Optional<SeekListEntry> entryOption = getAt(index);
            if (!entryOption.isPresent()) {
                break;
            }
            SeekListEntry entry = entryOption.get();
            if (entry.getToken().compareTo(prefixToken) < 0) {
                continue;
            }
            if (entry.getToken().startsWith(prefixToken)) {
                output.add(entry);
            } else {
                break;
            }
            index++;
        }

        return output.stream();
    }

    public Stream<SeekListEntry> stream() {
        return Arrays.stream(tokenOffsets)
                .mapToObj(this::getAtOffset)
                .flatMap(opt -> opt.map(Stream::of).orElseGet(Stream::empty));
    }

    @Override
    public Iterator<SeekListEntry> iterator() {
        return stream().iterator();
    }

}

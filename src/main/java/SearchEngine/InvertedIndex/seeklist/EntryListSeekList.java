package SearchEngine.InvertedIndex.seeklist;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by norman on 18.11.15.
 */
public class EntryListSeekList implements SeekList {
    protected final List<SeekListEntry> list;

    public EntryListSeekList(int seekListCapacity) {
        list = new ArrayList<>(seekListCapacity);
    }

    public EntryListSeekList() {
        list = new ArrayList<>();
    }

    public void add(SeekListEntry entry) {
        list.add(entry);
    }

    @Override
    public int getLength() {
        return list.size();
    }

    @Override
    public Stream<SeekListEntry> get(String token) {
        int index = Collections.binarySearch(list, SeekListEntry.createSearchDummy(token));
        if (index >= 0) {
            return Stream.of(list.get(index));
        } else {
            return Stream.empty();
        }
    }

    @Override
    public Stream<SeekListEntry> getByPrefix(String prefixToken) {
        return list.stream().filter(entry -> entry.getToken().startsWith(prefixToken));
    }

    @Override
    public Stream<SeekListEntry> stream() {
        return list.stream();
    }

    @Override
    public Iterator<SeekListEntry> iterator() {
        return stream().iterator();
    }

}

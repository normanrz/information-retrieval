package SearchEngine.InvertedIndex.disk;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by norman on 18.11.15.
 */
public class SeekList implements Iterable<SeekListEntry> {
    protected final List<SeekListEntry> list;

    public SeekList(int seekListCapacity) {
        list = new ArrayList<>(seekListCapacity);
    }

    public SeekList() {
        list = new ArrayList<>();
    }

    public void add(SeekListEntry entry) {
        list.add(entry);
    }

    public int getLength() {
        return list.size();
    }

    public Stream<SeekListEntry> get(String token) {
        int index = Collections.binarySearch(list, SeekListEntry.createSearchDummy(token));
        if (index >= 0) {
            return Stream.of(list.get(index));
        } else {
            return Stream.empty();
        }
    }

    public Stream<SeekListEntry> getByPrefix(String prefixToken) {
        return list.stream().filter(entry -> entry.getToken().startsWith(prefixToken));
    }

    public List<SeekListEntry> getPrimary(String token) {
        List<SeekListEntry> results = new ArrayList<>();

        SeekListEntry lastEntry = null;
        for (SeekListEntry entry : list) {
            if (entry.token.compareTo(token) >= 0) {
                if (lastEntry != null) {
                    results.add(lastEntry);
                    lastEntry = null;
                }
            } else {
                lastEntry = entry;
            }
            if (entry.token.compareTo(token) == 0) {
                results.add(entry);
            }
            if (entry.token.compareTo(token) > 0) {
                break;
            }
        }
        return results;
    }


    @Override
    public Iterator<SeekListEntry> iterator() {
        return list.iterator();
    }

    @Override
    public void forEach(Consumer<? super SeekListEntry> action) {
        list.forEach(action);
    }

    @Override
    public Spliterator<SeekListEntry> spliterator() {
        return list.spliterator();
    }

    public Stream<SeekListEntry> stream() {
        return list.stream();
    }

}

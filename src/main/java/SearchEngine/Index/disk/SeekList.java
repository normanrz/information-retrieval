package SearchEngine.Index.disk;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by norman on 18.11.15.
 */
public class SeekList implements Iterable<SeekListEntry> {
    protected final List<SeekListEntry> list;

    public SeekList() {
        list = new ArrayList<>();
    }

    public SeekList(List<SeekListEntry> initialEntries) {
        list = new ArrayList<>();
        list.addAll(initialEntries);
    }

    public void add(SeekListEntry entry) {
        list.add(entry);
    }

    public void insertAndSort(SeekListEntry entry) {
        add(entry);
        list.sort(Comparator.<SeekListEntry>naturalOrder());
    }

    public Stream<SeekListEntry> get(String token) {
        return list.stream().filter(entry -> entry.getToken().equals(token));
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

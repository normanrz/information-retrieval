package SearchEngine.InvertedIndex.memory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

/**
 * Created by norman on 26.10.15.
 */
public class MemoryIndex<T> {

    protected final ConcurrentSkipListMap<String, ConcurrentLinkedQueue<T>> index = new ConcurrentSkipListMap<>();


    public void put(String key, T value) {
        ConcurrentLinkedQueue<T> postingsList;
        synchronized (index) {
            postingsList = index.get(key);
            if (postingsList == null) {
                postingsList = new ConcurrentLinkedQueue<>();
                index.put(key, postingsList);
            }
        }
        postingsList.add(value);
    }

    public Stream<T> get(String key) {
        ConcurrentLinkedQueue<T> postingsList = index.get(key);
        if (postingsList != null) {
            return postingsList.stream();
        } else {
            return Stream.empty();
        }
    }

    public Stream<T> all() {
        return index.values().stream().flatMap(ConcurrentLinkedQueue::stream);
    }

    public Stream<String> allKeys() {
        return index.navigableKeySet().stream();
    }

    public Stream<String> getTokensByPrefix(String prefixKey) {
        return allKeys().filter(key -> key.startsWith(prefixKey));
    }

    public Stream<T> getByPrefix(String prefixKey) {
        return getTokensByPrefix(prefixKey)
                .map(index::get)
                .flatMap(ConcurrentLinkedQueue::stream);
    }

    public void printStats() {
        System.out.println("Terms in index: " + index.size());
    }
}


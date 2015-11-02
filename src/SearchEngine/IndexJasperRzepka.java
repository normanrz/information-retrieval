package SearchEngine;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

/**
 * Created by norman on 26.10.15.
 */
public class IndexJasperRzepka<T> {

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

    public void printStats() {
        System.out.println("Terms in index: " + index.size());
//        System.out.println("Document entries in index: " +
//                index.reduceValuesToLong(4, ConcurrentLinkedQueue::size, 0, (a, b) -> a + b));
    }
}


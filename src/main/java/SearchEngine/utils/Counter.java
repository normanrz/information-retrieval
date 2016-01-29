package SearchEngine.utils;

import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.IntList;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by norman on 29.01.16.
 */
public class Counter {
    private final HashMap<Integer, Integer> map = new HashMap<>();

    public void add(int docId) {
        map.put(docId, map.getOrDefault(docId, 0) + 1);
    }

    public int[] topElements(int softCutoff) {

        Map<Integer, List<Map.Entry<Integer, Integer>>> grouped = map.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue));

        int[] counts = grouped.keySet().stream()
                .distinct()
                .sorted(Comparator.<Integer>reverseOrder())
                .mapToInt(Integer::intValue)
                .toArray();
        IntList results = new ArrayIntList();
        for (int count : counts) {
            grouped.get(count).stream()
                    .map(Map.Entry::getKey)
                    .mapToInt(Integer::intValue)
                    .forEach(results::add);
            if (results.size() > softCutoff) {
                break;
            }
        }
        return results.toArray();
    }
}

package SearchEngine.InvertedIndex.seeklist;

import java.util.stream.Stream;

/**
 * Created by norman on 27.01.16.
 */
public interface SeekList extends Iterable<SeekListEntry> {
    int getLength();

    boolean has(String token);

    Stream<SeekListEntry> get(String token);

    Stream<SeekListEntry> getByPrefix(String prefixToken);

    Stream<SeekListEntry> stream();
}

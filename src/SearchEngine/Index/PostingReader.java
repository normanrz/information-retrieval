package SearchEngine.Index;

import SearchEngine.Posting;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by norman on 03.11.15.
 */
public class PostingReader {

    public static Posting readPosting(DataInputStream stream) throws IOException {
        long docNumber = stream.readLong();
        int pos = stream.readInt();
        return new SearchEngine.Posting(docNumber, pos);
    }

    public static Collection<SearchEngine.Posting> readPostingsList(DataInputStream stream) throws IOException {
        int postingsListLength = stream.readInt();
        Collection<SearchEngine.Posting> postingsList = new ArrayList<>(postingsListLength);
        for (int i = 0; i < postingsListLength; i++) {
            postingsList.add(readPosting(stream));
        }
        return postingsList;
    }
}

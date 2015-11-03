package SearchEngine.Index;

import SearchEngine.Posting;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by norman on 03.11.15.
 */
public class PostingReader {

    public static Posting readPosting(DataInputStream stream) throws IOException {
        int docNumber = stream.readInt();
        int pos = stream.readInt();
        return new SearchEngine.Posting(docNumber, pos);
    }

    public static List<SearchEngine.Posting> readPostingsList(DataInputStream stream) throws IOException {
        int postingsListLength = stream.readInt();
        List<Posting> postingsList = new ArrayList<>(postingsListLength);

        Posting lastPosting = null;
        for (int i = 0; i < postingsListLength; i++) {
            Posting posting = readPosting(stream);
            if (lastPosting != null) {
                posting = fromDelta(posting, lastPosting);
            }
            postingsList.add(posting);
            lastPosting = posting;

        }
        return postingsList;
    }


    public static Posting fromDelta(Posting a, Posting b) {
        return new Posting(a.docId() + b.docId(), a.pos() + b.pos());
    }
}

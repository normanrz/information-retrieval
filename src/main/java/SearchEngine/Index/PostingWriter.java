package SearchEngine.Index;

import SearchEngine.Posting;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by norman on 03.11.15.
 */
public class PostingWriter {

    public static void writePosting(DataOutputStream stream, Posting posting) throws IOException {
        stream.writeInt(posting.docId());
        stream.writeInt(posting.pos());
    }

    public static void writePostingsList(DataOutputStream stream, List<Posting> postingsList) throws IOException {
        stream.writeInt(postingsList.size());
        Posting lastPosting = null;
        for (Posting posting : postingsList) {
            if (lastPosting == null) {
                writePosting(stream, posting);
            } else {
                writePosting(stream, toDelta(posting, lastPosting));
            }
            lastPosting = posting;
        }
    }

    private static Posting toDelta(Posting a, Posting b) {
        return new Posting(a.docId() - b.docId(), a.pos() - b.pos());
    }

}

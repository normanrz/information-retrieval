package SearchEngine.Index;

import SearchEngine.Posting;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by norman on 03.11.15.
 */
public class PostingWriter {

    public static void writePosting(DataOutputStream stream, Posting posting) throws IOException {
        stream.writeLong(posting.docId());
        stream.writeInt(posting.pos());
    }

    public static void writePostingsList(DataOutputStream stream, Collection<Posting> postingsList) throws IOException {
        stream.writeInt(postingsList.size());
        for (Posting posting : postingsList) {
            writePosting(stream, posting);
        }
    }
}

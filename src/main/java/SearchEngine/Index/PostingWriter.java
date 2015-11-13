package SearchEngine.Index;

import SearchEngine.DocumentPostings;
import SearchEngine.Posting;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by norman on 03.11.15.
 */
public class PostingWriter {

    @Deprecated
    public static void writePosting(DataOutputStream stream, Posting posting) throws IOException {
        stream.writeInt(posting.docId());
        stream.writeInt(posting.pos());
    }

    @Deprecated
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

    public static void writeDocumentPostings(
            DataOutputStream stream, DocumentPostings documentPostings) throws IOException {
        stream.writeInt(documentPostings.docId());
        stream.writeInt(documentPostings.tokenFrequency());
        for (int pos : documentPostings.positions().toArray()) {
            stream.writeInt(pos);
        }
    }

    public static void writeDocumentPostingsList(
            DataOutputStream stream, List<DocumentPostings> documentPostingsList) throws IOException {
        stream.writeInt(documentPostingsList.size());
        DocumentPostings lastDocumentPostings = null;
        for (DocumentPostings documentPostings : documentPostingsList) {
            if (lastDocumentPostings == null) {
                writeDocumentPostings(stream, documentPostings);
            } else {
                writeDocumentPostings(stream, toDelta(documentPostings, lastDocumentPostings));
            }
            lastDocumentPostings = documentPostings;
        }

    }

    private static DocumentPostings toDelta(DocumentPostings a, DocumentPostings b) {
        return new DocumentPostings(a.docId() - b.docId(), a.positions());
    }

    @Deprecated
    private static Posting toDelta(Posting a, Posting b) {
        return new Posting(a.docId() - b.docId(), a.pos());
    }

}

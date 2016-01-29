package SearchEngine.InvertedIndex;

import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.IntList;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by norman on 03.11.15.
 */
public class PostingReader {

    private PostingReader() {
    }

    public static DocumentPostings readDocumentPostings(DataInput stream) throws IOException {
        int docId = stream.readInt();
        int termFrequency = stream.readInt();
        IntList positions = new ArrayIntList(termFrequency);
        for (int i = 0; i < termFrequency; i++) {
            positions.add(stream.readInt());
        }
        return new DocumentPostings(docId, positions);
    }

    public static List<DocumentPostings> readDocumentPostingsList(DataInput stream) throws IOException {
        int documentPostingsListLength = stream.readInt();
        List<DocumentPostings> output = new ArrayList<>(documentPostingsListLength);

        DocumentPostings lastDocumentPostings = null;
        for (int i = 0; i < documentPostingsListLength; i++) {
            DocumentPostings documentPostings = readDocumentPostings(stream);
            if (lastDocumentPostings != null) {
                documentPostings = fromDelta(documentPostings, lastDocumentPostings);
            }
            output.add(documentPostings);
            lastDocumentPostings = documentPostings;

        }

        return output;
    }


    private static DocumentPostings fromDelta(DocumentPostings a, DocumentPostings b) {
        return new DocumentPostings(a.getDocId() + b.getDocId(), a.getPositions());
    }

    @Deprecated
    private static Posting fromDelta(Posting a, Posting b) {
        return new Posting(a.docId() + b.docId(), a.pos());
    }
}

package SearchEngine.InvertedIndex;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by norman on 03.11.15.
 */
public class PostingWriter {

    private PostingWriter() {
    }

    public static int writeDocumentPostings(
            DataOutputStream stream, DocumentPostings documentPostings) throws IOException {

        stream.writeInt(documentPostings.getDocId());
        stream.writeInt(documentPostings.getTokenCount());
        for (int pos : documentPostings.getPositions().toArray()) {
            stream.writeInt(pos);
        }
        return documentPostings.getPositions().size();
    }

    public static int writeDocumentPostingsList(
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
        return documentPostingsList.size();
    }

    public static ByteArrayOutputStream writeDocumentPostingsListToBuffer(
            List<DocumentPostings> documentPostingsList) throws IOException {
        ByteArrayOutputStream postingsBuffer = new ByteArrayOutputStream();
        DataOutputStream postingsDataOutput = new DataOutputStream(new DeflaterOutputStream(postingsBuffer));
        PostingWriter.writeDocumentPostingsList(postingsDataOutput, documentPostingsList);
        postingsDataOutput.close();
        return postingsBuffer;
    }

    public static int documentPostingsByteLength(DocumentPostings documentPostings) {
        return (2 + documentPostings.getPositions().size()) * Integer.BYTES;
    }

    public static int documentPostingsListByteLength(Stream<DocumentPostings> documentPostingsStream) {
        return Integer.BYTES + documentPostingsStream.mapToInt(PostingWriter::documentPostingsByteLength).sum();
    }

    public static int documentPostingsListByteLength(List<DocumentPostings> documentPostingsList) {
        return documentPostingsListByteLength(documentPostingsList.stream());
    }


    private static DocumentPostings toDelta(DocumentPostings a, DocumentPostings b) {
        return new DocumentPostings(a.getDocId() - b.getDocId(), a.getPositions());
    }

    @Deprecated
    private static Posting toDelta(Posting a, Posting b) {
        return new Posting(a.docId() - b.docId(), a.pos());
    }

}
